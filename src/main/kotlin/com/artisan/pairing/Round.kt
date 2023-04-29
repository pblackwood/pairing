package com.artisan.pairing

data class Round (
    val id: Int,
    val playerIds: List<Int> = listOf(),
    var byeId: Int? = null,
    var pairs: List<Pair<Int, Int>>? = listOf()
) {
    override fun toString(): String =
        "%d;%s;%s;%s".format(
            id,
            playerIds.joinToString(separator=","),
            byeId?.toString() ?: "",
            pairs?.joinToString(separator = "|") { p ->
                p.toList().joinToString(separator = ",")
            }
        )

    fun printRoundDetails(playerList: List<Player>) {
        val byePlayer: Player? = if (byeId == null) null else playerList.find { p -> p.id == byeId }
        println("Round %d".format(id))
//        println("Available players: %s".format(playerIds))
        println("%s".format(if (byePlayer == null) "NO BYE" else "BYE: %s".format(byePlayer.fullName())))
        println("PAIRS:")
        pairs?.forEach {
            println("%s vs %s".format(
                playerList.find { p -> p.id == it.first }?.details(withId = true),
                playerList.find { p -> p.id == it.second }?.details(withId = true)
            ))
        }
    }
}
