package api.v1.dto

import java.net.InetSocketAddress

class Ping(
    override var address: InetSocketAddress,
    override var senderId: Int
) : Message(address, senderId) {
    override fun toString(): String {
        return "Ping(address=$address, senderId=$senderId)"
    }
}