package api.v1.dto

import java.net.InetSocketAddress

class Discover(
    override val address: InetSocketAddress,
    override val senderId: Int
) : Message(address, senderId)