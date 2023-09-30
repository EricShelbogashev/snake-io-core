package model.api.v1.dto

import java.net.InetSocketAddress

open class Message(
    open var address: InetSocketAddress,
    open var senderId: Int,
    open var receiverId: Int = -1,
    open var msgSeq: Long = -1
) {
    override fun toString(): String {
        return "Message(address=$address, senderId=$senderId, msgSeq=$msgSeq)"
    }
}