@file:Suppress("CanBeParameter", "unused")

import config.ClientSettings
import doc.Contract
import model.GameController
import model.LobbyController
import model.ProtectedController
import model.state.State
import java.io.Closeable

class Client(
    private val settings: ClientSettings,
    private val onStateChanged: (state: State) -> Unit
) : Closeable {
    private var protectedController = ProtectedController(settings) { state -> onStateChanged(state) }

    @Contract("допустимые исключения: IllegalStateException, model.error.IllegalArgumentException, CriticalException")
    fun lobbyController(): LobbyController = protectedController

    @Contract("допустимые исключения: IllegalStateException, model.error.IllegalArgumentException, CriticalException")
    fun gameController(): GameController = protectedController

    override fun close() {
        protectedController.close()
    }
}