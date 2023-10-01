@file:Suppress("JoinDeclarationAndAssignment")

package model.state.impl

import model.Context
import model.DirectionsHolder
import model.GameController
import model.api.v1.dto.*
import model.engine.Field
import model.api.controller.RequestController
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MasterMatchState(context: Context, val playerName: String, val gameName: String, config: GameConfig) :
    MatchState(context, config),
    GameController {

    private companion object {
        const val USED_THREADS_NUMBER = 2
        const val MASTER_PLAYER_ID = 0
        /*
            Задержка запуска игры после вызова конструктора.
        */
        const val GAME_START_DELAY_MS: Long = 1000
        const val GAME_ANNOUNCEMENT_DELAY_MS: Long = 1000
    }

    private val master = Player(
        "this machine's ip address is unavailable",
        0,
        NodeRole.MASTER,
        PlayerType.HUMAN,
        0,
        playerName,
        MASTER_PLAYER_ID
    )
    private val executors = Executors.newScheduledThreadPool(USED_THREADS_NUMBER)
    private val directions: DirectionsHolder
    private val field: Field
    private val requestController: RequestController
    private var sequenceNumber = Long.MIN_VALUE

    init {
        this.requestController = RequestController(
            socket = context.clientSettings.generalSocket,
            networkInterface = context.clientSettings.networkInterface,
        )
        this.field = Field(config, master)
        this.directions = DirectionsHolder()

        // Задача на обновление игрового состояния. Начинает работать через секунду после
        executors.scheduleAtFixedRate({
            // Update game state.
            val gameState = field.calculateStep(directions.readAll())
            // Send game update to nodes.
            field.players.values.forEach { player ->
                if (player.role == NodeRole.MASTER) return@forEach

                gameState.address = player.address()
                context.connectionManager.send(gameState)
            }

            // Update master's screen.
            updateGameState(gameState)

        }, GAME_START_DELAY_MS, config.stateDelayMs.toLong(), TimeUnit.MILLISECONDS)

        executors.scheduleAtFixedRate({
            // Send game announcements.
            context.connectionManager.send(
                Announcement(
                    address = context.clientSettings.gameGroupAddress,
                    senderId = MASTER_PLAYER_ID,
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
        }, 1000, GAME_ANNOUNCEMENT_DELAY_MS, TimeUnit.MILLISECONDS)
    }

    override fun leaveGame() {
        // TODO: сообщить об уходе
        super.leaveGame()
    }

    override fun close() {
        executors.shutdown()
        super.close()
    }

    override fun move(direction: Direction) {
        directions.request(
            MASTER_PLAYER_ID,
            direction,
            sequenceNumber++
        )
    }

    override fun gameName(): String = gameName
    override fun currentPlayer(): Player = master
}