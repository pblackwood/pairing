package com.artisan.pairing

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.random.Random
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

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
        every { files.appendPlayer("players.txt", any()) } returns Unit
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
        @BeforeEach
        fun setUp() {
            players = listOf(
                Player(32, "Bob", "Robertson"),
                Player(9, "Amy", status = "out", chipCount = 0)
            )
            every { files.readPlayers("players.txt") } returns players.toMutableList()
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
        fun `can list all players from a file and report the total chip count`() {
            val outputStream = ByteArrayOutputStream()
            System.setOut(PrintStream(outputStream))
            val command = "l\nq\n"
            val inputStream = ByteArrayInputStream(command.toByteArray())
            System.setIn(inputStream)
            pairing = Pairing(files)
            pairing.inputLoop()
            assertEquals(
                """
            > STILL IN
            32 Bob Robertson (10)
            OUT
            9 Amy  (0)
            TOTAL CHIPS: 10
            CURRENT ROUND: NO ROUND
            > 
                """.trimIndent(),
                outputStream.toString()
            )
        }

        @Test
        fun `Can add a player to the list`() {
            val command = "a Kent Beck\nq\n"
            val inputStream = ByteArrayInputStream(command.toByteArray())
            System.setIn(inputStream)
            pairing = Pairing(files)
            pairing.inputLoop()
            val player = Player(3, "Kent", "Beck")
            val expectedPlayerList = listOf(
                Player(32, "Bob", "Robertson", "in"),
                Player(9, "Amy", status = "out", chipCount = 0),
                Player(3, "Kent", "Beck")
            )
            verify { files.readPlayers("players.txt") }
            verify(exactly = 1) { files.appendPlayer("players.txt", player) }
            verify(exactly = 0) { files.writePlayers("players.txt", expectedPlayerList) }
            assertEquals(expectedPlayerList, pairing.playerList)
        }

        @Test
        fun `Can mark a player 'out'`() {
            val command = "d 32\nq\n"
            val inputStream = ByteArrayInputStream(command.toByteArray())
            System.setIn(inputStream)
            pairing = Pairing(files)
            pairing.inputLoop()
            val expectedPlayerList = listOf(
                Player(32, "Bob", "Robertson", "out", chipCount = 10),
                Player(9, "Amy", status = "out", chipCount = 0),
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
            val command = "r\nq\n"
            System.setIn(ByteArrayInputStream(command.toByteArray()))
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
        fun `Can list any round`() {
            val myRounds = listOf(
                Round(1, listOf(2, 3, 4, 5), null, listOf(Pair(2, 3), Pair(4, 5))),
                Round(2, listOf(2, 3, 4, 5), null, listOf(Pair(4, 5), Pair(2, 2)))
            )
            every { files.readRounds("rounds.txt") } returns myRounds
            val command = "r 1\nq\n"
            System.setIn(ByteArrayInputStream(command.toByteArray()))
            val outputStream = ByteArrayOutputStream()
            System.setOut(PrintStream(outputStream))
            every { random.nextInt(4) } returns 2
            every { random.nextInt(3) } returns 1
            every { random.nextInt(2) } returns 0
            pairing = Pairing(files, random)
            pairing.inputLoop()
            assertEquals(
                """
                > Round 1
                NO BYE
                PAIRS:
                2 Bob Robertson (10) vs 3 Bill Carpenter (10)
                4 Mary Smith (10) vs 5 Betty Smythe (10)
                > 
                """.trimIndent(),
                outputStream.toString()
            )
        }

        @Test
        fun `With an even number of players, there should not be a bye`() {
            val expectedRound = Round(1, listOf(2, 3, 4, 5), null, listOf(
                Pair(4, 3),
                Pair(2, 5)
            ))
            val command = "r\nq\n"
            System.setIn(ByteArrayInputStream(command.toByteArray()))
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
            val command = "r\nq\n"
            System.setIn(ByteArrayInputStream(command.toByteArray()))
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
                println("Number of unique pairs, round %d: %d".format(r.id, uniquePairs.size))
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
    @DisplayName("Command line")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CommandLineTests {

        @AfterEach
        fun tearDown() {
            Settings.resetDefaults()
        }

        @Test
        fun `command line arguments can be blank`() {
            Pairing.parseCommandLine(emptyArray())
            assertEquals(10, Settings.chipValue)
            assertEquals(10, Settings.buyInChipCount)
            assertEquals(0, Settings.fees)
        }

        @Test
        fun `command line arguments can be reset to the defaults`() {
            Pairing.parseCommandLine(arrayOf("--chipCount=35", "--chipValue=50", "--fees=100"))
            assertEquals(50, Settings.chipValue)
            assertEquals(35, Settings.buyInChipCount)
            assertEquals(100, Settings.fees)
            Settings.resetDefaults()
            assertEquals(10, Settings.chipValue)
            assertEquals(10, Settings.buyInChipCount)
            assertEquals(0, Settings.fees)
        }

        @Test
        fun `unrecognized command line arguments prints usage`() {
            val outputStream = ByteArrayOutputStream()
            System.setOut(PrintStream(outputStream))
            Pairing.parseCommandLine(arrayOf("bob", "mary"))
            assertEquals("Usage: pairing [--chipValue=chipValue] [--chipCount=startingChipCount]\n",
                outputStream.toString()
            )
        }

        @Test
        fun `can change the chip value with --chipValue`() {
            Pairing.parseCommandLine(arrayOf("--chipValue=35"))
            assertEquals(35, Settings.chipValue)
            assertEquals(10, Settings.buyInChipCount)
            assertEquals(0, Settings.fees)
        }

        @Test
        fun `can change the chip value with -v`() {
            Pairing.parseCommandLine(arrayOf("-v=35"))
            assertEquals(35, Settings.chipValue)
            assertEquals(10, Settings.buyInChipCount)
            assertEquals(0, Settings.fees)
        }

        @Test
        fun `can change the starting chip count with --chipCount`() {
            Pairing.parseCommandLine(arrayOf("--chipCount=35"))
            assertEquals(10, Settings.chipValue)
            assertEquals(35, Settings.buyInChipCount)
            assertEquals(0, Settings.fees)
        }

        @Test
        fun `can change the starting chip count with -c`() {
            Pairing.parseCommandLine(arrayOf("-c=35"))
            assertEquals(10, Settings.chipValue)
            assertEquals(35, Settings.buyInChipCount)
            assertEquals(0, Settings.fees)
        }

        @Test
        fun `can change the room fees with --fees`() {
            Pairing.parseCommandLine(arrayOf("--fees=85"))
            assertEquals(10, Settings.chipValue)
            assertEquals(10, Settings.buyInChipCount)
            assertEquals(85, Settings.fees)
        }

        @Test
        fun `can change the room fees with -f`() {
            Pairing.parseCommandLine(arrayOf("-f=85"))
            assertEquals(10, Settings.chipValue)
            assertEquals(10, Settings.buyInChipCount)
            assertEquals(85, Settings.fees)
        }

        @Test
        fun `can change all parameters`() {
            Pairing.parseCommandLine(arrayOf("--chipCount=35", "--chipValue=50", "--fees=100"))
            assertEquals(50, Settings.chipValue)
            assertEquals(35, Settings.buyInChipCount)
            assertEquals(100, Settings.fees)
        }

        @Test
        fun `can change all parameters with shorthand notation`() {
            Pairing.parseCommandLine(arrayOf("-c=35", "-v=50", "-f=100"))
            assertEquals(50, Settings.chipValue)
            assertEquals(35, Settings.buyInChipCount)
            assertEquals(100, Settings.fees)
        }
    }

    @Nested
    @DisplayName("Chip Counts and Buy-backs")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class MoneyTests {
        @AfterEach
        fun tearDown() {
            Settings.resetDefaults()
        }

        @Test
        fun `on load, players start with the default number of chips`() {
            val expectedPlayers = listOf(
                Player(2, "Bob", "Robertson", chipCount=10),
                Player(3, "Bill", "Carpenter", chipCount=10),
                Player(4, "Mary", "Smith", chipCount=10),
                Player(5, "Betty", "Smythe", chipCount=10),
                Player(9, "Amy", status = "out", chipCount=10)
            )
            pairing = Pairing(files)
            verify {
                files.readPlayers("players.txt")
                assertEquals(expectedPlayers, pairing.playerList)
            }
        }

        @Test
        fun `A player can buy back in`() {
            val command = "b 9\nq\n"
            val inputStream = ByteArrayInputStream(command.toByteArray())
            System.setIn(inputStream)
            pairing = Pairing(files)
            pairing.inputLoop()
            assertEquals(Player(9, "Amy", status = "in"), pairing.playerList[4])
        }

        @Test
        fun `A player can buy back in with chip count`() {
            val command = "b 9 5\nq\n"
            val inputStream = ByteArrayInputStream(command.toByteArray())
            System.setIn(inputStream)
            pairing = Pairing(files)
            pairing.inputLoop()
            assertEquals(Player(9, "Amy", status = "in", chipCount = 5), pairing.playerList[4])
        }

        @Test
        fun `A player can report their chip count`() {
            val command = "c 4 15\nq\n"
            val inputStream = ByteArrayInputStream(command.toByteArray())
            System.setIn(inputStream)
            pairing = Pairing(files)
            pairing.inputLoop()
            assertEquals(Player(4, "Mary", "Smith", chipCount = 15), pairing.playerList[2])
        }

        @Test
        fun `A player's chip count must not be negative`() {
            val command = "c 4 -2\nq\n"
            val inputStream = ByteArrayInputStream(command.toByteArray())
            System.setIn(inputStream)
            val outputStream = ByteArrayOutputStream()
            System.setOut(PrintStream(outputStream))
            pairing = Pairing(files)
            pairing.inputLoop()
            assertEquals("> Chip count must be >= 0\n> ", outputStream.toString())
            assertEquals(Player(4, "Mary", "Smith"), pairing.playerList[2])
        }

        @Test
        fun `A player who reports 0 chip count is out of the tournament`() {
            val command = "c 4 0\nq\n"
            val inputStream = ByteArrayInputStream(command.toByteArray())
            System.setIn(inputStream)
            pairing = Pairing(files)
            pairing.inputLoop()
            assertEquals(Player(4, "Mary", "Smith", chipCount=0, status = "out"), pairing.playerList[2])
        }

        @Test
        fun `List players reports the total chip count`() {
            val command = "l\nq\n"
            val inputStream = ByteArrayInputStream(command.toByteArray())
            System.setIn(inputStream)
            val outputStream = ByteArrayOutputStream()
            System.setOut(PrintStream(outputStream))
            players.forEach { p -> p.chipCount = 20 }
            pairing = Pairing(files)
            pairing.inputLoop()
            assertEquals(
                """
            > STILL IN
            2 Bob Robertson (20)
            3 Bill Carpenter (20)
            4 Mary Smith (20)
            5 Betty Smythe (20)
            OUT
            9 Amy  (20)
            TOTAL CHIPS: 100
            CURRENT ROUND: NO ROUND
            > 
                """.trimIndent(),
                outputStream.toString()
            )
        }

        @Test
        fun `Finish the tournament and report winnings`() {
            val command = "f\nq\n"
            val inputStream = ByteArrayInputStream(command.toByteArray())
            System.setIn(inputStream)
            val outputStream = ByteArrayOutputStream()
            System.setOut(PrintStream(outputStream))
            players = listOf(
                Player(2, "Bob", "Robertson", chipCount=55),
                Player(3, "Bill", "Carpenter", chipCount=79),
                Player(4, "Mary", "Smith", chipCount=16),
                Player(5, "Betty", "Smythe", "out", chipCount=0),
                Player(9, "Amy", status = "out", chipCount=0)
            )
            every { files.readPlayers("players.txt") } returns players
            pairing = Pairing(files)
            pairing.inputLoop()
            assertEquals(
                """
            > FINAL TOTALS
            Bob Robertson (55) = $550
            Bill Carpenter (79) = $790
            Mary Smith (16) = $160
            TOTAL CHIPS: 150
            TOTAL POT: $1500
            NET POT: $1500
            > 
                """.trimIndent(),
                outputStream.toString()
            )
        }

        @Test
        fun `Finish the tournament and report winnings, including fees`() {
            val command = "f\nq\n"
            val inputStream = ByteArrayInputStream(command.toByteArray())
            System.setIn(inputStream)
            val outputStream = ByteArrayOutputStream()
            System.setOut(PrintStream(outputStream))
            players = listOf(
                Player(2, "Bob", "Robertson", chipCount=55),
                Player(3, "Bill", "Carpenter", chipCount=79),
                Player(4, "Mary", "Smith", chipCount=16),
                Player(5, "Betty", "Smythe", "out", chipCount=0),
                Player(9, "Amy", status = "out", chipCount=0)
            )
            every { files.readPlayers("players.txt") } returns players
            Settings.fees = 100
            pairing = Pairing(files)
            pairing.inputLoop()
            assertEquals(
                """
            > FINAL TOTALS
            Bob Robertson (55) = $513
            Bill Carpenter (79) = $737
            Mary Smith (16) = $149
            TOTAL CHIPS: 150
            TOTAL POT: $1500
            NET POT: $1400
            > 
                """.trimIndent(),
                outputStream.toString()
            )
        }
    }
}
