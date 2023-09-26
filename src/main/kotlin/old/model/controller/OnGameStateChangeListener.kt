package model.controller

import me.ippolitov.fit.snakes.SnakesProto.GameState

interface OnGameStateChangeListener {
    fun updateState(announcement: GameState)
}