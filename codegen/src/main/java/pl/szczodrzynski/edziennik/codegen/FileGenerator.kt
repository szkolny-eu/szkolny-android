/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-28.
 */

package pl.szczodrzynski.edziennik.codegen

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.TypeConverters
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import pl.szczodrzynski.edziennik.annotation.SelectiveDao
import pl.szczodrzynski.edziennik.annotation.UpdateSelective
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.lang.model.type.*
import javax.lang.model.util.ElementFilter
import javax.tools.Diagnostic
import kotlin.reflect.KClass

@Suppress("unused")
@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(FileGenerator.KAPT_KOTLIN_GENERATED_OPTION_NAME)
class FileGenerator : AbstractProcessor() {
    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    private data class TypeConverter(val dataType: TypeMirror, val converterType: TypeElement, val methodName: Name, val returnType: TypeMirror)

    private inline fun <reified T : Annotation> Element.getAnnotationClassValue(f: T.() -> KClass<*>) = try {
        getAnnotation(T::class.java).f()
        throw Exception("Expected to get a MirroredTypeException")
    } catch (e: MirroredTypeException) {
        e.typeMirror
    }
    private inline fun <reified T : Annotation> Element.getAnnotationClassValues(f: T.() -> Array<KClass<*>>) = try {
        getAnnotation(T::class.java).f()
        throw Exception("Expected to get a MirroredTypesException")
    } catch (e: MirroredTypesException) {
        e.typeMirrors
    }

    override fun process(set: MutableSet<out TypeElement>?, roundEnvironment: RoundEnvironment?): Boolean {
        roundEnvironment?.getElementsAnnotatedWith(SelectiveDao::class.java)?.forEach { it ->
            if (it.kind != ElementKind.CLASS) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Can only be applied to classes, element: $it")
                return false
            }

            val generatedSourcesRoot = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
            if (generatedSourcesRoot?.isEmpty() != false) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Can't find the target directory for generated Kotlin files.")
                return false
            }

            val file = File(generatedSourcesRoot)
            file.mkdirs()

            val dao = it as TypeElement
            processClass(dao, file)

