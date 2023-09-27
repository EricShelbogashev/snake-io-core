package api.v1.dto

import java.net.InetSocketAddress

class Discover(
    override var address: InetSocketAddress,
    override var senderId: Int
) : Message(address, senderId) {
    override fun toString(): String {
        return "Discover(address=$address, senderId=$senderId)"
    }
}