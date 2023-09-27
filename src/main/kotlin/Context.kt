import java.net.DatagramSocket
import java.net.MulticastSocket

data class Context(
    // Group input
    val inputSocket: MulticastSocket,
    val commonSocket: DatagramSocket
)
