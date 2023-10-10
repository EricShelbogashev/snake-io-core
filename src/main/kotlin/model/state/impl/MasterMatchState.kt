@file:Suppress("JoinDeclarationAndAssignment")

package model.state.impl

import model.Context
import model.DirectionsHolder
import model.GameController
import model.api.JoinRequest
import model.api.v1.dto.*
import model.engine.Field
import model.error.CriticalException
import mu.KotlinLogging
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MasterMatchState : MatchState, GameController {
    private val playerName: String
    private val gameName: String
    private var deputy: Player?

    constructor(context: Context, playerName: String, gameName: String, config: GameConfig) : super(context, config) {
        this.playerName = playerName
        this.gameName = gameName
        this.master = Player(
            "this machine's ip address is unavailable",
            0,
            NodeRole.MASTER,
            PlayerType.HUMAN,
            0,
            playerName,
            MASTER_PLAYER_ID
        )
        this.field = Field(config, master, ::onDeathNotification)
        this.deputy = null
        this.directions = DirectionsHolder()
//        context.connectionManager.setOnRoleChangeHandler { address: InetSocketAddress, role: NodeRole ->
//            if (role == NodeRole.DEPUTY) {
//                val player = field.getPlayerByAddress(address)
//                    ?: throw CriticalException(
//                        "если игрока нет в MasterMatchState, значит обработчик удаления должен" +
//                                " был уже отработать и удаленный узел не мог учавствовать в смене ролей"
//                    )
//                player.role = NodeRole.DEPUTY
//            }
//        }

        // Задача на обновление игрового состояния. Начинает работать через секунду после

    }

    override fun initialize() {
        context.connectionManager.setOnJoinRequestHandler(::joinRequest)
        context.connectionManager.setOnSteerHandler(::directionRequest)
        executors.scheduleAtFixedRate({
            // Update game state.
            try {
                val gameState = synchronized(fieldLock) {
                    field.calculateStep(directions.readAll())
                } ?: return@scheduleAtFixedRate
                logger.debug { "Отправлено обновление игрового поля игроком ${master.name}\n$gameState" }
                // Send game update to nodes.
                field.players.values.forEach { player ->
                    if (player.id == master.id) return@forEach

                    gameState.address = player.address
                    gameState.let { context.connectionManager.send(it) }
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
        super.initialize()
    }

    constructor(
        context: Context,
        playerName: String,
        gameName: String,
        config: GameConfig,
        gameState: GameState,
        newMaster: Player
    ) : this(context, playerName, gameName, config) {
        field = Field(config, newMaster, gameState, ::onDeathNotification)
        master = newMaster
    }

    private companion object {
        const val USED_THREADS_NUMBER = 2
        const val MASTER_PLAYER_ID = 0

        /*
            Задержка запуска игры после вызова конструктора.
        */
        const val GAME_START_DELAY_MS: Long = 1000
        const val GAME_ANNOUNCEMENT_DELAY_MS: Long = 1000
    }

    private var master: Player
    private val executors = Executors.newScheduledThreadPool(USED_THREADS_NUMBER)
    private val directions: DirectionsHolder
    private var field: Field
    private val fieldLock = Any()
    private var sequenceNumber = Long.MIN_VALUE
    private val logger = KotlinLogging.logger {  }

    private fun directionRequest(steer: Steer) {
        directions.request(
            steer.senderId,
            steer.direction,
            steer.msgSeq
        )
    }

    private fun joinRequest(joinRequest: JoinRequest) {
        synchronized(fieldLock) {
            if (field.players.values.find { player -> player.address == joinRequest.join.address } != null) {
                return
            }
        }
        logger.info { joinRequest }
        val join = joinRequest.join
        if (join.nodeRole == NodeRole.MASTER || join.nodeRole == NodeRole.DEPUTY) {
            // ошибка, такое в функцию попасть не могло
            // должно было отсеяться в connectionManager
            throw CriticalException("")
        }

        val role = if (deputy == null && join.nodeRole != NodeRole.VIEWER) {
            NodeRole.DEPUTY
        } else join.nodeRole

        var player = Player(
            ip = join.address.hostName,
            port = join.address.port,
            role = role,
            type = join.playerType,
            score = -1,
            name = join.playerName,
            id = -1
        )

        if (joinRequest.join.nodeRole == NodeRole.VIEWER) {
            synchronized(fieldLock) {
                joinRequest.accept(field.addViewer(player))
            }
            return
        }

        if (role == NodeRole.DEPUTY) {
            deputy = player
        }

        player = try {
            synchronized(fieldLock) {
                field.addPlayer(player)
            }
        } catch (e: IllegalStateException) {
            context.connectionManager.send(
                model.api.v1.dto.Error(
                    join.address,
                    master.id,
                    "на поле нет места"
                )
            )
            joinRequest.decline()
            return
        }

        joinRequest.accept(player)
    }

    private fun onDeathNotification() {
        logger.info { "onDeathNotification()" }
        if (deputy == null) {
            context.stateHolder.change(
                LobbyState(context)
            )
            return
        }
        logger.info { "Становимся зрителем" }
//        val replacement = deputy
//        replacement?.role = NodeRole.MASTER
//        notification.replaceMe(replacement)
        context.connectionManager.send(
            RoleChange(
                deputy!!.address,
                master.id,
                deputy!!.id,
                NodeRole.VIEWER,
                NodeRole.MASTER
            )
        )
        context.stateHolder.change(
            NormalMatchState(
                context,
                master.id,
                gameName,
                config,
                master.name
            )
        )
    }

    override fun onNodeRemoved(address: InetSocketAddress, role: NodeRole) {
        logger.info { "onNodeRemoved() : address=$address, role=$role" }
        val player = field.getPlayerByAddress(address)
            ?: return
        synchronized(fieldLock) {
            field.removePlayer(player.id)
        }

        if (deputy == null || role == NodeRole.DEPUTY) {
            for (node in field.players.values) {
                if (node.id != master.id && node.role != NodeRole.VIEWER) {
                    context.connectionManager.send(
                        RoleChange(
                            node.address,
                            master.id,
                            node.id,
                            NodeRole.MASTER,
                            NodeRole.DEPUTY,
                        )
                    )
                    node.role = NodeRole.DEPUTY
                    deputy = node
                    return
                }
            }
        }
    }

    override fun close() {
        context.connectionManager.setOnJoinRequestHandler(null)
        executors.shutdown()
        super.close()
    }

    override fun move(direction: Direction) {
        directions.request(
            master.id,
            direction,
            sequenceNumber++
        )
    }

    override fun gameName(): String = gameName
    override fun currentPlayer(): Player = master
}
