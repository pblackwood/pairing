package com.artisan.pairing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoundTest {

    lateinit var round: Round

    @Test
    fun `A round can serialize itself`() {
        round = Round(12, listOf(3, 5, 6, 9), 1, listOf(Pair(5,9),Pair(6,3)))
        assertEquals("12;3,5,6,9;1;5,9|6,3", round.toString())
    }

    @Test
    fun `The playerIds list can be missing`() {
        round = Round(12, byeId=4, pairs = listOf(Pair(1,2), Pair(3,4)))
        assertEquals("12;;4;1,2|3,4", round.toString())
    }

    @Test
    fun `The byeId can be missing`() {
        round = Round(12, listOf(9), pairs = listOf(Pair(1, 2)))
        assertEquals("12;9;;1,2", round.toString())
    }

    @Test
    fun `Both playerIds and byeId can be missing`() {
        round = Round(12, pairs = listOf(Pair(1,2), Pair(3,4)))
        assertEquals("12;;;1,2|3,4", round.toString())
    }

    @Test
    fun `The pairs list can be missing`() {
        round = Round(12, listOf(3, 5), byeId=4)
        assertEquals("12;3,5;4;", round.toString())
    }

    @Test
    fun `Everything can be missing except the id`() {
        round = Round(12)
        assertEquals("12;;;", round.toString())
    }

}