package config

import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.NetworkInterface

data class ClientSettings(
    val multicastReceiveSocket: MulticastSocket,
    val generalSocket: DatagramSocket,
    val gameGroupAddress: InetSocketAddress = gameGroupAddress(),
    val networkInterface: NetworkInterface
) {
    companion object Defaults {
        // Общий для всех пользователей.
        fun gameGroupAddress(): InetSocketAddress = InetSocketAddress("239.192.0.4", 9192)
    }
}
