package api.v1.dto

import java.net.InetSocketAddress

open class Ack(
    override val address: InetSocketAddress,
    override val senderId: Int,
    open val receiverId: Int
) : Message(address, senderId)