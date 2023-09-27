import controller.GameController
import controller.LobbyController
import state.LobbyState
import state.MatchState
import state.State
import java.io.Closeable
import java.net.InetSocketAddress

class GameClient(
    address: InetSocketAddress = InetSocketAddress("239.192.0.4", 9192),
    private val onClientStateChanged: (state: State) -> Unit
) : Closeable {
    // ensures correct disposal of state machine nodes
    private var permissionLayer = GameClientPermissionLayer(address) { state -> onClientStateChanged(state) }

    fun lobbyController(): LobbyController {
        if (permissionLayer.state !is LobbyState) {
            throw IllegalStateException("could not able to get lobby controller not from lobby")
        }
        return permissionLayer.state as LobbyController
    }

    fun gameController(): GameController {
        if (permissionLayer.state !is MatchState) {
            throw IllegalStateException("could not able to get game controller not from game")
        }
        return permissionLayer.state as GameController
    }

    override fun close() {
        permissionLayer.close()
    }
}