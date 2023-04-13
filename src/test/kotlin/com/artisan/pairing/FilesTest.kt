package com.artisan.pairing

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class FilesTest {

    lateinit var files: Files

    @BeforeEach
    fun setUp() {
        files = Files()
    }

    @AfterEach
    fun tearDown() {
        File("files", "abc.txt").delete()
    }

    @Test
    fun `it can read a player from a file`() {
        File("files", "abc.txt").appendText("3,four,,in\n")
        val players = files.read("abc.txt")
        assertEquals(listOf(Player(3, "four")), players)
    }

    @Test
    fun `it can read multiple players from a file`() {
        File("files", "abc.txt").appendText("3,four,,in\n5,six,seven,out\n")
        val players = files.read("abc.txt")
        assertEquals(listOf(
            Player(3, "four"),
            Player(5, "six", "seven", "out")
        ), players)
    }

    @Test
    fun `it can read a file with a blank line`() {
        File("files", "abc.txt").appendText("3,four,,in\n\n5,six,seven,out\n")
        val players = files.read("abc.txt")
        assertEquals(listOf(
            Player(3, "four"),
            Player(5, "six", "seven", "out")
        ), players)
    }

    @Test
    fun `it can append a player to a file`() {
        File("files", "abc.txt").appendText("3,four,,in\n")
        files.appendPlayer("abc.txt", Player(1, "two", "three", "four"))
        assertEquals("3,four,,in\n1,two,three,four\n", File("files", "abc.txt").readText())
    }

    @Test
    fun `it can write multiple players to a file, starting with a new file`() {
        File("files", "abc.txt").appendText("3,four,,in\n")
        files.writePlayers("abc.txt", listOf(
            Player(1, "two", "three", "four"),
            Player(5, "six")
        ))
        assertEquals("1,two,three,four\n5,six,,in\n", File("files", "abc.txt").readText())
    }
}