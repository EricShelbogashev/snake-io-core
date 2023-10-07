@file:Suppress("CanBeParameter")

package model

import config.ClientSettings
import model.api.ConnectionManager
import model.api.v1.dto.*
import model.state.HaltState
import model.state.State
import model.state.StateHolder
import model.state.impl.LobbyState
import model.state.impl.MatchState
import java.io.Closeable
import java.net.InetSocketAddress

/**
 * Represents a protected controller that manages game and lobby state transitions.
 *
 * @param clientSettings The client settings.
 * @param stateListener Callback for handling state changes.
 */
class ProtectedController(
    private val clientSettings: ClientSettings,
    private val stateListener: (state: State) -> Unit
) : LobbyController, GameController, Closeable {
    private val holder: StateHolder
    private val context: Context

    init {
        holder = StateHolder(transitionListener = stateListener)
        context = Context(
            clientSettings = clientSettings,
            stateHolder = holder,
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

    private inline fun <reified T : State> assertState() {
        require(holder.state() is T) { "Invalid state transition." }
    }

    override fun newGame(playerName: String, gameName: String, config: GameConfig) {
        assertState<LobbyState>()
        (holder.state() as LobbyState).newGame(playerName, gameName, config)
    }

    override fun joinGame(address: InetSocketAddress, playerName: String, gameName: String) {
        assertState<LobbyState>()
        (holder.state() as LobbyState).joinGame(address, playerName, gameName)
    }

    override fun watchGame(address: InetSocketAddress, playerName: String, gameName: String) {
        assertState<LobbyState>()
        (holder.state() as LobbyState).watchGame(address, playerName, gameName)
    }

    override fun setGameAnnouncementsListener(action: (announcements: List<Announcement>) -> Unit) {
        assertState<LobbyState>()
        (holder.state() as LobbyState).setGameAnnouncementsListener(action)
    }

    override fun exit() {
        assertState<LobbyState>()
        (holder.state() as LobbyState).close()
    }

    override fun leaveGame() {
        assertState<MatchState>()
        (holder.state() as MatchState).leaveGame()
    }

    override fun setOnGameStateChangeListener(action: (state: GameState) -> Unit) {
        assertState<MatchState>()
        (holder.state() as MatchState).setOnGameStateChangeListener(action)
    }

    override fun config(): GameConfig {
        assertState<MatchState>()
        return (holder.state() as MatchState).config()
    }

    override fun move(direction: Direction) {
        assertState<MatchState>()
        require(holder.state() is GameController) { "Not able to move snake in VIEWER mode or not from the game process" }
        (holder.state() as GameController).move(direction)
    }

    override fun gameName(): String {
        assertState<MatchState>()
        return (holder.state() as MatchState).gameName()
    }

    override fun currentPlayer(): Player {
        assertState<MatchState>()
        return (holder.state() as MatchState).currentPlayer()
    }

    override fun close() {
        holder.change(HaltState)
    }
}