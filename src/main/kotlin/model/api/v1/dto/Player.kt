package model.api.v1.dto

import java.net.InetSocketAddress

class Player(
    var ip: String,
    var port: Int,
    var role: NodeRole,
    var type: PlayerType,
    var score: Int,
    var name: String,
    var id: Int
) {
    private val address = InetSocketAddress(ip, port)
    fun address() = address

    override fun toString(): String {
        return "Player(ip='$ip', port=$port, role=$role, type=$type, score=$score, name='$name', id=$id)"
    }
}