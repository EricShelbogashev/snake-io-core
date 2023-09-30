package model.api.v1.dto

import java.net.InetSocketAddress

open class Message(
    val address: InetSocketAddress,
    val senderId: Int,
    var receiverId: Int = DEFAULT_RECEIVER_ID,
    var msgSeq: Long = DEFAULT_MESSAGE_SEQUENCE_NUMBER
) {

    companion object {
        const val DEFAULT_RECEIVER_ID: Int = -1
        const val DEFAULT_MESSAGE_SEQUENCE_NUMBER: Long = -1
    }

    override fun toString(): String {
        return "Message(address=$address, senderId=$senderId, receiverId=$receiverId, msgSeq=$msgSeq)"
    }
}