package com.artisan.pairing

import kotlin.random.Random

class Pairing(
    private val files: Files = Files(),
    private val random: Random = Random.Default) {
    lateinit var playerList: MutableList<Player>
    lateinit var rounds: MutableList<Round>
    var byeIds: MutableList<Int> = mutableListOf()

    init {
        initLists()
    }

    private fun initLists() {
        playerList = files.readPlayers().toMutableList()
        rounds = files.readRounds().toMutableList()
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
        player?.status = "out"
        files.writePlayers(players = playerList)
        listPlayers()
    }

    private fun listRounds() {
        rounds.forEach {
            val byePlayer: Player? = if (it.byeId == null) null else playerList.find { p -> p.id == it.byeId }
            println("Round %d".format(it.id))
            println("%s".format(if (byePlayer == null) "NO BYE" else "BYE: %s".format(byePlayer.fullName())))
        }
    }

    private fun startRound() {
        val round = Round(
            id = rounds.size + 1,
            playerIds = playerList.filter { it.status == "in" }.map { it.id },
        )
        round.byeId = if (isOdd(round.playerIds.size)) assignBye(round) else null
        rounds.add(round)
        files.appendRound(round = round)
        listRounds()
    }

    private fun assignBye(round: Round): Int {
        val byeIndex = random.nextInt(round.playerIds.size)
        return round.playerIds[byeIndex]
    }

    private fun isOdd(n: Int): Boolean {
        return n % 2 == 1
    }
}