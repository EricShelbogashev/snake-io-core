import config.ClientSettings
import model.api.ConnectionManager
import model.api.v1.dto.Join
import model.api.v1.dto.NodeRole
import model.api.v1.dto.PlayerType
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.NetworkInterface

fun main() {
    val manager = ConnectionManager(
        MulticastSocket(ClientSettings.gameGroupAddress()),
        DatagramSocket(),
        NetworkInterface.getByName("wlan0"),
        ClientSettings.gameGroupAddress()
    )

    manager.setOnJoinAccepted {
        println(it)
    }

    manager.send(Join(
        InetSocketAddress("192.168.2.2", 221),
        3,
        "afsefsef",
        "Daefesfes",
        PlayerType.HUMAN,
        NodeRole.NORMAL
    ))
}