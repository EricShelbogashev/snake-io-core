import api.v1.controller.GameNetController
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.MulticastSocket

data class Context(
    val inputSocket: MulticastSocket,
    val commonSocket: DatagramSocket,
    val gameGroupAddress: InetSocketAddress,
    val gameClientPermissionLayer: GameClientPermissionLayer,
    val gameNetController: GameNetController,
    val announcementDelay: Long = 1000L
)
