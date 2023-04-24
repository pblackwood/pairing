package com.artisan.pairing

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
}
