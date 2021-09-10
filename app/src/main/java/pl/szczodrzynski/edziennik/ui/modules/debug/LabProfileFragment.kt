/*
 * Copyright (c) Kuba Szczodrzyński 2020-5-12.
 */

package pl.szczodrzynski.edziennik.ui.modules.debug

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.databinding.TemplateListPageFragmentBinding
import pl.szczodrzynski.edziennik.ui.dialogs.input
import pl.szczodrzynski.edziennik.ui.modules.base.lazypager.LazyFragment
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration
import kotlin.coroutines.CoroutineContext

class LabProfileFragment : LazyFragment(), CoroutineScope {
    companion object {
        private const val TAG = "LabProfileFragment"
    }

    private lateinit var app: App
    private lateinit var activity: MainActivity
    private lateinit var b: TemplateListPageFragmentBinding

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // local/private variables go here
    private lateinit var adapter: LabJsonAdapter
    private val loginStore by lazy {
        app.db.loginStoreDao().getByIdNow(app.profile.loginStoreId)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as MainActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = TemplateListPageFragmentBinding.inflate(inflater)
        return b.root
    }

    override fun onPageCreated(): Boolean { startCoroutineTimer(100L) {
        adapter = LabJsonAdapter(activity, onJsonElementClick = { item ->
            try {
                var parent: Any = Unit
                var obj: Any = Unit
                var objName: String = ""
                item.key.split(":").forEach { el ->
                    parent = obj
                    obj = when (el) {
                        "App.profile" -> app.profile
                        "App.profile.studentData" -> app.profile.studentData
                        "App.profile.loginStore" -> loginStore?.data ?: JsonObject()
                        "App.config" -> app.config.values
                        else -> when (obj) {
                            is JsonObject -> (obj as JsonObject).get(el)
                            is JsonArray -> (obj as JsonArray).get(el.toInt())
                            is HashMap<*, *> -> (obj as HashMap<String, String?>)[el].toString()
                            else -> {
                                val field = obj::class.java.getDeclaredField(el)
                                field.isAccessible = true
                                field.get(obj) ?: return@forEach
                            }
                        }
                    }
                    objName = el
                }

                val objVal = obj
                val value = when (objVal) {
                    is JsonPrimitive -> when {
                        objVal.isString -> objVal.asString
                        objVal.isNumber -> objVal.asNumber.toString()
                        objVal.isBoolean -> objVal.asBoolean.toString()
                        else -> objVal.asString
                    }
                    else -> objVal.toString()
                }

                MaterialAlertDialogBuilder(activity)
                    .setTitle(item.key)
                    .input(
                        hint = "value",
                        value = value,
                        positiveButton = R.string.ok,
                        positiveListener = { _, input ->
                            when (parent) {
                                is JsonObject -> {
                                    val v = objVal as JsonPrimitive
                                    when {
                                        v.isString -> (parent as JsonObject)[objName] = input
                                        v.isNumber -> (parent as JsonObject)[objName] = input.toLong()
                                        v.isBoolean -> (parent as JsonObject)[objName] = input.toBoolean()
                                    }
                                }
                                is JsonArray -> {

                                }
                                is HashMap<*, *> -> app.config.set(objName, input)
                                else -> {
                                    val field = parent::class.java.getDeclaredField(objName)
                                    field.isAccessible = true
                                    val newVal = when (objVal) {
                                        is Int -> input.toInt()
                                        is Boolean -> input.toBoolean()
                                        is Float -> input.toFloat()
                                        is Char -> input.toCharArray()[0]
                                        is String -> input
                                        is Long -> input.toLong()
                                        is Double -> input.toDouble()
                                        else -> input
                                    }
                                    field.set(parent, newVal)
                                }
                            }

                            when (item.key.substringBefore(":")) {
                                "App.profile" -> app.profileSave()
                                "App.profile.studentData" -> app.profileSave()
                                "App.profile.loginStore" -> app.db.loginStoreDao().add(loginStore)
                            }

                            showJson()

                            return@input true
                        }
                    )
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
            catch (e: Exception) {
                activity.error(ApiError.fromThrowable(TAG, e))
            }
        })

        showJson()

        b.list.adapter = adapter
        b.list.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(SimpleDividerItemDecoration(context))
            addOnScrollListener(onScrollListener)
        }

        // show/hide relevant views
        b.progressBar.isVisible = false
        b.list.isVisible = true
        b.noData.isVisible = false

    }; return true }

    private fun showJson() {
        val json = JsonObject().also { json ->
            json.add("App.profile", app.gson.toJsonTree(app.profile))
            json.add("App.profile.studentData", app.profile.studentData)
            json.add("App.profile.loginStore", loginStore?.data ?: JsonObject())
            json.add("App.config", JsonParser().parse(app.gson.toJson(app.config.values.toSortedMap())))
        }
        adapter.items = LabJsonAdapter.expand(json, 0)
        adapter.notifyDataSetChanged()
    }
}
