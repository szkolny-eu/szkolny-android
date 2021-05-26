/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-8.
 */

package pl.szczodrzynski.edziennik.utils

import pl.szczodrzynski.edziennik.utils.models.Date
import kotlin.math.absoluteValue

// Obliczanie daty wielkanocy - algorytm Gaussa
// www.algorytm.org
// (c) 2008 by Tomasz Lubinski
// http://www.algorytm.org/przetwarzanie-dat/wyznaczanie-daty-wielkanocy-algortym-gaussa/dwg-j.html

class BigNightUtil {

    /* Pobierz wartosc A z tabeli lat */
    private fun getA(rok: Int) = when {
        rok <= 1582 -> 15
        rok <= 1699 -> 22
        rok <= 1899 -> 23
        rok <= 2199 -> 24
        rok <= 2299 -> 25
        rok <= 2399 -> 26
        rok <= 2499 -> 25
        else -> 0
    }

    /* Pobierz wartosc B z tabeli lat */
    private fun getB(rok: Int) = when {
        rok <= 1582 -> 6
        rok <= 1699 -> 2
        rok <= 1799 -> 3
        rok <= 1899 -> 4
        rok <= 2099 -> 5
        rok <= 2199 -> 6
        rok <= 2299 -> 0
        rok <= 2499 -> 1
        else -> 0
    }

    /* oblicz ile dni po 22 marca przypada wielkanoc */
    private fun Oblicz_Date_wielkanocy(rok: Int): Int {
        val a = rok % 19
        val b = rok % 4
        val c = rok % 7
        var d = (a * 19 + getA(rok)) % 30
        val e = (2 * b + 4 * c + 6 * d + getB(rok)) % 7
        if (d == 29 && e == 6 || d == 28 && e == 6) {
            d -= 7
        }
        return d + e
    }

    private fun get_dataOf_bigNight(): Date {
        val date = Date.getToday()
        date.month = 4
        date.day = 22 + Oblicz_Date_wielkanocy(date.year)
        if (date.day > 31)
            date.day = date.day % 31
        else
            date.month = 3

        return date
    }

    fun isDataWielkanocyNearDzisiaj() =
        Date.diffDays(Date.getToday(), get_dataOf_bigNight()).absoluteValue < 7
}
