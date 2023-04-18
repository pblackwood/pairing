package com.artisan.pairing

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
import kotlin.random.Random

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
            every { files.readRounds("rounds.txt") } returns emptyList()
            every { files.readByes("byes.txt") } returns emptyList()
            every { files.appendPlayer("players.txt", any()) } returns Unit
            every { files.writePlayers("players.txt", any()) } returns Unit
            pairing = Pairing(files)
        }

        @AfterEach
        fun tearDown() {
            System.setIn(standardIn)
            System.setOut(standardOut)
            clearAllMocks()
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

        private fun playerCommands(): Stream<Arguments> {
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

        @MockK
        lateinit var random: Random.Default

        private lateinit var players: List<Player>

        private lateinit var rounds: List<Round>

        private lateinit var byes: List<Int>

        @BeforeEach
        fun setUp() {
            players = listOf(
                Player(2, "Bob", "Robertson"),
                Player(3, "Bill", "Carpenter"),
                Player(4, "Mary", "Smith"),
                Player(5, "Betty", "Smythe"),
                Player(9, "Amy", status = "out")
            )

            rounds = emptyList()
            byes = emptyList()

            every { files.readPlayers("players.txt") } returns players
            every { files.readRounds("rounds.txt") } returns rounds
            every { files.appendRound("rounds.txt", any()) } returns Unit
            every { files.readByes("byes.txt") } returns byes
            every { files.writeByes("byes.txt", any()) } returns Unit
            every { files.writePlayers("players.txt", any()) } returns Unit
            pairing = Pairing(files, random)
        }

        @AfterEach
        fun tearDown() {
            System.setIn(standardIn)
            System.setOut(standardOut)
            clearAllMocks()
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("roundCommands")
        fun `Test round-oriented commands`(
            name: String,
            command: String,
            round: Round?,
            extraPlayers: List<Player>?,
            expectedRoundList: List<Round>,
            appendRoundTimes: Int,
            writeRoundsTimes: Int,
            randomNumber: Int?,
        ) {
            val commandWithQuit = "%s\nq\n".format(command)
            val inputStream = ByteArrayInputStream(commandWithQuit.toByteArray())
            System.setIn(inputStream)
            if (extraPlayers != null) {
                val newPlayerList = players.toMutableList()
                newPlayerList.addAll(extraPlayers)
                every { files.readPlayers("players.txt") } returns newPlayerList
            }
            every { random.nextInt(any()) } returns (randomNumber ?: 1)
            pairing = Pairing(files, random)
            pairing.inputLoop()
            verify { files.readRounds("rounds.txt") }
            assertEquals(expectedRoundList, pairing.rounds)
            if (round != null) {
                verify(exactly = appendRoundTimes) { files.appendRound("rounds.txt", round = round) }
            }
            verify(exactly = writeRoundsTimes) { files.writeRounds("rounds.txt", expectedRoundList) }
            assertEquals(expectedRoundList, pairing.rounds)
        }

        private fun roundCommands(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    "Can start a new round",
                    "r",
                    Round(1, listOf(2, 3, 4, 5)),
                    null,
                    mutableListOf(
                        Round(1, listOf(2, 3, 4, 5))
                    ),
                    1,
                    0,
                    null
                ),
                Arguments.of(
                    "With an even number of players, there should not be a bye",
                    "r",
                    Round(1, listOf(2, 3, 4, 5)),
                    null,
                    mutableListOf(
                        Round(1, listOf(2, 3, 4, 5), null)
                    ),
                    1,
                    0,
                    null
                ),
                Arguments.of(
                    "With an odd number of players, there should be a bye",
                    "r",
                    Round(1, listOf(2, 3, 4, 5, 10), 5),
                    listOf(Player(10, "Peter", "Parker")),
                    mutableListOf(
                        Round(1, listOf(2, 3, 4, 5, 10), 5)
                    ),
                    1,
                    0,
                    3
                ),
            )
        }

        @Test
        fun`A bye should not go to the same player twice`() {
            val myPlayers = players.toMutableList()
            myPlayers.add(Player(6, "first", "last"))
            every { files.readPlayers("players.txt") } returns myPlayers
            val myRounds = rounds.toMutableList()
            myRounds.add(Round(1, listOf(2, 3, 4, 5, 6), 5))
            every { files.readRounds("rounds.txt") } returns myRounds
            val myByes = emptyList<Int>().toMutableList()
            every { files.readByes("byes.txt") } returns myByes

            System.setIn(ByteArrayInputStream("r\nq\n".toByteArray()))
            every { random.nextInt(5) } returns 3
            pairing = Pairing(files, random)
            pairing.inputLoop()
            assertEquals(2, pairing.rounds.size)
            assertEquals(5, pairing.rounds.last().byeId)
            assertEquals(listOf(5), pairing.byes)

            System.setIn(ByteArrayInputStream("r\nq\n".toByteArray()))
            every { random.nextInt(4) } returns 3
            pairing.inputLoop()
            assertEquals(3, pairing.rounds.size)
            assertEquals(6, pairing.rounds.last().byeId)
            assertEquals(listOf(5, 6), pairing.byes)
        }

        @Test
        fun`If all remaining players have had a bye, choose the oldest one and move oldest to newest`() {
            val myPlayers = players.subList(0, 3).toMutableList()
            every { files.readPlayers("players.txt") } returns myPlayers
            val myRounds = rounds.toMutableList()
            myRounds.add(Round(1, listOf(2, 3, 4), 3))
            every { files.readRounds("rounds.txt") } returns myRounds
            val myByes = mutableListOf(2, 3, 4)
            every { files.readByes("byes.txt") } returns myByes

            System.setIn(ByteArrayInputStream("r\nq\n".toByteArray()))
            pairing = Pairing(files)
            pairing.inputLoop()
            assertEquals(2, pairing.rounds.size)
            assertEquals(2, pairing.rounds.last().byeId)
            assertEquals(listOf(3, 4, 2), pairing.byes)
        }

        @Test
        fun`Marking a player 'out' also means the player is removed from the bye list if present`() {
            val myByes = mutableListOf(2, 3, 4)
            every { files.readByes("byes.txt") } returns myByes
            System.setIn(ByteArrayInputStream("d 3\nq\n".toByteArray()))
            pairing = Pairing(files)
            pairing.inputLoop()
            assertEquals(listOf(2, 4), pairing.byes)
        }

        @Test
        fun`Marking a player 'out' has no effect on the bye list if not present`() {
            val myByes = mutableListOf(2, 3, 4)
            every { files.readByes("byes.txt") } returns myByes
            System.setIn(ByteArrayInputStream("d 5\nq\n".toByteArray()))
            pairing = Pairing(files)
            pairing.inputLoop()
            assertEquals(listOf(2, 3, 4), pairing.byes)
        }

        @Test
        fun `Making a new round assigns pairs and all players are represented`() {
            System.setIn(ByteArrayInputStream("r\nq\n".toByteArray()))
            pairing = Pairing(files)
            pairing.inputLoop()
            assertEquals(1, pairing.rounds.size)

            val round = pairing.rounds.last()
            assertEquals(2, round.pairs?.size)
            val pairedIds = mutableSetOf<Int>()
            round.pairs?.forEach {
                pairedIds.add(it.first)
                pairedIds.add(it.second)
            }
            assertEquals(4, pairedIds.size)
        }

        @Test
        fun `The same two players are not paired twice if possible`() {
            val myPlayers: MutableList<Player> = mutableListOf()
            myPlayers.addAll(players)
            myPlayers.add(Player(6, "Joe", "McGray"))
            myPlayers.add(Player(7, "Marie", "Antionette"))
            every { files.readPlayers("players.txt") } returns myPlayers

            System.setIn(ByteArrayInputStream("r\nr\nr\nr\nr\nr\nq\n".toByteArray()))
            pairing = Pairing(files)
            pairing.inputLoop()
            assertEquals(6, pairing.rounds.size)
            var totalUniquePairs: MutableSet<Pair<Int, Int>>
            var uniquePairs: MutableSet<Pair<Int, Int>>
            pairing.rounds.forEach { r ->
                uniquePairs = createUniquePairs(pairing.rounds[0])
                println("Number of unique pairs, round 1: %d".format(uniquePairs.size))
                assertTrue(uniquePairs.size == 3)
            }
            totalUniquePairs = createUniquePairsAllRounds(pairing)
            println("Number of total unique pairs: %d".format(totalUniquePairs.size))
            assertTrue(totalUniquePairs.size <= 15)
//            uniquePairs.forEach { println(it) }
        }

        private fun createUniquePairsAllRounds(pairing: Pairing): MutableSet<Pair<Int, Int>> {
            val uniquePairs = mutableSetOf<Pair<Int, Int>>()
            pairing.rounds.forEach { r ->
                r.pairs?.forEach { p ->
                    val pAsList = p.toList().toMutableList()
                    pAsList.sort()
                    uniquePairs.add(Pair(pAsList[0], pAsList[1]))
                }
            }
            return uniquePairs
        }

        private fun createUniquePairs(round: Round): MutableSet<Pair<Int, Int>> {
            val uniquePairs = mutableSetOf<Pair<Int, Int>>()
            round.pairs?.forEach { p ->
                val pAsList = p.toList().toMutableList()
                pAsList.sort()
                uniquePairs.add(Pair(pAsList[0], pAsList[1]))
            }
            return uniquePairs
        }
    }
}
