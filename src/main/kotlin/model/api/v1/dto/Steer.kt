package model.api.v1.dto

import java.net.InetSocketAddress

class Steer(
    address: InetSocketAddress,
    senderId: Int,
    val direction: Direction,
) : Message(address, senderId) {
    override fun toString(): String {
        return "Steer(address=$address, senderId=$senderId, direction=$direction)"
    }
}