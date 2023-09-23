package api.v1.dto

import java.net.InetSocketAddress

class Announcement(
    override val address: InetSocketAddress,
    override val senderId: Int,
    val games: Array<Game>
) : Message(address, senderId)