            //processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "package = $packageName, className = $className, methodName = $methodName, tableName = $tableName, paramName = $paramName, paramClass = $paramClass")
        }
        return true
    }

    private fun processClass(dao: TypeElement, file: File) {
        val daoName = dao.simpleName.toString()
        val packageName = processingEnv.elementUtils.getPackageOf(dao).toString()

        val dbType = processingEnv.typeUtils.asElement(dao.getAnnotationClassValue<SelectiveDao> { db }) as TypeElement
        val typeConverters = dbType.getAnnotationClassValues<TypeConverters> { value }.map {
            processingEnv.typeUtils.asElement(it) as TypeElement
        }.map { type ->
            processingEnv.elementUtils.getAllMembers(type).mapNotNull { element ->
                if (element is ExecutableElement) {
                    if (element.returnType.toString() == "java.lang.String"
                            || element.returnType.toString() == "java.lang.Long"
                            || element.returnType.toString() == "java.lang.Integer"
                            || element.returnType.kind.isPrimitive) {
                        if (element.simpleName.startsWith("to") && element.parameters.isNotEmpty())
                            return@mapNotNull TypeConverter(element.parameters.first().asType(), type, element.simpleName, element.returnType)
                    }
                }
                null
            }
        }.flatten()

        //processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "c = ${typeConverters.joinToString()}")

        val roomDatabase = ClassName("androidx.room", "RoomDatabase")
        val selective = TypeSpec.classBuilder("${daoName}Selective")
                .primaryConstructor(FunSpec.constructorBuilder()
                        .addParameter("__db", roomDatabase, KModifier.PRIVATE)
                        .build())
                .addProperty(PropertySpec.builder("__db", roomDatabase)
                        .initializer("__db")
                        .addModifiers(KModifier.PRIVATE)
                        .build())

        val usedTypeConverters = mutableSetOf<TypeConverter>()

        processingEnv.elementUtils.getAllMembers(dao).forEach { element ->
            if (element.kind != ElementKind.METHOD)
                return@forEach
            val method = element as ExecutableElement
            val annotation = method.getAnnotation(UpdateSelective::class.java) ?: return@forEach
            usedTypeConverters.addAll(processMethod(selective, method, annotation, typeConverters))
        }

        usedTypeConverters.forEach { converter ->
            selective.addProperty(PropertySpec.builder("__${converter.converterType.simpleName}", converter.converterType.asType().asTypeName(), KModifier.PRIVATE)
                    .delegate(CodeBlock.builder()
                            .beginControlFlow("lazy")
                            .addStatement("%T()", converter.converterType.asType().asTypeName())
                            .endControlFlow()
                            .build())
                    .build())
        }

        FileSpec.builder(packageName, "${daoName}Selective")
                .addType(selective.build())
                .build()
                .writeTo(file)
    }

    private fun VariableElement.name() = getAnnotation(ColumnInfo::class.java)?.name ?: simpleName.toString()

    private fun processMethod(cls: TypeSpec.Builder, method: ExecutableElement, annotation: UpdateSelective, typeConverters: List<TypeConverter>): List<TypeConverter> {
        val methodName = method.simpleName.toString()
        val parameter = method.parameters.first()
        val paramName = parameter.simpleName.toString()
        val paramTypeElement = processingEnv.typeUtils.asElement(parameter.asType()) as TypeElement
        val paramTypeAnnotation = paramTypeElement.getAnnotation(Entity::class.java)

        val tableName = paramTypeAnnotation.tableName
        val primaryKeys = annotation.primaryKeys
        val skippedColumns = annotation.skippedColumns


        var members = processingEnv.elementUtils.getAllMembers(paramTypeElement)
        val allFields = ElementFilter.fieldsIn(members)

        // check all super classes
        var superType = paramTypeElement.superclass
        while (superType !is NoType) {
            val superTypeElement = processingEnv.typeUtils.asElement(superType) as TypeElement
            members = processingEnv.elementUtils.getAllMembers(superTypeElement)
            allFields += ElementFilter.fieldsIn(members)
            superType = superTypeElement.superclass
        }

        allFields.removeAll { skippedColumns.contains(it.name()) }
        allFields.removeAll { it.getAnnotation(Ignore::class.java) != null }
        allFields.removeAll { field -> field.modifiers.any { it == Modifier.STATIC } }
        val allFieldsDistinct = allFields.distinct()

        // dump fields
        //processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, allFieldsDistinct.joinToString())

        val fields = allFieldsDistinct.filterNot { primaryKeys.contains(it.name()) }
        val primaryFields = allFieldsDistinct.filter { primaryKeys.contains(it.name()) }
        val fieldNames = fields.map { it.name() }
        val primaryFieldNames = primaryFields.map { it.name() }

        val fieldNamesQuery = fieldNames.joinToString { "$it = ?" }
        val primaryFieldNamesQuery = primaryFieldNames.joinToString(" AND ") { "$it = ?" }
        val query = "\"\"\"UPDATE $tableName SET $fieldNamesQuery WHERE $primaryFieldNamesQuery\"\"\""

        val entityInsertionAdapter = ClassName("androidx.room", "EntityInsertionAdapter")
        val supportSQLiteStatement = ClassName("androidx.sqlite.db", "SupportSQLiteStatement")

        val usedTypeConverters = mutableListOf<TypeConverter>()

        val bind = CodeBlock.builder()
        (fields+primaryFields).forEachIndexed { i, field ->
            val index = i+1
            val fieldName = field.simpleName.toString()
            val name = "${paramName}_$fieldName"
            val realName = "${paramName}.$fieldName"
            val nullable = field.getAnnotation(org.jetbrains.annotations.Nullable::class.java) != null

            var param = when (field.asType().kind) {
                TypeKind.BOOLEAN -> "if ($name) 1L else 0L"
                TypeKind.BYTE,
                TypeKind.SHORT,
                TypeKind.INT -> "$name.toLong()"
                TypeKind.CHAR -> "$name.toString()"
                TypeKind.FLOAT -> "$name.toDouble()"
                else -> when (field.asType().toString()) {
                    "java.lang.String" -> name
                    "java.lang.Boolean" -> "if ($name == true) 1L else 0L"
                    "java.lang.Byte",
                    "java.lang.Short",
                    "java.lang.Integer" -> "$name.toLong()"
                    "java.lang.Long" -> name
                    "java.lang.Char" -> "$name.toString()"
                    "java.lang.Float" -> "$name.toDouble()"
                    "java.lang.Double" -> name
                    else -> name
                }
            }

            var isConvert = false
            val bindMethod = when (field.asType().kind) {
                TypeKind.BOOLEAN -> "bindLong"
                TypeKind.BYTE -> "bindLong"
                TypeKind.SHORT -> "bindLong"
                TypeKind.INT -> "bindLong"
                TypeKind.LONG -> "bindLong"
                TypeKind.CHAR -> "bindString"
                TypeKind.FLOAT -> "bindDouble"
                TypeKind.DOUBLE -> "bindDouble"
                else -> when (field.asType().toString()) {
                    "java.lang.String" -> "bindString"
                    "java.lang.Boolean" -> "bindLong"
                    "java.lang.Byte" -> "bindLong"
                    "java.lang.Short" -> "bindLong"
                    "java.lang.Integer" -> "bindLong"
                    "java.lang.Long" -> "bindLong"
                    "java.lang.Char" -> "bindString"
                    "java.lang.Float" -> "bindDouble"
                    "java.lang.Double" -> "bindDouble"
                    else -> {
                        val converter = typeConverters.firstOrNull {
                            it.dataType.toString() == field.asType().toString()
                        }
                        if (converter != null) {
                            param = "__${converter.converterType.simpleName}.${converter.methodName}($realName)"
                            param = when (converter.returnType.toString()) {
                                "java.lang.Integer", "int",
                                "java.lang.Short", "short",
                                "java.lang.Byte", "byte" -> "$param.toLong()"
                                "java.lang.Boolean", "boolean" -> "if ($param) 1L else 0L"
                                "java.lang.Char", "char" -> "$param.toString()"
                                "java.lang.Float", "float" -> "$param.toDouble()"
                                else -> param
                            }
                            isConvert = true
                            usedTypeConverters += converter
                            when (converter.returnType.toString()) {
                                "java.lang.Integer", "int",
                                "java.lang.Short", "short",
                                "java.lang.Byte", "byte",
                                "java.lang.Boolean", "boolean" -> "bindLong"
                                "java.lang.Char", "char" -> "bindString"
                                "java.lang.Float", "float" -> "bindDouble"
                                else -> "bindString"
                            }
                        }
                        else "bind${field.asType()}"
                    }
                }
            }

            if (!isConvert) {
                bind.addStatement("val $name = $realName")
            }
            else {
                bind.addStatement("val $name = $param")
                param = name
            }
            if (nullable) {
                bind.beginControlFlow("if ($name == null)")
                        .addStatement("stmt.bindNull($index)")
                        .endControlFlow()
                        .beginControlFlow("else")
                        .addStatement("stmt.$bindMethod($index, $param)")
                        .endControlFlow()
            }
            else {
                bind.addStatement("stmt.$bindMethod($index, $param)")
            }
        }

        val adapterName = "__insertionAdapterOf$methodName"
        val delegate = CodeBlock.builder().add("""
            |lazy {
            |    object : EntityInsertionAdapter<%T>(__db) {
            |        override fun createQuery() = $query
            |        override fun bind(stmt: %T, $paramName: %T) {
            |${bind.indent().indent().indent().build()}
            |        }
            |    }
            |}""".trimMargin(), paramTypeElement.asClassName(), supportSQLiteStatement, paramTypeElement.asClassName())

        cls.addProperty(PropertySpec.builder(adapterName, entityInsertionAdapter.parameterizedBy(paramTypeElement.asClassName()), KModifier.PRIVATE)
                .delegate(delegate.build())
                .build())

        val list = ClassName("kotlin.collections", "List")
        val longArray = ClassName("kotlin", "LongArray")

        val function = FunSpec.builder(methodName)
                .addModifiers(KModifier.INTERNAL)
                .addParameter("item", parameter.asType().asTypeName())
                .returns(Long::class.java)
                .addStatement("__db.assertNotSuspendingTransaction()")
                .addStatement("__db.beginTransaction()")
                .addCode("""
                    |try {
                    |    val _result = $adapterName.insertAndReturnId(item)
                    |    __db.setTransactionSuccessful()
                    |    return _result
                    |} finally {
                    |    __db.endTransaction()
                    |}
                """.trimMargin())
                .build()

        val functionAll = FunSpec.builder(methodName+"All")
                .addModifiers(KModifier.INTERNAL)
                .addParameter("items", list.parameterizedBy(parameter.asType().asTypeName()))
                .returns(longArray)
                .addStatement("__db.assertNotSuspendingTransaction()")
                .addStatement("__db.beginTransaction()")
                .addCode("""
                    |try {
                    |    val _result = $adapterName.insertAndReturnIdsArray(items)
                    |    __db.setTransactionSuccessful()
                    |    return _result
                    |} finally {
                    |    __db.endTransaction()
                    |}
                """.trimMargin())
                .build()

        cls.addFunction(function)
        cls.addFunction(functionAll)
        return usedTypeConverters
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(SelectiveDao::class.java.canonicalName)
    }
}
