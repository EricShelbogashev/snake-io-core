package model.api.v1.dto

import java.net.InetSocketAddress

class Ping(
    address: InetSocketAddress,
    senderId: Int
) : Message(address, senderId) {
    override fun toString(): String {
        return "Ping(address=$address, senderId=$senderId)"
    }
}