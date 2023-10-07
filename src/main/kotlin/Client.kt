@file:Suppress("CanBeParameter", "unused")

import config.ClientSettings
import doc.Contract
import model.GameController
import model.LobbyController
import model.ProtectedController
import model.state.State
import java.io.Closeable

/**
 * Represents a client for interacting with the game system.
 * @param settings The client settings.
 * @param onStateChanged Callback for handling state changes.
 */
class Client(
    private val settings: ClientSettings,
    private val onStateChanged: (state: State) -> Unit
) : Closeable {
    private var protectedController = ProtectedController(settings) { state -> onStateChanged(state) }

    /**
     * Get the lobby controller.
     * @return The lobby controller.
     */
    @Contract("допустимые исключения: IllegalStateException, model.error.IllegalArgumentException, CriticalException")
    fun lobbyController(): LobbyController = protectedController

    /**
     * Get the game controller.
     * @return The game controller.
     */
    @Contract("допустимые исключения: IllegalStateException, model.error.IllegalArgumentException, CriticalException")
    fun gameController(): GameController = protectedController

    override fun close() {
        protectedController.close()
    }
}
