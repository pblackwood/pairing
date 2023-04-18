package com.artisan.pairing

import java.io.File

class Files {

    fun readPlayers(fileName: String = "players.txt"): List<Player> {
        val players = mutableListOf<Player>()
        File("files", fileName).forEachLine {
            val chunks = it.split(",")
            if (chunks.size == 4) {
                players.add(Player(chunks[0].toInt(), chunks[1], chunks[2], chunks[3]))
            }
        }
        return players
    }

    fun appendPlayer(fileName: String = "players.txt", player: Player) {
        File("files", fileName).appendText("%s\n".format(player.toString()))
    }

    fun writePlayers(fileName: String = "players.txt", players: List<Player>) {
        File("files", fileName).delete()
        players.forEach {
            appendPlayer(fileName, it)
        }
    }

    fun readRounds(fileName: String = "rounds.txt"): List<Round> {
        val rounds = mutableListOf<Round>()
        File("files", fileName).forEachLine {
            val chunks = it.split(";")
            if (chunks.size == 4) {
                val playerIds = if (chunks[1].isNotEmpty()) chunks[1].split(",").map { p -> p.toInt() }.toList() else listOf()
                val byeId = if (chunks[2].isNotEmpty()) chunks[2].toInt() else null
                val pairs = if (chunks[3].isNotEmpty()) chunks[3].split("|").map { p -> Pair(p.split(",")[0].toInt(), p.split(",")[1].toInt()) }.toList() else listOf()
                rounds.add(Round(chunks[0].toInt(), playerIds, byeId, pairs))
            }
        }
        return rounds
    }

    fun appendRound(fileName: String = "rounds.txt", round: Round) {
        File("files", fileName).appendText("%s\n".format(round.toString()))
    }

    fun writeRounds(fileName: String = "rounds.txt", rounds: List<Round>) {
        File("files", fileName).delete()
        rounds.forEach {
            appendRound(fileName, it)
        }
    }

    fun readByes(fileName: String = "byes.txt"): List<Int> {
        val rawList = File("files", fileName).readLines()
        return if (rawList.isEmpty()) emptyList() else rawList.first().split(",").map { it.toInt() }.toList()
    }

    fun appendBye(fileName: String = "byes.txt", byeId: Int) {
        val byes = readByes(fileName).toMutableList()
        byes.add(byeId)
        writeByes(fileName, byes)
    }

    fun writeByes(fileName: String = "byes.txt", byes: List<Int>) {
        File("files", fileName).delete()
        File("files", fileName).writeBytes(byes.joinToString(separator=",").toByteArray())
    }

}