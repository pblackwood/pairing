package com.artisan.pairing

import java.io.File

class Files {

    fun read(fileName: String): List<Player> {
        val players = mutableListOf<Player>()
        File("files", fileName).forEachLine {
            val chunks = it.split(",")
            if (chunks.size == 4) {
                players.add(Player(chunks[0].toInt(), chunks[1], chunks[2], chunks[3]))
            }
        }
        return players
    }

    fun appendPlayer(fileName: String, player: Player) {
        File("files", fileName).appendText("%s\n".format(player.toString()))
    }

    fun writePlayers(fileName: String, players: List<Player>) {
        File("files", fileName).delete()
        players.forEach {
            appendPlayer(fileName, it)
        }
    }
}