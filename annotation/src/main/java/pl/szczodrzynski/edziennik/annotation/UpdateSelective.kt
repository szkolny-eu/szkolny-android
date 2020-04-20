/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-28. 
 */

package pl.szczodrzynski.edziennik.annotation

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class UpdateSelective(
        val primaryKeys: Array<String>,
        val skippedColumns: Array<String> = []
)
