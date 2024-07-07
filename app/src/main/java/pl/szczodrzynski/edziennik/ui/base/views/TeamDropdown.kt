/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-7.
 */

package pl.szczodrzynski.edziennik.ui.base.views

import android.content.Context
import android.util.AttributeSet
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

    lateinit var db: AppDb
    var profileId: Int = 0
    var showNoTeam = true
    var onTeamSelected: ((team: Team?) -> Unit)? = null

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
                    tag = it
            ) }

            list
        }

        clear().append(teams)
        isEnabled = true

        setOnChangeListener {
            when (it.tag) {
                -1L -> {
                    // no team
                    deselect()
                    onTeamSelected?.invoke(null)
                    false
                }
                is Team -> {
                    // selected a team
                    onTeamSelected?.invoke(it.tag)
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Select a teacher by the [teamId].
     */
    fun selectTeam(teamId: Long): Item? {
        if (teamId == -1L) {
            deselect()
            return null
        }
        return select(teamId)
    }

    /**
     * Select a team by the [teamId] **if it's not selected yet**.
     */
    fun selectDefault(teamId: Long?): Item? {
        if (teamId == null || selected != null)
            return null
        return selectTeam(teamId)
    }

    /**
     * Select a team of the [Team.TYPE_CLASS] type.
     */
    fun selectTeamClass() {
        select(items.singleOrNull {
            it.tag is Team && it.tag.type == Team.TYPE_CLASS
        })
    }

    /**
     * Get the currently selected team.
     * ### Returns:
     * - null if no valid team is selected
     * - [Team] - the selected team
     */
    fun getSelected(): Team? {
        return when (selected?.tag) {
            -1L -> null
            is Team -> selected?.tag as Team
            else -> null
        }
    }
}
