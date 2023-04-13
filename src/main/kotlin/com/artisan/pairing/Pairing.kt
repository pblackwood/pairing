package com.artisan.pairing

import kotlin.system.exitProcess

class Pairing(val files: Files = Files()) {
    var playerList: MutableList<Player> = mutableListOf()

    init {
        initLists()
    }

    private fun initLists() {
        playerList = files.read("players.txt").toMutableList()
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
                'q' -> waiting = false
            }
        } while (waiting)
    }

    fun listPlayers() {
        println("STILL IN")
        playerList.filter { it.status == "in" }.forEach {
            println("%d %s".format(it.id, it.fullName()))
        }
        println("OUT")
        playerList.filter { it.status == "out" }.forEach {
            println("%d %s".format(it.id, it.fullName()))
        }
    }

    fun addPlayer(command: String) {
        val chunks = command.split(" +".toRegex())
        val first = chunks[1]
        val last = if (chunks.size > 2) chunks[2] else ""
        val player = Player(playerList.size + 1, first, last)
        playerList.add(player)
        files.appendPlayer("players.txt", player)
        listPlayers()
    }

    fun removePlayer(command: String) {
        val id: Int = command.split(" +".toRegex())[1].toInt()
        val player = playerList.find { it.id == id }
        player?.status = "out"
        files.writePlayers("players.txt", playerList)
        listPlayers()
    }
}