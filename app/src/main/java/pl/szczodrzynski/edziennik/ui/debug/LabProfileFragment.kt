/*
 * Copyright (c) Kuba Szczodrzyński 2020-5-12.
 */

package pl.szczodrzynski.edziennik.ui.debug

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.databinding.TemplateListPageFragmentBinding
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.ui.base.lazypager.LazyFragment
import pl.szczodrzynski.edziennik.ui.login.LoginActivity
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration
import kotlin.coroutines.CoroutineContext

class LabProfileFragment : LazyFragment(), CoroutineScope {
    companion object {
        private const val TAG = "LabProfileFragment"
    }

    private lateinit var app: App
    private lateinit var activity: AppCompatActivity
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
        activity = (getActivity() as AppCompatActivity?) ?: return null
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
                var objName = ""
                item.key.split(":").forEach { el ->
                    parent = obj
                    obj = when (el) {
                        "Profile" -> app.profile
                        "Profile / studentData" -> app.profile.studentData
                        "LoginStore" -> loginStore
                        "LoginStore / data" -> loginStore?.data ?: JsonObject()
                        "Config" -> app.config.values
                        "Config (profile)" -> app.profile.config.values
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
                    is Enum<*> -> objVal.toString()
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
                                is HashMap<*, *> -> app.config[objName] = input
                                else -> {
                                    val field = parent::class.java.getDeclaredField(objName)
                                    field.isAccessible = true
                                    @Suppress("USELESS_CAST")
                                    val newVal = when (objVal) {
                                        is Int -> input.toInt()
                                        is Boolean -> input.toBoolean()
                                        is Float -> input.toFloat()
                                        is Char -> input.toCharArray()[0]
                                        is String -> input
                                        is Long -> input.toLong()
                                        is Double -> input.toDouble()
                                        is Enum<*> -> input.toEnum(objVal::class.java) as Enum
                                        else -> input
                                    }
                                    field.set(parent, newVal)
                                }
                            }

                            when (item.key.substringBefore(":")) {
                                "Profile", "Profile / studentData" -> app.profileSave()
                                "LoginStore", "LoginStore / data" -> app.db.loginStoreDao().add(loginStore)
                            }

                            showJson()

                            return@input true
                        }
                    )
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
            catch (e: Exception) {
                if (activity is MainActivity)
                    (activity as MainActivity).error(ApiError.fromThrowable(TAG, e))
                if (activity is LoginActivity)
                    (activity as LoginActivity).error(ApiError.fromThrowable(TAG, e))
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
            json.add("Profile", app.gson.toJsonTree(app.profile))
            json.add("Profile / studentData", app.profile.studentData)
            json.add("LoginStore", app.gson.toJsonTree(loginStore))
            json.add("LoginStore / data", loginStore?.data ?: JsonObject())
            json.add("Config", JsonParser.parseString(app.gson.toJson(app.config.values.toSortedMap())))
            json.add("Config (profile)", JsonParser.parseString(app.gson.toJson(app.profile.config.values.toSortedMap())))
        }
        adapter.items = LabJsonAdapter.expand(json, 0)
        adapter.notifyDataSetChanged()
    }
}
