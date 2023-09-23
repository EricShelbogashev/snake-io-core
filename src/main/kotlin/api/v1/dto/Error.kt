package api.v1.dto

import java.net.InetSocketAddress

class Error(
    override val address: InetSocketAddress,
    override val senderId: Int,
    val message: String
) : Message(address, senderId)