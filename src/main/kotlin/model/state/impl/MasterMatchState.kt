@file:Suppress("JoinDeclarationAndAssignment")

package model.state.impl

import model.Context
import model.DirectionsHolder
import model.GameController
import model.api.JoinRequest
import model.api.controller.RequestController
import model.api.v1.dto.*
import model.engine.Field
import model.error.CriticalException
import mu.KotlinLogging
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.Error

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
    private val fieldLock = Any()
    private var sequenceNumber = Long.MIN_VALUE
    private val logger = KotlinLogging.logger {}

    init {
        context.connectionManager.setOnJoinRequestHandler(::joinRequest)
        context.connectionManager.setOnSteerHandler(::directionRequest)
        this.field = Field(config, master)
        this.directions = DirectionsHolder()

        // Задача на обновление игрового состояния. Начинает работать через секунду после
        executors.scheduleAtFixedRate({
            // Update game state.
            logger.debug { "[game state update task] : executed" }

            try {
                val gameState = synchronized(fieldLock) {
                    field.calculateStep(directions.readAll())
                }
                gameState.snakes.forEach { snake ->
                    logger.info { snake.toString() }
                }
                // Send game update to nodes.
                field.players.values.forEach { player ->
                    if (player.role == NodeRole.MASTER) return@forEach

                    gameState.address = player.address
                    context.connectionManager.send(gameState)
                }

                // Update master's screen.
                updateGameState(gameState)
            } catch (e: Exception) {
                e.printStackTrace()
            }

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

    private fun directionRequest(steer: Steer) {
        directions.request(
            steer.senderId,
            steer.direction,
            steer.msgSeq
        )
    }

    private fun joinRequest(joinRequest: JoinRequest) {
        val join = joinRequest.join
        if (join.nodeRole == NodeRole.MASTER || join.nodeRole == NodeRole.DEPUTY) {
            // ошибка, такое в функцию попасть не могло
            // должно было отсеяться в connectionManager
            throw CriticalException("")
        }

        var player = Player(
            ip = join.address.hostName,
            port = join.address.port,
            role = join.nodeRole,
            type = join.playerType,
            score = -1,
            name = join.playerName,
            id = -1
        )

        if (player.role != NodeRole.VIEWER) {
            synchronized(fieldLock) {
                player = field.addPlayer(player)
            }
        }
        joinRequest.accept(player)
    }

    override fun leaveGame() {
        // TODO: сообщить об уходе
        super.leaveGame()
    }

    override fun onNodeRemoved(address: InetSocketAddress, role: NodeRole) {
        val player = field.getPlayerByAddress(address)
            ?: throw CriticalException("игрок не может быть null, так как контроллер гарантирует, что отключаться будут лишь ноды, учавствующие в игре")
        field.removePlayer(player.id)
    }

    override fun close() {
        context.connectionManager.setOnJoinRequestHandler(null)
        context.connectionManager.setOnNodeRemovedHandler(null)
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
