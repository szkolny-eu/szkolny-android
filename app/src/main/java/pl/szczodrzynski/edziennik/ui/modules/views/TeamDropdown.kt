/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-7.
 */

package pl.szczodrzynski.edziennik.ui.modules.views

import android.content.Context
import android.content.ContextWrapper
import android.util.AttributeSet
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.AppDb
import pl.szczodrzynski.edziennik.data.db.entity.Team
import pl.szczodrzynski.edziennik.utils.TextInputDropDown

class TeamDropdown : TextInputDropDown {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val activity: AppCompatActivity?
        get() {
            var context: Context? = context ?: return null
            if (context is AppCompatActivity) return context
            while (context is ContextWrapper) {
                if (context is AppCompatActivity)
                    return context
                context = context.baseContext
            }
            return null
        }

    lateinit var db: AppDb
    var profileId: Int = 0
    var showNoTeam = true
    var onTeamSelected: ((teamId: Long?) -> Unit)? = null

    override fun create(context: Context) {
        super.create(context)
        isEnabled = false
    }

    suspend fun loadItems() {
        val teams = withContext(Dispatchers.Default) {
            val list = mutableListOf<Item>()

            if (showNoTeam) {
                list += Item(
                        -1L,
                        context.getString(R.string.dialog_event_manual_no_team),
                        tag = -1L
                )
            }

            val teams = db.teamDao().getAllNow(profileId)

            list += teams.map { Item(
                    it.id,
                    it.name,
                    tag = it.id
            ) }

            list
        }

        clear().append(teams)
        isEnabled = true

        setOnChangeListener {
            when (it.tag) {
                -1L -> {
                    // no team
                    onTeamSelected?.invoke(null)
                    true
                }
                is Long -> {
                    // selected a team
                    onTeamSelected?.invoke(it.tag)
                    true
                }
                else -> false
            }
        }
    }

    fun selectTeam(teamId: Long) {
        if (select(teamId) == null)
            select(Item(
                    teamId,
                    "nieznana grupa ($teamId)",
                    tag = teamId
            ))
    }

    fun selectDefault(teamId: Long?) {
        if (teamId == null || selected != null)
            return
        selectTeam(teamId)
    }

    fun selectTeamClass() {
        select(items.singleOrNull {
            it.tag is Team && it.tag.type == Team.TYPE_CLASS
        })
    }

    /**
     * Get the currently selected team.
     * ### Returns:
     * - null if no valid team is selected
     * - [Long] - the team's ID
     */
    fun getSelected(): Any? {
        return when (selected?.tag) {
            -1L -> null
            is Long -> selected?.tag as Long
            else -> null
        }
    }
}
