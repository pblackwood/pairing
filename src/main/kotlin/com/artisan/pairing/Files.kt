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
            if (chunks.size == 3) {
                val playerIds = if (chunks[1].isNotEmpty()) chunks[1].split(",").map { p -> p.toInt() }.toList() else listOf()
                val byeId = if (chunks[2].isNotEmpty()) chunks[2].toInt() else null
                rounds.add(Round(chunks[0].toInt(), playerIds, byeId))
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

}