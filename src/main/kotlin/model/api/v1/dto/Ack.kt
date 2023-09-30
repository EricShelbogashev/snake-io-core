package model.api.v1.dto

import java.net.InetSocketAddress

open class Ack(
    override var address: InetSocketAddress,
    override var senderId: Int,
    override var receiverId: Int,
) : Message(address, senderId) {
    override fun toString(): String {
        return "Ack(address=$address, senderId=$senderId, receiverId=$receiverId)"
    }
}