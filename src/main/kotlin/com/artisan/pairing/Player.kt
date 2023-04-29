package com.artisan.pairing

import kotlin.math.roundToInt

data class Player(
    val id: Int,
    val first: String,
    val last: String = "",
    var status: String = "in",
    val pairs: MutableList<Int> = mutableListOf(),
    var chipCount: Int = Settings.buyInChipCount
) {
    override fun toString(): String = "%d;%s;%s;%s;%s;%d".format(id, first, last, status, pairs.joinToString(separator=","), chipCount)

    fun fullName(): String = "%s %s".format(first, last)

    fun details(withId: Boolean = false, withMoney: Boolean = false, moneyPercent: Double = 0.0): String {
        var details = if (withId) {
            "%d %s %s (%d)".format(id, first, last, chipCount)
        } else {
            "%s %s (%d)".format(first, last, chipCount)
        }
        if (withMoney) {
            details = "%s = $%d".format(details, (chipCount * Settings.chipValue * moneyPercent).roundToInt())
        }
        return details
    }
}
