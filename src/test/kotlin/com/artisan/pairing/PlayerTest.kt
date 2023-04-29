package com.artisan.pairing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlayerTest {

    lateinit var player: Player

    @Test
    fun `A player can serialize itself`() {
        player = Player(12, "Bob", "Robbie", "x", mutableListOf(3, 2, 5), 35)
        assertEquals("12;Bob;Robbie;x;3,2,5;35", player.toString())
    }

    @Test
    fun `A player can serialize itself shortened`() {
        player = Player(12, "Bob", "Robbie")
        assertEquals("12;Bob;Robbie;in;;10", player.toString())
    }

    @Test
    fun `Last name can be missing, but status defaults to 'in'`() {
        player = Player(23, "Mary")
        assertEquals("23;Mary;;in;;10", player.toString())
    }

    @Test
    fun `A player has a readable name`() {
        player = Player(12, "Bob", "Robbie", "x")
        assertEquals("Bob Robbie", player.fullName())
    }

    @Test
    fun `A player has a details display`() {
        Settings.chipValue = 10
        player = Player(12, "Bob", "Robbie", "in", chipCount = 35)
        assertEquals("Bob Robbie (35)", player.details())
        assertEquals("12 Bob Robbie (35)", player.details(withId=true))
        assertEquals("12 Bob Robbie (35) = $350", player.details(withId=true, withMoney=true, moneyPercent=1.0))
        assertEquals("Bob Robbie (35) = $350", player.details(withMoney=true, moneyPercent=1.0))
    }

    @Test
    fun `A player has a details display, allowing for fees`() {
        Settings.chipValue = 20
        player = Player(12, "Bob", "Robbie", "in", chipCount = 35)
        assertEquals("Bob Robbie (35)", player.details())
        assertEquals("12 Bob Robbie (35)", player.details(withId=true))
        assertEquals("12 Bob Robbie (35) = $630", player.details(withId=true, withMoney=true, moneyPercent = .9))
        assertEquals("Bob Robbie (35) = $630", player.details(withMoney=true, moneyPercent = .9))
    }
}