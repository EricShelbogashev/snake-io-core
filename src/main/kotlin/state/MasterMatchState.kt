package state

import Context
import api.v1.dto.*
import controller.Directions
import controller.GameController
import game.engine.Field
import model.GameConfig
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class MasterMatchState(context: Context, val playerName: String, val gameName: String, config: GameConfig) : MatchState(context, config),
    GameController {
    private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(2) // Number of threads
    private val directions = Directions()
    private var sequenceNumber = Long.MIN_VALUE
    private val masterName: String = playerName
    private val defaultMasterPlayerId = 0
    private val field = Field(config, defaultMasterPlayerId)

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
                if (player.role == NodeRole.MASTER) return@forEach

                gameState.address = player.address()
                context.gameNetController.state(gameState)
            }

            // Update master's screen.
            updateGameState(gameState)
        }, 1000, config.stateDelayMs.toLong(), TimeUnit.MILLISECONDS)

        scheduler.scheduleAtFixedRate({
            // Send game announcements.
            context.gameNetController.announcement(
                Announcement(
                    address = context.gameGroupAddress,
                    senderId = defaultMasterPlayerId,
                    games = arrayOf(
                        Game(
                            gameName = gameName,
                            config = config,
                            canJoin = true,
                            players = field.players.values.toTypedArray()
                        )
                    )
                )
            )
        }, 1000, context.announcementDelay, TimeUnit.MILLISECONDS)
    }

    private fun masterPlayer(): Player {
        return Player(
            "",
            0,
            NodeRole.MASTER,
            PlayerType.HUMAN,
            0,
            masterName,
            0
        )
    }

    override fun leaveGame() {
        scheduler.shutdownNow()
        super.leaveGame()
    }

    override fun move(direction: Direction) {
        directions.request(
            defaultMasterPlayerId,
            direction,
            sequenceNumber++
        )
    }
}