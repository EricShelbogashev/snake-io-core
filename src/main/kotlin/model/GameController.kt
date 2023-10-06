package model

import model.api.v1.dto.Direction
import model.api.v1.dto.GameConfig
import model.api.v1.dto.GameState
import model.api.v1.dto.Player

interface GameController : ViewGameController {
    fun move(direction: Direction)
}