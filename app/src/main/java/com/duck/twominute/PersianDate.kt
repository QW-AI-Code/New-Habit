package com.duck.twominute

import java.time.LocalDate

/** Jalali (Solar Hijri) calendar conversion based on the 33-year cycle algorithm. */
data class JalaliDate(val year: Int, val month: Int, val day: Int) {
    override fun toString(): String = "%04d/%02d/%02d".format(year, month, day)
}

/** Converts Arabic and Latin digits to Persian digits for Persian UI surfaces. */
fun String.toPersianDigits(): String = map { ch ->
    when (ch) {
        '0' -> '۰'; '1' -> '۱'; '2' -> '۲'; '3' -> '۳'; '4' -> '۴'
        '5' -> '۵'; '6' -> '۶'; '7' -> '۷'; '8' -> '۸'; '9' -> '۹'
        else -> ch
    }
}.joinToString("")

fun Int.toPersianDigits(): String = toString().toPersianDigits()

object PersianDate {
    val monthNamesFa = listOf("فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")
    private val breaks = intArrayOf(-61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178)
    private fun div(a: Int, b: Int): Int = a / b
    private fun mod(a: Int, b: Int): Int = a - (a / b) * b
    private fun jalCal(jalaliYear: Int): IntArray {
        var leapJ = -14; var jp = breaks[0]; var jump = 0
        for (index in 1 until breaks.size) { val jm = breaks[index]; jump = jm - jp; if (jalaliYear < jm) break; leapJ += div(jump, 33) * 8 + div(mod(jump, 33), 4); jp = jm }
        var n = jalaliYear - jp; leapJ += div(n, 33) * 8 + div(mod(n, 33) + 3, 4)
        if (mod(jump, 33) == 4 && jump - n == 4) leapJ += 1
        val gregorianYear = jalaliYear + 621; val leapG = div(gregorianYear, 4) - div((div(gregorianYear, 100) + 1) * 3, 4) - 150; val march = 20 + leapJ - leapG
        if (jump - n < 6) n = n - jump + div(jump + 4, 33) * 33
        var leap = mod(mod(n + 1, 33) - 1, 4); if (leap == -1) leap = 4
        return intArrayOf(leap, gregorianYear, march)
    }
    private fun gregorianToDayNumber(year: Int, month: Int, day: Int): Int {
        var d = div((year + div(month - 8, 6) + 100100) * 1461, 4) + div(153 * mod(month + 9, 12) + 2, 5) + day - 34840408
        d -= div(div(year + 100100 + div(month - 8, 6), 100) * 3, 4) - 752
        return d
    }
    private fun dayNumberToGregorian(dayNumber: Int): IntArray {
        var j = 4 * dayNumber + 139361631; j += div(div(4 * dayNumber + 183187720, 146097) * 3, 4) * 4 - 3908
        val i = div(mod(j, 1461), 4) * 5 + 308; val day = div(mod(i, 153), 5) + 1; val month = mod(div(i, 153), 12) + 1; val year = div(j, 1461) - 100100 + div(8 - month, 6)
        return intArrayOf(year, month, day)
    }
    private fun jalaliToDayNumber(year: Int, month: Int, day: Int): Int { val cal = jalCal(year); return gregorianToDayNumber(cal[1], 3, cal[2]) + (month - 1) * 31 - div(month, 7) * (month - 7) + day - 1 }
    fun isLeapYear(jalaliYear: Int): Boolean = jalCal(jalaliYear)[0] == 0
    fun monthLength(jalaliYear: Int, jalaliMonth: Int): Int = when { jalaliMonth <= 6 -> 31; jalaliMonth <= 11 -> 30; isLeapYear(jalaliYear) -> 30; else -> 29 }
    fun fromGregorian(date: LocalDate): JalaliDate {
        val dayNumber = gregorianToDayNumber(date.year, date.monthValue, date.dayOfMonth); val gregorianYear = dayNumberToGregorian(dayNumber)[0]; var jalaliYear = gregorianYear - 621; val cal = jalCal(jalaliYear); val firstDayNumber = gregorianToDayNumber(gregorianYear, 3, cal[2]); var k = dayNumber - firstDayNumber
        if (k >= 0) { if (k <= 185) return JalaliDate(jalaliYear, 1 + div(k, 31), mod(k, 31) + 1); k -= 186 } else { jalaliYear -= 1; k += 179; if (cal[0] == 1) k += 1 }
        return JalaliDate(jalaliYear, 7 + div(k, 30), mod(k, 30) + 1)
    }
    fun toGregorian(date: JalaliDate): LocalDate { val parts = dayNumberToGregorian(jalaliToDayNumber(date.year, date.month, date.day)); return LocalDate.of(parts[0], parts[1], parts[2]) }
    fun monthName(month: Int): String = monthNamesFa[(month - 1).coerceIn(0, 11)]
}
