package model.api.v1.dto

import java.net.InetSocketAddress

class Announcement(
    address: InetSocketAddress,
    senderId: Int,
    val games: Array<Game>
) : Message(address, senderId) {
    override fun toString(): String {
        return "Announcement(address=$address, senderId=$senderId, games=${games.contentToString()})"
    }
}