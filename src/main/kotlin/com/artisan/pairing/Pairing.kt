package com.artisan.pairing

import kotlin.random.Random

class Pairing(
    private val files: Files = Files(),
    private val random: Random = Random.Default) {
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
            Pairing().inputLoop()
        }
    }

    fun inputLoop() {
        var waiting = true
        do {
            print("> ")
            val command = readln()
            when (command.get(0)) {
                'l' -> listPlayers()
                'a' -> addPlayer(command)
                'd' -> removePlayer(command)
                'r' -> startRound()
                'q' -> waiting = false
            }
        } while (waiting)
    }

    private fun listPlayers() {
        println("STILL IN")
        playerList.filter { it.status == "in" }.forEach {
            println("%d %s".format(it.id, it.fullName()))
        }
        println("OUT")
        playerList.filter { it.status == "out" }.forEach {
            println("%d %s".format(it.id, it.fullName()))
        }
    }

    private fun addPlayer(command: String) {
        val chunks = command.split(" +".toRegex())
        val first = chunks[1]
        val last = if (chunks.size > 2) chunks[2] else ""
        val player = Player(playerList.size + 1, first, last)
        playerList.add(player)
        files.appendPlayer(player = player)
        listPlayers()
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

    private fun listRound(round: Round) {
        round.printRoundDetails(playerList)
    }

    private fun listRounds() {
        rounds.forEach { r ->
            r.printRoundDetails(playerList)
        }
    }

    private fun startRound() {
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
        val playerIds = round.playerIds.toMutableList()
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