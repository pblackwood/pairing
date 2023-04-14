package com.artisan.pairing

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.stream.Stream

@ExtendWith(MockKExtension::class)
class PairingTest {

    @MockK
    lateinit var files: Files

    lateinit var pairing: Pairing

    private val standardIn = System.`in`
    private val standardOut = System.out
    private val players = listOf(
        Player(32, "Bob", "Robertson"),
        Player(9, "Amy", status="out")
    )
    private val rounds = listOf(
        Round(0)
    )

    @BeforeEach
    fun setUp() {
        every { files.readPlayers() } returns players
        every { files.readRounds() } returns rounds
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
        verify { files.readPlayers() }
        assertEquals(players, pairing.playerList)
    }

    @Test
    fun `can list all players from a file`() {
        val outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))
        val commandWithQuit = "l\nq\n"
        val inputStream = ByteArrayInputStream(commandWithQuit.toByteArray())
        System.setIn(inputStream)
        pairing.inputLoop()
        assertEquals("""
            > STILL IN
            32 Bob Robertson
            OUT
            9 Amy 
            > 
                     """.trimIndent(),
            outputStream.toString()
        )
    }

    @ParameterizedTest(name ="{0}")
    @MethodSource("commands")
    fun `Test all commands`(
        name: String,
        command: String,
        player: Player?,
        expectedPlayerList: List<Player>?,
        expectedRoundList: List<Round>?,
        appendPlayerTimes: Int,
        writePlayersTimes: Int
    ) {
        val commandWithQuit = "%s\nq\n".format(command)
        val inputStream = ByteArrayInputStream(commandWithQuit.toByteArray())
        System.setIn(inputStream)
        pairing.inputLoop()
        verify { files.readPlayers() }
        if (player != null) {
            verify(exactly = appendPlayerTimes) { files.appendPlayer("players.txt", player) }
        }
//        if (expectedRoundList != null) {
//            verify(exactly = appendRoundTimes) { files.appendRound("players.txt", player) }
//        }
        if (expectedPlayerList != null) {
            verify(exactly = writePlayersTimes) { files.writePlayers("players.txt", expectedPlayerList) }
            assertEquals(expectedPlayerList, pairing.playerList)
        }
    }

    companion object {
        @JvmStatic
        private fun commands(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    "Can add a player to the list",
                    "a Kent Beck",
                    null,
                    mutableListOf(
                        Player(32, "Bob", "Robertson", "in"),
                        Player(9, "Amy", status = "out"),
                        Player(3, "Kent", "Beck")
                    ),
                    null,
                    1,
                    0
                ),
                Arguments.of(
                    "Can mark a player 'out'",
                    "d 32",
                    null,
                    mutableListOf(
                        Player(32, "Bob", "Robertson", "out"),
                        Player(9, "Amy", status = "out")
                    ),
                    null,
                    0,
                    1
                ),
                Arguments.of(
                    "Can start a new round",
                    "r",
                    null,
                    null,
                    mutableListOf(
                        Round(0),
                        Round(1)
                    ),
                    0,
                    0
                )
            )

        }
    }
}