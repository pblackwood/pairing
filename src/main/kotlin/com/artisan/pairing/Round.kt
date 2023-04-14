package com.artisan.pairing

data class Round (
    val id: Int,
    val playerIds: List<Int> = listOf(),
    val byeId: Int? = null
) {
    override fun toString(): String =
        "%d;%s;%s".format(id, playerIds.joinToString(separator=","), byeId?.toString() ?: "")
}
