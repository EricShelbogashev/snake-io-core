package model.api.v1.dto

import java.net.InetSocketAddress

class Discover(
    address: InetSocketAddress,
    senderId: Int
) : Message(address, senderId) {
    override fun toString(): String {
        return "Discover(address=$address, senderId=$senderId)"
    }
}