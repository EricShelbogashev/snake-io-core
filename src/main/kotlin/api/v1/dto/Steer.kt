package api.v1.dto

import java.net.InetSocketAddress

class Steer(
    override val address: InetSocketAddress,
    override val senderId: Int,
    val direction: Direction,
) : Message(address, senderId)