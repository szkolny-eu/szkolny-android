/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-10.
 */

package pl.szczodrzynski.edziennik.ui.modules.search

interface Searchable<in T> : Comparable<Searchable<*>> {

    /**
     * A prioritized list of keywords sets. First items are of the highest priority.
     * Items within a keyword set have the same priority.
     */
    val searchKeywords: List<List<String?>?>

    /**
     * A priority assigned by [SearchFilter]. Lower numbers mean a higher priority.
     */
    var searchPriority: Int

    /**
     * The text to be highlighted when filtering.
     */
    var searchHighlightText: CharSequence?
}
