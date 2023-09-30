package model

import config.ClientSettings
import model.api.v1.dto.*
import model.state.*
import model.state.impl.LobbyState
import model.state.impl.MatchState
import java.io.Closeable

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
            stateHolder = this.holder
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

    override fun joinGame(playerName: String, gameName: String) {
        if (holder.state() !is LobbyState) {
            throw IllegalStateException("not able to join the game not from lobby")
        }
        (holder.state() as LobbyState).joinGame(playerName, gameName)
    }

    override fun watchGame(playerName: String, gameName: String) {
        if (holder.state() !is LobbyState) {
            throw IllegalStateException("not able to watch the game not from lobby")
        }
        (holder.state() as LobbyState).watchGame(playerName, gameName)
    }

    override fun setGameAnnouncementsListener(action: (announcements: List<Announcement>) -> Unit) {
        if (holder.state() !is LobbyState) {
            throw IllegalStateException("not able to watch the announcements not from lobby")
        }
        (holder.state() as LobbyState).setGameAnnouncementsListener(action)
    }

    override fun removeGameAnnouncementsListener() {
        if (holder.state() !is LobbyState) {
            throw IllegalStateException("not able to stop watch the announcements not from lobby")
        }
        (holder.state() as LobbyState).removeGameAnnouncementsListener()
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
        return (holder.state() as MatchState).move(direction)
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
