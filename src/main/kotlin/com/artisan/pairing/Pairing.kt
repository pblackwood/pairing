package com.artisan.pairing

import kotlin.random.Random

open class Pairing(
    private val files: Files = Files(),
    private val random: Random = Random.Default,
) {
    lateinit var playerList: MutableList<Player>
    lateinit var rounds: MutableList<Round>
    lateinit var byes: MutableList<Int>

    init {
        initLists()
    }

    private fun initLists() {
        playerList = files.readPlayers().toMutableList()
        rounds = files.readRounds().toMutableList()
        byes = files.readByes().toMutableList()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val pairingApp = Pairing()
            if (args.isNotEmpty()) {
                parseCommandLine(args)
            }
            pairingApp.inputLoop()
        }

        fun parseCommandLine(args: Array<String>) {
            Settings.resetDefaults()
            for (arg in args) {
                val argPair = arg.split("=")
                when (argPair[0]) {
                    in "--chipCount", "-c" -> Settings.buyInChipCount = argPair[1].toInt()
                    in "--chipValue", "-v" -> Settings.chipValue = argPair[1].toInt()
                    in "--fees", "-f" -> Settings.fees = argPair[1].toInt()
                    else -> {
                        println("Usage: pairing [--chipValue=chipValue] [--chipCount=startingChipCount]")
                        Settings.runOnLoad = false
                        break
                    }
                }
            }
        }
    }

    fun inputLoop() {
        var running = Settings.runOnLoad
        while (running) {
            print("> ")
            val command = readln()
            if (command.length == 0) {
                println()
            }
            else {
                when (command.get(0)) {
                    'l' -> listPlayers()
                    'a' -> addPlayer(command)
                    'd' -> removePlayer(command)
                    'b' -> reinstatePlayer(command)
                    'c' -> chipCount(command)
                    'r' -> startRound(command)
                    'f' -> finish()
                    'p' -> listLastRound()
                    'q' -> running = false
                }
            }
        }
    }

    private fun totalChipCount(): Int {
        return playerList.sumOf { p -> p.chipCount }
    }

    private fun totalPot(): Int {
        return totalChipCount() * Settings.chipValue
    }

    private fun netPot(): Int {
        return totalPot() - Settings.fees
    }

    private fun listPlayers() {
        println("STILL IN")
        playerList.filter { it.status == "in" }.forEach { p ->
            println(p.details(withId = true))
        }
        println("OUT")
        playerList.filter { it.status == "out" }.forEach { p ->
            println(p.details(withId = true))
        }
        println("TOTAL CHIPS: %d".format(totalChipCount()))
        println("CURRENT ROUND: %s".format(if (rounds.size > 0) rounds.size.toString() else "NO ROUND"))
    }

    private fun addPlayer(command: String) {
        val chunks = command.split(" +".toRegex())
        val first = chunks[1]
        val last = if (chunks.size > 2) chunks[2] else ""
        val player = Player(playerList.size + 1, first, last)
        playerList.add(player)
        files.appendPlayer(player = player)
        println(player.details())
    }

    private fun removePlayer(command: String) {
        val id: Int = command.split(" +".toRegex())[1].toInt()
        val player = playerList.find { it.id == id }
        if (player != null) {
            player.status = "out"
            files.writePlayers(players = playerList)
            val byeIndex = byes.indexOf(player.id)
            if (byeIndex >= 0) {
                byes.removeAt(byeIndex)
            }
        }
        listPlayers()
    }

    private fun reinstatePlayer(command: String) {
        val cmd: List<String> = command.split(" +".toRegex())
        val playerId: Int = cmd[1].toInt()
        val player = playerList.find { it.id == playerId }
        if (player != null) {
            player.status = "in"
            if (cmd.size > 2) {
                player.chipCount = cmd[2].toInt()
            }
            files.writePlayers(players = playerList)
        }
        listPlayers()
    }

    private fun chipCount(command: String) {
        val cmd: List<String> = command.split(" +".toRegex())
        val playerId: Int = cmd[1].toInt()
        val player = playerList.find { it.id == playerId }
        if (player != null) {
            val count = cmd[2].toInt()
            if (count < 0) {
                println("Chip count must be >= 0")
            }
            else {
                player.chipCount = count
                if (player.chipCount == 0) {
                    player.status = "out"
                }
                files.writePlayers(players = playerList)
                println(player.details())
            }
        }
    }

    private fun listRound(round: Round) {
        round.printRoundDetails(playerList)
    }

    private fun listLastRound() {
        if (rounds.size > 0) {
            listRound(rounds[rounds.size - 1])
        }
    }

    private fun startRound(command: String) {
        val cmd: List<String> = command.split(" +".toRegex())
        if (cmd.size > 1) {
            listRound(rounds[cmd[1].toInt() - 1])
        }
        else {
            val round = Round(
                id = rounds.size + 1,
                playerIds = playerList.filter { it.status == "in" }.map { it.id }
            )
            round.byeId = if (isOdd(round.playerIds.size)) assignBye(round) else null
            round.pairs = assignPairs(round)
            rounds.add(round)
            files.appendRound(round = round)
            listRound(round)
        }
    }

    private fun finish() {
        println("FINAL TOTALS")
        playerList.filter { it.status == "in" }.forEach { p ->
            println(p.details(withMoney=true, moneyPercent=(netPot().toDouble() / totalPot())))
        }
        println("TOTAL CHIPS: %d".format(totalChipCount()))
        println("TOTAL POT: $%d".format(totalPot()))
        println("NET POT: $%d".format(netPot()))
    }

    private fun assignBye(round: Round): Int {
        val playerIdsAvailableForBye = round.playerIds.filter { !byes.contains(it) }
        val byeId = if (playerIdsAvailableForBye.isEmpty()) {
            byes.removeAt(0)
        }
        else {
            playerIdsAvailableForBye[random.nextInt(playerIdsAvailableForBye.size)]
        }
        byes.add(byeId)
        files.writeByes(byes = byes)
        return byeId
    }

    private fun assignPairs(round: Round): List<Pair<Int, Int>> {
        val playerIds = round.playerIds.filter { it != round.byeId }.toMutableList()
        val pairs = mutableListOf<Pair<Int, Int>>()
        while (playerIds.size > 1) {
            val firstPlayerId = playerIds.removeAt(random.nextInt(playerIds.size))
            val firstPlayer: Player = playerList.find { it.id == firstPlayerId }!!
            val secondPlayerId = findPairFor(firstPlayer, playerIds)
            val secondPlayer: Player = playerList.find { it.id == secondPlayerId }!!
            playerIds.remove(secondPlayerId)
            firstPlayer.pairs.add(secondPlayerId)
            secondPlayer.pairs.add(firstPlayerId)
            pairs.add(Pair(firstPlayerId, secondPlayerId))
        }
        return pairs
    }

    private fun findPairFor(player: Player, availablePlayerIds: List<Int>): Int {
        val availablePairIds = availablePlayerIds.filter { pId -> !player.pairs.contains(pId) }
        return when (availablePairIds.size) {
            0 -> availablePlayerIds.first()
            1 -> availablePairIds.first()
            else -> availablePairIds[random.nextInt(availablePairIds.size)]
        }
    }

    private fun isOdd(n: Int): Boolean {
        return n % 2 == 1
    }
}