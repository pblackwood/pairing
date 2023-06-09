package com.artisan.pairing

import java.io.File
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FilesTest {

    lateinit var files: Files

    @BeforeEach
    fun setUp() {
        files = Files()
    }

    @AfterEach
    fun tearDown() {
        File("files", "abc.txt").delete()
        File("files", "xyz.txt").delete()
    }

    @Test
    fun `it can read a player from a file`() {
        File("files", "abc.txt").appendText("3;four;;in;;10\n")
        val players = files.readPlayers("abc.txt")
        assertEquals(listOf(Player(3, "four")), players)
    }

    @Test
    fun `it can read multiple players from a file`() {
        File("files", "abc.txt").appendText("3;four;;in;;25\n5;six;seven;out;4,5;0\n")
        val players = files.readPlayers("abc.txt")
        assertEquals(listOf(
            Player(3, "four", chipCount = 25),
            Player(5, "six", "seven", "out", pairs = mutableListOf(4, 5), chipCount = 0)
        ), players)
    }

    @Test
    fun `it can read a file with a blank line`() {
        File("files", "abc.txt").appendText("3;four;;in;;10\n\n5;six;seven;out;;20\n")
        val players = files.readPlayers("abc.txt")
        assertEquals(listOf(
            Player(3, "four"),
            Player(5, "six", "seven", "out", chipCount=20)
        ), players)
    }

    @Test
    fun `it can append a player to a file`() {
        File("files", "abc.txt").appendText("3;four;;in;;10\n")
        files.appendPlayer("abc.txt", Player(1, "two", "three", "four", mutableListOf(4)))
        assertEquals("3;four;;in;;10\n1;two;three;four;4;10\n", File("files", "abc.txt").readText())
    }

    @Test
    fun `it can write multiple players to a file, wiping out previous contents`() {
        File("files", "abc.txt").appendText("3;four;;in;20\n")
        files.writePlayers("abc.txt", listOf(
            Player(1, "two", "three", "four"),
            Player(5, "six")
        ))
        assertEquals("1;two;three;four;;10\n5;six;;in;;10\n", File("files", "abc.txt").readText())
    }

    @Test
    fun `a group of Players can be written and read from a file and get the same Players back`() {
        val expectedPlayers = listOf(
            Player(3, "four", chipCount = 25),
            Player(5, "six", "seven", "out", pairs = mutableListOf(4, 5), chipCount = 0),
            Player(6, "Mary", pairs = mutableListOf(6, 7))
        )
        files.writePlayers("xyz.txt", expectedPlayers)
        val retrievedPlayers = files.readPlayers("xyz.txt")
        assertEquals(expectedPlayers, retrievedPlayers)
    }

    @Test
    fun `it can read a round from a file`() {
        File("files", "xyz.txt").appendText("1;5,6,7;10;\n")
        val rounds = files.readRounds("xyz.txt")
        assertEquals(listOf(
            Round(1, listOf(5,6,7), 10)
        ), rounds)
    }

    @Test
    fun `it can read multiple rounds from a file`() {
        File("files", "xyz.txt").appendText("1;5,6,7;10;\n2;;12;1,2|3,4\n3;7;;\n")
        val rounds = files.readRounds("xyz.txt")
        assertEquals(listOf(
            Round(1, listOf(5,6,7), 10),
            Round(2, listOf(), 12, listOf(Pair(1,2), Pair(3,4))),
            Round(3, listOf(7), null)
        ), rounds)
    }

    @Test
    fun `a group of Rounds can be written and read from a file and get the same Rounds back`() {
        val expectedRounds = listOf(
            Round(1, listOf(3, 4, 5), 2, listOf(Pair(4,5), Pair(6,7), Pair(8,9))),
            Round(2, listOf(), 2),
            Round(3, listOf(3, 4, 5), pairs = listOf(Pair(4,5))),
            Round(4, listOf()),
        )
        files.writeRounds("xyz.txt", expectedRounds)
        val retrievedRounds = files.readRounds("xyz.txt")
        assertEquals(expectedRounds, retrievedRounds)
    }

    @Test
    fun `it can read all the previous bye ids from a file`() {
        File("files", "xyz.txt").appendText("6,7,10\n")
        val byes = files.readByes("xyz.txt")
        assertEquals(listOf(6, 7, 10), byes)
    }

    @Test
    fun `it can append a bye id to a file`() {
        File("files", "abc.txt").appendText("3\n")
        files.appendBye("abc.txt", 12)
        assertEquals("3,12", File("files", "abc.txt").readText())
    }

    @Test
    fun `it can append a bye id to a blank file`() {
        File("files", "abc.txt").createNewFile()
        files.appendBye("abc.txt", 12)
        assertEquals("12", File("files", "abc.txt").readText())
    }

    @Test
    fun `a group of bye ids can be written and read from a file and get the same ones back`() {
        val expectedByes = listOf(3, 9, 12)
        files.writeByes("xyz.txt", expectedByes)
        val retrievedByes = files.readByes("xyz.txt")
        assertEquals(expectedByes, retrievedByes)
    }

}