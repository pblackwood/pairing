package com.artisan.pairing

class Pairing(val files: Files = Files()) {
    var playerList: MutableList<Player> = mutableListOf()
    var rounds: MutableList<Round> = mutableListOf()
    var byes: MutableList<Player> = mutableListOf()

    init {
        initLists()
    }

    private fun initLists() {
        playerList = files.readPlayers().toMutableList()
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
                'r' -> startRound(command)
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
        files.appendPlayer("players.txt", player)
        listPlayers()
    }

    private fun removePlayer(command: String) {
        val id: Int = command.split(" +".toRegex())[1].toInt()
        val player = playerList.find { it.id == id }
        player?.status = "out"
        files.writePlayers("players.txt", playerList)
        listPlayers()
    }

    private fun startRound(command: String) {
        val round = Round(
            id = rounds.size + 1,
            playerIds = playerList.filter { it.status == "in" }.map { it.id }
        )
    }
}