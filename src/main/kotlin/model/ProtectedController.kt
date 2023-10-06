@file:Suppress("CanBeParameter")

package model

import config.ClientSettings
import model.api.ConnectionManager
import model.api.JoinRequest
import model.api.v1.dto.*
import model.state.HaltState
import model.state.State
import model.state.StateHolder
import model.state.impl.LobbyState
import model.state.impl.MatchState
import model.state.impl.NormalMatchState
import model.state.impl.ViewMatchState
import java.io.Closeable
import java.net.InetSocketAddress

/*
    Внешним образом состояние обновляется только в случае создания контроллера и при вызове close(),
    таким образом контроллер стоит воспринимать как держатель ресурса
*/
class ProtectedController(
    private val clientSettings: ClientSettings,
    private val stateListener: (state: State) -> Unit
) : LobbyController, GameController, Closeable {
    private val holder: StateHolder
    private val context: Context

    init {
        this.holder = StateHolder(transitionListener = stateListener)
        this.context = Context(
            clientSettings = clientSettings,
            stateHolder = this.holder,
            connectionManager = ConnectionManager(
                clientSettings.multicastReceiveSocket,
                clientSettings.generalSocket,
                clientSettings.networkInterface,
                clientSettings.gameGroupAddress
            )
        )
        initialize()
    }

    private fun initialize() {
        val initState = LobbyState(context = context)
        holder.change(initState)
    }

    override fun newGame(playerName: String, gameName: String, config: GameConfig) {
        if (holder.state() !is LobbyState) {
            throw IllegalStateException("not able to start new game not from lobby")
        }
        (holder.state() as LobbyState).newGame(playerName, gameName, config)
    }

    override fun joinGame(address: InetSocketAddress, playerName: String, gameName: String) {
        if (holder.state() !is LobbyState) {
            throw IllegalStateException("not able to join the game not from lobby")
        }
        (holder.state() as LobbyState).joinGame(address, playerName, gameName)
    }

    override fun watchGame(address: InetSocketAddress, playerName: String, gameName: String) {
        if (holder.state() !is LobbyState) {
            throw IllegalStateException("not able to watch the game not from lobby")
        }
        (holder.state() as LobbyState).watchGame(address, playerName, gameName)
    }

    override fun setGameAnnouncementsListener(action: (announcements: List<Announcement>) -> Unit) {
        if (holder.state() !is LobbyState) {
            throw IllegalStateException("not able to watch the announcements not from lobby")
        }
        (holder.state() as LobbyState).setGameAnnouncementsListener(action)
    }

    override fun exit() {
        if (holder.state() !is LobbyState) {
            throw IllegalStateException("not able to free lobby resources not from lobby")
        }
        (holder.state() as LobbyState).close()
    }

    override fun leaveGame() {
        if (holder.state() !is MatchState) {
            throw IllegalStateException("not able to leave the game not from game")
        }
        (holder.state() as MatchState).leaveGame()
    }

    override fun setOnGameStateChangeListener(action: (state: GameState) -> Unit) {
        if (holder.state() !is MatchState) {
            throw IllegalStateException("not able to handle game updates not out of game process")
        }
        (holder.state() as MatchState).setOnGameStateChangeListener(action)
    }

    override fun config(): GameConfig {
        if (holder.state() !is MatchState) {
            throw IllegalStateException("not able to get config not from game state")
        }
        return (holder.state() as MatchState).config()
    }

    override fun move(direction: Direction) {
        if (holder.state() !is MatchState) {
            throw IllegalStateException("not able to move snake not from game process")
        }
        if (holder.state() !is GameController) {
            throw IllegalStateException("not able to move snake in VIEWER mode")
        }
        return (holder.state() as GameController).move(direction)
    }

    override fun gameName(): String {
        if (holder.state() !is MatchState) {
            throw IllegalStateException("not able to get game name not from game state")
        }
        return (holder.state() as MatchState).gameName()
    }

    override fun currentPlayer(): Player {
        if (holder.state() !is MatchState) {
            throw IllegalStateException("not able to get current player not from game process")
        }
        return (holder.state() as MatchState).currentPlayer()
    }

    override fun close() {
        holder.change(HaltState)
    }
}
