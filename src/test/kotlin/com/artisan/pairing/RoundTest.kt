package com.artisan.pairing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoundTest {

    lateinit var round: Round

    @Test
    fun `A round can serialize itself`() {
        round = Round(12, listOf(3, 5, 6), 1)
        assertEquals("12;3,5,6;1", round.toString())
    }

    @Test
    fun `The playerIds list can be missing`() {
        round = Round(12, byeId=4)
        assertEquals("12;;4", round.toString())
    }

    @Test
    fun `The byeId can be missing`() {
        round = Round(12, listOf(9))
        assertEquals("12;9;", round.toString())
    }

    @Test
    fun `Both playerIds and byeId can be missing`() {
        round = Round(12)
        assertEquals("12;;", round.toString())
    }
}