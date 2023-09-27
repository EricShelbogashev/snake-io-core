package api.v1.dto

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
    fun address() = InetSocketAddress(ip, port)
    override fun toString(): String {
        return "Player(ip='$ip', port=$port, role=$role, type=$type, score=$score, name='$name', id=$id)"
    }
}