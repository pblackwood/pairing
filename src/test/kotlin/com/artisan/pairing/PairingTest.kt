package com.artisan.pairing

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.PrintStream

@ExtendWith(MockKExtension::class)
class PairingTest {

    @MockK
    lateinit var files: Files

    lateinit var pairing: Pairing

    private val standardIn = System.`in`;
    private val standardOut = System.out;
    private val players = listOf(
        Player(32, "Bob", "Robertson"),
        Player(9, "Amy", status="out")
    )

    @BeforeEach
    fun setUp() {
        every { files.read("players.txt") } returns players
        every { files.appendPlayer("players.txt", any()) } returns Unit
        every { files.writePlayers("players.txt", any()) } returns Unit
        pairing = Pairing(files)
    }

    @AfterEach
    fun tearDown() {
        System.setIn(standardIn)
        System.setOut(standardOut)
    }

    @Test
    fun `on load, reads player list from a file`() {
        verify { files.read("players.txt") }
        assertEquals(players, pairing.playerList)
    }

    @Test
    fun `can list all players from a file`() {
        val outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))
        pairing.listPlayers()
        assertEquals("""
            STILL IN
            32 Bob Robertson
            OUT
            9 Amy 
            
                     """.trimIndent(),
            outputStream.toString()
        )
    }

    @Test
    fun `can add a player to the list`() {
        val expectedPlayerList: MutableList<Player> = players.toMutableList()
        expectedPlayerList.add(Player(3, "Kent", "Beck"))
        pairing.addPlayer("a Kent Beck")
        verify { files.read("players.txt") }
        verify { files.appendPlayer("players.txt", Player(3, "Kent", "Beck")) }
        assertEquals(expectedPlayerList, pairing.playerList)
    }

    @Test
    fun `can add a player to the list from the input loop`() {
        val inputStream = ByteArrayInputStream("a Kent Beck\nq\n".toByteArray())
        System.setIn(inputStream)
        val expectedPlayerList: MutableList<Player> = players.toMutableList()
        expectedPlayerList.add(Player(3, "Kent", "Beck"))
        pairing.inputLoop()
        verify { files.read("players.txt") }
        verify { files.appendPlayer("players.txt", Player(3, "Kent", "Beck")) }
        assertEquals(expectedPlayerList, pairing.playerList)
    }

    @Test
    fun `can mark a player 'out' in the list`() {
        val expectedPlayerList = mutableListOf(
            Player(32, "Bob", "Robertson", "out"),
            Player(9, "Amy", status="out")
        )
        pairing.removePlayer("d 32")
        verify { files.read("players.txt") }
        verify { files.writePlayers("players.txt", expectedPlayerList) }
        assertEquals(expectedPlayerList, pairing.playerList)
    }

    @Test
    fun `can mark a player 'out' from the input loop`() {
        val inputStream = ByteArrayInputStream("d 32\nq\n".toByteArray())
        System.setIn(inputStream)
        val expectedPlayerList = mutableListOf(
            Player(32, "Bob", "Robertson", "out"),
            Player(9, "Amy", status="out")
        )
        pairing.inputLoop()
        verify { files.read("players.txt") }
        verify { files.writePlayers("players.txt", expectedPlayerList) }
        assertEquals(expectedPlayerList, pairing.playerList)
    }

//    @ParameterizedTest(name =
//    @MethodSource
//

}