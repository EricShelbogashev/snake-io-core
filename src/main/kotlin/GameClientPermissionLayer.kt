import api.v1.controller.GameNetController
import api.v1.dto.Announcement
import api.v1.dto.Direction
import controller.GameController
import controller.LobbyController
import model.GameConfig
import state.HaltState
import state.LobbyState
import state.MatchState
import state.State
import java.io.Closeable
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.NetworkInterface

class GameClientPermissionLayer(
    gameGroupAddress: InetSocketAddress,
    private val stateListener: (state: State) -> Unit
) :
    LobbyController,
    GameController,
    Closeable {
    private val inputSocket = MulticastSocket(gameGroupAddress)
    private val commonSocket = DatagramSocket()
    private var context = Context(
        inputSocket = inputSocket,
        commonSocket = commonSocket,
        gameGroupAddress = gameGroupAddress,
        gameClientPermissionLayer = this,
        gameNetController = GameNetController(
            multicastGroup = gameGroupAddress,
            recieveSocket = inputSocket,
            commonSocket = commonSocket,
            networkInterface = NetworkInterface.getByName("wlan0")
        )
    )
    var state: State = LobbyState(context)

    fun changeState(state: State) {
        this.state = state
        stateListener(state)
    }

    override fun newGame(playerName: String, gameName: String, config: GameConfig) {
        if (state !is LobbyState) {
            throw IllegalStateException("not able to start new game not from lobby")
        }
        (state as LobbyState).newGame(playerName, gameName, config)
    }

    override fun joinGame(playerName: String, gameName: String) {
        if (state !is LobbyState) {
            throw IllegalStateException("not able to join the game not from lobby")
        }
        (state as LobbyState).joinGame(playerName, gameName)
    }

    override fun watchGame(playerName: String, gameName: String) {
        if (state !is LobbyState) {
            throw IllegalStateException("not able to watch the game not from lobby")
        }
        (state as LobbyState).watchGame(playerName, gameName)
    }

    override fun setGameAnnouncementListener(action: (announcement: Announcement) -> Unit) {
        if (state !is LobbyState) {
            throw IllegalStateException("not able to watch the announcements not from lobby")
        }
        (state as LobbyState).setGameAnnouncementListener(action)
    }

    override fun removeGameAnnouncementListener() {
        if (state !is LobbyState) {
            throw IllegalStateException("not able to stop watch the announcements not from lobby")
        }
        (state as LobbyState).removeGameAnnouncementListener()
    }

    override fun leaveGame() {
        if (state !is MatchState) {
            throw IllegalStateException("not able to leave the game not from game")
        }
        (state as MatchState).leaveGame()
    }

    override fun setOnGameStateChangeListener(action: (state: api.v1.dto.GameState) -> Unit) {
        if (state !is MatchState) {
            throw IllegalStateException("not able to handle game updates not out of game process")
        }
        (state as MatchState).setOnGameStateChangeListener(action)
    }

    override fun config(): GameConfig {
        if (state !is MatchState) {
            throw IllegalStateException("not able to get config not from game state")
        }
        return (state as MatchState).config()
    }

    override fun move(direction: Direction) {
        if (state !is MatchState) {
            throw IllegalStateException("not able to move snake not from game process")
        }
        return (state as MatchState).move(direction)
    }

    override fun close() {
        context.inputSocket.close()
        context.commonSocket.close()
        state = HaltState(context)
    }
}
