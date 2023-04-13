package com.artisan.pairing

data class Player(
    val id: Int,
    val first: String,
    val last: String = "",
    var status: String = "in",
    val pairingList: List<Player> = listOf()
) {
    override fun toString(): String = "%d,%s,%s,%s".format(id, first, last, status)

    fun fullName(): String = "%s %s".format(first, last)
}
