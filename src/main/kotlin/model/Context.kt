package model

import config.ClientSettings
import model.api.ConnectionManager
import model.state.StateHolder

data class Context(
    val clientSettings: ClientSettings,
    val stateHolder: StateHolder,
    val connectionManager: ConnectionManager
)