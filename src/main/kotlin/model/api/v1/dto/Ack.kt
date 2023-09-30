package model.api.v1.dto

import java.net.InetSocketAddress

class Ack(
    address: InetSocketAddress,
    senderId: Int,
    receiverId: Int,
) : Message(
    address = address,
    senderId = senderId,
    receiverId = receiverId
) {
    override fun toString(): String {
        return "Ack(address=$address, senderId=$senderId, receiverId=$receiverId)"
    }
}