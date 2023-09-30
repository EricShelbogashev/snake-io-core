package model

import config.ClientSettings
import model.state.StateHolder

data class Context(
    val clientSettings: ClientSettings,
    val stateHolder: StateHolder
)