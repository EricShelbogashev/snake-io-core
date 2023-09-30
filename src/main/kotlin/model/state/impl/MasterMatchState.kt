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

    @Suppress("MayBeConstant")
    private companion object {
        val USED_THREADS_NUMBER = 2
        val MASTER_PLAYER_ID = 0
        val MASTER_PLAYER = Player(
            "this machine's ip address is unavailable",
            0,
            NodeRole.MASTER,
            PlayerType.HUMAN,
            0,
            "set me",
            MASTER_PLAYER_ID
        )
        /*
            Задержка запуска игры после вызова конструктора.
        */
        val GAME_START_DELAY_MS: Long = 1000
    }

    private val executors = Executors.newScheduledThreadPool(USED_THREADS_NUMBER)
    private val directions: DirectionsHolder
    private val field: Field
    private val requestController: RequestController

    //    private var sequenceNumber = Long.MIN_VALUE
//    private val masterName: String = playerName
//    private val defaultMasterPlayerId = 0
    init {
        this.requestController = RequestController(
            multicastGroup = context.clientSettings.gameGroupAddress,
            multicastReceiveSocket = context.clientSettings.multicastReceiveSocket,
            generalSocket = context.clientSettings.generalSocket,
            networkInterface = context.clientSettings.networkInterface,
        )
        this.field = Field(config, Defaults.MASTER_PLAYER)
        this.directions = DirectionsHolder()

        // Задача на обновление игрового состояния. Начинает работать через секунду после
        executors.scheduleAtFixedRate({
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
        }, Defaults.GAME_START_DELAY_MS, config.stateDelayMs.toLong(), TimeUnit.MILLISECONDS)

        executors.scheduleAtFixedRate({
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
            defaultMasterPlayerId,
            direction,
            sequenceNumber++
        )
    }

    override fun gameName(): String = gameName
    override fun currentPlayer(): Player = master
}