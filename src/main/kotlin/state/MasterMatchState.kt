package state

import Context
import GameClientPermissionLayer
import api.v1.dto.NodeRole
import api.v1.dto.Player
import api.v1.dto.PlayerType
import controller.Directions
import controller.GameController
import model.GameConfig
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class MasterMatchState(context: Context, config: GameConfig, gameClientPermissionLayer: GameClientPermissionLayer) : MatchState(context, config,
    gameClientPermissionLayer
), GameController {
    private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(2) // Number of threads
    private val directions = Directions()

    init {
        field.addPlayer(
            masterPlayer(),
            true
        )
        scheduler.scheduleAtFixedRate({
            // Update game state.
            val gameState = field.calculateStep(directions.readAll())

            // Send game update to nodes.
            field.players.values.forEach { player ->
                gameState.address = player.address()
                controller.state(gameState)
            }

            // Update master's screen.
            updateGameState(gameState)
        }, 1000, config.stateDelayMs.toLong(), TimeUnit.MILLISECONDS)
    }

    fun masterPlayer(): Player {
        return Player(
            "",
            0,
            NodeRole.MASTER,
            PlayerType.HUMAN,
            0,
            config.playerName,
            0
        )
    }

    override fun leaveGame() {
        scheduler.shutdownNow()
        super.leaveGame()
    }
}