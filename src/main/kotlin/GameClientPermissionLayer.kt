import model.controller.GameController
import model.controller.LobbyController
import model.controller.OnGameAnnouncementListener
import model.controller.OnGameStateChangeListener
import model.state.game.GameConfig
import state.GameState
import state.HaltState
import state.LobbyState
import state.State
import java.io.Closeable
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.MulticastSocket

internal class GameClientPermissionLayer(address: InetSocketAddress) :
    LobbyController,
    GameController,
    Closeable {
    var context = Context(
        MulticastSocket(address),
        DatagramSocket()
    )
    var state: State = LobbyState(context, this)

    fun changeState(state: State) {
        this.state = state
    }

    override fun newGame(config: GameConfig) {
        if (state !is LobbyState) {
            throw IllegalStateException("not able to start new game not from lobby")
        }
        (state as LobbyState).newGame(config)
    }

    override fun joinGame(gameName: String) {
        if (state !is LobbyState) {
            throw IllegalStateException("not able to join the game not from lobby")
        }
        (state as LobbyState).joinGame(gameName)
    }

    override fun watchGame(gameName: String) {
        if (state !is LobbyState) {
            throw IllegalStateException("not able to watch the game not from lobby")
        }
        (state as LobbyState).watchGame(gameName)
    }

    override fun setGameAnnouncementListener(listener: OnGameAnnouncementListener) {
        if (state !is LobbyState) {
            throw IllegalStateException("not able to watch the announcements not from lobby")
        }
        (state as LobbyState).setGameAnnouncementListener(listener)
    }

    override fun removeGameAnnouncementListener() {
        if (state !is LobbyState) {
            throw IllegalStateException("not able to stop watch the announcements not from lobby")
        }
        (state as LobbyState).removeGameAnnouncementListener()
    }

    override fun leaveGame() {
        if (state !is GameState) {
            throw IllegalStateException("not able to leave the game not from game")
        }
        (state as GameState).leaveGame()
    }

    override fun setOnGameStateChangeListener(listener: OnGameStateChangeListener) {
        if (state !is GameState) {
            throw IllegalStateException("not able to handle game updates not out of game process")
        }
        (state as GameState).setOnGameStateChangeListener(listener)
    }

    override fun close() {
        context.inputSocket.close()
        context.outputSocket.close()
        state = HaltState(context)
    }
}
