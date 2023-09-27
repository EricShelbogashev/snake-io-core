import api.v1.controller.GameRestController
import api.v1.dto.*
import java.net.DatagramSocket
import java.net.InetSocketAddress

fun main() {
    val controller = GameRestController(DatagramSocket())
    controller.state(
        GameState(
            InetSocketAddress(33),
            0,
            0,
            arrayOf(
                Player(
                    "",
                    0,
                    NodeRole.MASTER,
                    PlayerType.HUMAN,
                    3,
                    "Petya",
                    3
                )
            ),
            arrayOf(),
            arrayOf(Snake(
                Direction.DOWN,
                3,
                Snake.State.ALIVE,
                arrayOf(
                    Coords(3,3),
                    Coords(3,2),
                )
            ))
        )
    )
}