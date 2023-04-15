package com.artisan.pairing

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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

    private val standardIn = System.`in`
    private val standardOut = System.out

    @Nested
    @DisplayName("Adding and Removing Players")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PlayerTests {
        lateinit var pairing: Pairing

        @MockK
        lateinit var files: Files

        private lateinit var players: List<Player>

        @BeforeEach
        fun setUp() {
            players = listOf(
                Player(32, "Bob", "Robertson"),
                Player(9, "Amy", status = "out")
            )
            every { files.readPlayers("players.txt") } returns players.toMutableList()
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
            verify {
                files.readPlayers("players.txt")
                assertEquals(players, pairing.playerList)
            }
        }

        @Test
        fun `can list all players from a file`() {
            val outputStream = ByteArrayOutputStream()
            System.setOut(PrintStream(outputStream))
            val commandWithQuit = "l\nq\n"
            val inputStream = ByteArrayInputStream(commandWithQuit.toByteArray())
            System.setIn(inputStream)
            pairing.inputLoop()
            assertEquals(
                """
            > STILL IN
            32 Bob Robertson
            OUT
            9 Amy 
            > 
                """.trimIndent(),
                outputStream.toString()
            )
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("playerCommands")
        fun `Test player-oriented commands`(
            name: String,
            command: String,
            player: Player?,
            expectedPlayerList: List<Player>,
            appendPlayerTimes: Int,
            writePlayersTimes: Int
        ) {
            val commandWithQuit = "%s\nq\n".format(command)
            val inputStream = ByteArrayInputStream(commandWithQuit.toByteArray())
            System.setIn(inputStream)
            pairing.inputLoop()
            verify { files.readPlayers("players.txt") }
            if (player != null) {
                verify(exactly = appendPlayerTimes) { files.appendPlayer("players.txt", player) }
            }
            verify(exactly = writePlayersTimes) { files.writePlayers("players.txt", expectedPlayerList) }
            assertEquals(expectedPlayerList, pairing.playerList)
        }

        fun playerCommands(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    "Can add a player to the list",
                    "a Kent Beck",
                    Player(3, "Kent", "Beck"),
                    mutableListOf(
                        Player(32, "Bob", "Robertson", "in"),
                        Player(9, "Amy", status = "out"),
                        Player(3, "Kent", "Beck")
                    ),
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
                    0,
                    1
                ),
            )
        }
    }

    @Nested
    @DisplayName("Creating and Changing Rounds")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RoundTests {
        lateinit var pairing: Pairing

        @MockK
        lateinit var files: Files

        private lateinit var players: List<Player>

        private lateinit var rounds: List<Round>

        @BeforeEach
        fun setUp() {
            players = listOf(
                Player(2, "Bob", "Robertson"),
                Player(3, "Bill", "Carpenter"),
                Player(9, "Amy", status = "out")
            )

            rounds = listOf(
                Round(0)
            )

            every { files.readPlayers("players.txt") } returns players
            every { files.readRounds("rounds.txt") } returns rounds
            pairing = Pairing(files)
        }

        @AfterEach
        fun tearDown() {
            System.setIn(standardIn)
            System.setOut(standardOut)
        }

        @Test
        fun `Does nothing interesting yet`() {
            assertEquals(1, 1)
        }

        fun roundCommands(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    "Can start a new round",
                    "r",
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
