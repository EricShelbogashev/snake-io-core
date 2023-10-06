package model.api.v1.dto

import java.net.InetSocketAddress

class Ack(
    address: InetSocketAddress,
    senderId: Int,
    receiverId: Int,
    msgSeq: Long = DEFAULT_MESSAGE_SEQUENCE_NUMBER,
) : Message(
    address = address,
    senderId = senderId,
    receiverId = receiverId,
    msgSeq = msgSeq
) {
    override fun toString(): String {
        return "Ack(address=$address, senderId=$senderId, receiverId=$receiverId)"
    }
}