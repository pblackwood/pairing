package com.artisan.pairing

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.provider.Arguments
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.stream.Stream
import kotlin.random.Random

@ExtendWith(MockKExtension::class)
class PairingTest {
    lateinit var pairing: Pairing

    @MockK
    lateinit var files: Files

    @MockK
    lateinit var random: Random.Default

    private lateinit var players: List<Player>

    private lateinit var rounds: List<Round>

    private lateinit var byes: List<Int>

    private val standardIn = System.`in`

    private val standardOut = System.out

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
    }

    @AfterEach
    fun tearDown() {
        System.setIn(standardIn)
        System.setOut(standardOut)
        clearAllMocks()
    }

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
        }

        @AfterEach
        fun tearDown() {
            System.setIn(standardIn)
            System.setOut(standardOut)
            clearAllMocks()
        }

        @Test
        fun `on load, reads player list from a file`() {
            pairing = Pairing(files)
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
            pairing = Pairing(files)
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

        @Test
        fun `Can add a player to the list`() {
            val commandWithQuit = "a Kent Beck\nq\n"
            val inputStream = ByteArrayInputStream(commandWithQuit.toByteArray())
            System.setIn(inputStream)
            pairing = Pairing(files)
            pairing.inputLoop()
            val player = Player(3, "Kent", "Beck")
            val expectedPlayerList = listOf(
                Player(32, "Bob", "Robertson", "in"),
                Player(9, "Amy", status = "out"),
                Player(3, "Kent", "Beck")
            )
            verify { files.readPlayers("players.txt") }
            verify(exactly = 1) { files.appendPlayer("players.txt", player) }
            verify(exactly = 0) { files.writePlayers("players.txt", expectedPlayerList) }
            assertEquals(expectedPlayerList, pairing.playerList)
        }

        @Test
        fun `Can mark a player 'out'`() {
            val commandWithQuit = "d 32\nq\n"
            val inputStream = ByteArrayInputStream(commandWithQuit.toByteArray())
            System.setIn(inputStream)
            pairing = Pairing(files)
            pairing.inputLoop()
            val expectedPlayerList = listOf(
                Player(32, "Bob", "Robertson", "out"),
                Player(9, "Amy", status = "out"),
            )
            verify { files.readPlayers("players.txt") }
            verify(exactly = 0) { files.appendPlayer("players.txt", any()) }
            verify(exactly = 1) { files.writePlayers("players.txt", expectedPlayerList) }
            assertEquals(expectedPlayerList, pairing.playerList)
        }
    }

    @Nested
    @DisplayName("Creating and Changing Rounds")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RoundTests {
        @Test
        fun `Can start a new round`() {
            val expectedRound = Round(1, listOf(2, 3, 4, 5), null, listOf(
                Pair(4, 3),
                Pair(2, 5)
            ))
            val expectedRoundList = mutableListOf(expectedRound)
            val commandWithQuit = "r\nq\n"
            System.setIn(ByteArrayInputStream(commandWithQuit.toByteArray()))
            every { random.nextInt(4) } returns 2
            every { random.nextInt(3) } returns 1
            every { random.nextInt(2) } returns 0
            pairing = Pairing(files, random)
            pairing.inputLoop()
            verify { files.readRounds("rounds.txt") }
            assertEquals(expectedRoundList, pairing.rounds)
            verify(exactly = 1) { files.appendRound("rounds.txt", round = expectedRound) }
            verify(exactly = 0) { files.writeRounds("rounds.txt", expectedRoundList) }
            assertEquals(expectedRoundList, pairing.rounds)
        }

        @Test
        fun `With an even number of players, there should not be a bye`() {
            val expectedRound = Round(1, listOf(2, 3, 4, 5), null, listOf(
                Pair(4, 3),
                Pair(2, 5)
            ))
            val commandWithQuit = "r\nq\n"
            System.setIn(ByteArrayInputStream(commandWithQuit.toByteArray()))
            every { random.nextInt(4) } returns 2
            every { random.nextInt(3) } returns 1
            every { random.nextInt(2) } returns 0
            pairing = Pairing(files, random)
            pairing.inputLoop()
            verify(exactly = 0) { files.writeByes("byes.txt", any() ) }
            assertEquals(expectedRound, pairing.rounds.last())
        }

        @Test
        fun `With an odd number of players, there should be a bye`() {
            val myPlayers = players.toMutableList()
            myPlayers.add(Player(6, "first", "last"))
            every { files.readPlayers("players.txt") } returns myPlayers
            val expectedRound = Round(1, listOf(2, 3, 4, 5, 6), 5, listOf(
                Pair(4, 3),
                Pair(2, 6)
            ))
            val commandWithQuit = "r\nq\n"
            System.setIn(ByteArrayInputStream(commandWithQuit.toByteArray()))
            every { random.nextInt(5) } returns 3
            every { random.nextInt(4) } returns 2
            every { random.nextInt(3) } returns 1
            every { random.nextInt(2) } returns 0
            pairing = Pairing(files, random)
            pairing.inputLoop()
            verify { files.writeByes("byes.txt", any() ) }
            assertEquals(expectedRound, pairing.rounds.last())
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
            every { random.nextInt(4) } returns 2
            every { random.nextInt(3) } returns 1
            every { random.nextInt(2) } returns 0
            pairing = Pairing(files, random)
            pairing.inputLoop()
            assertEquals(2, pairing.rounds.size)
            assertEquals(5, pairing.rounds.last().byeId)
            assertEquals(listOf(5), pairing.byes)
            verify { random.nextInt(5) }
            verify { random.nextInt(4) }
            verify { random.nextInt(3) }
            verify { random.nextInt(2) }

            System.setIn(ByteArrayInputStream("r\nq\n".toByteArray()))
            pairing.inputLoop()
            assertEquals(3, pairing.rounds.size)
            assertEquals(4, pairing.rounds.last().byeId)
            assertEquals(listOf(5, 4), pairing.byes)
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
            val totalUniquePairs: MutableSet<Pair<Int, Int>> = createUniquePairsAllRounds(pairing)
            var uniquePairs: MutableSet<Pair<Int, Int>>
            pairing.rounds.forEach { r ->
                uniquePairs = createUniquePairs(pairing.rounds[0])
                println("Number of unique pairs, round 1: %d".format(uniquePairs.size))
                assertTrue(uniquePairs.size == 3)
            }
            println("Number of total unique pairs: %d".format(totalUniquePairs.size))
            assertTrue(totalUniquePairs.size <= 15)
        }

        @Test
        fun `If a player has a bye, they cannot be paired`() {
            val myPlayers: MutableList<Player> = mutableListOf()
            myPlayers.addAll(players)
            myPlayers.add(Player(6, "Joe", "McGray"))
            every { files.readPlayers("players.txt") } returns myPlayers

            every { random.nextInt(5) } returns 3
            every { random.nextInt(4) } returns 2
            every { random.nextInt(3) } returns 1
            every { random.nextInt(2) } returns 0

            System.setIn(ByteArrayInputStream("r\nq\n".toByteArray()))
            pairing = Pairing(files, random)
            pairing.inputLoop()

            assertEquals(5, pairing.rounds.last().byeId)
            val pairIds: Set<Int> = returnUniquePairIds(pairing.rounds.last())
            assertNull(pairIds.find { it == 5 })
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

        private fun returnUniquePairIds(round: Round): Set<Int> {
            val uniquePairIds = mutableSetOf<Int>()
            round.pairs?.forEach { p ->
                uniquePairIds.add(p.first)
                uniquePairIds.add(p.second)
            }
            return uniquePairIds
        }
    }

    @Nested
    @DisplayName("Chip Counts and Buy-backs")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class MoneyTests {
        @Test
        fun `A player can buy back in`() {
            val commandWithQuit = "b 9\nq\n"
            val inputStream = ByteArrayInputStream(commandWithQuit.toByteArray())
            System.setIn(inputStream)
            pairing = Pairing(files)
            pairing.inputLoop()
            val expectedPlayerList = listOf(
                Player(2, "Bob", "Robertson"),
                Player(3, "Bill", "Carpenter"),
                Player(4, "Mary", "Smith"),
                Player(5, "Betty", "Smythe"),
                Player(9, "Amy", status = "in")
            )
            assertEquals(expectedPlayerList, pairing.playerList)
        }
    }
}
