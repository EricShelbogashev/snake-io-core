package model.api.v1.dto

import java.net.InetSocketAddress

class Error(
    override var address: InetSocketAddress,
    override var senderId: Int,
    val message: String
) : Message(address, senderId) {
    override fun toString(): String {
        return "Error(address=$address, senderId=$senderId, message='$message')"
    }
}