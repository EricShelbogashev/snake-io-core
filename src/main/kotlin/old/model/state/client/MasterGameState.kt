package model.state.client

import com.google.protobuf.InvalidProtocolBufferException
import me.ippolitov.fit.snakes.SnakesProto.GameMessage
import Context
import model.controller.OnGameStateChangeListener
import model.state.game.Directions
import model.state.game.GameConfig
import model.state.game.GameWrapper
import state.GameState
import java.net.DatagramPacket
import java.util.*

class MasterGameState(context: Context, config: GameConfig) : GameState(context) {
    private var onGameStateChangeListener: OnGameStateChangeListener? = null
    private val gameWrapper = GameWrapper(config)
    private val directions = Directions()
    private val publisher = Publisher(context, GameConfig.masterId())
    private val announcementPublisher = object : TimerTask() {
        override fun run() {
            if (gameWrapper.getState() == null) return
            publisher.announcement(
                "239.192.0.4",
                9192,
                config,
                gameWrapper.players(),
                gameWrapper.canJoin()
            )
        }
    }
    private val gameStateUpdate = object : TimerTask() {
        override fun run() {
            gameWrapper.calculateStep(directions)
        }
    }
    private val socketListener = SocketListener(context, publisher, config, gameWrapper, directions)

    private class SocketListener(
        private val context: Context,
        private val publisher: Publisher,
        private val config: GameConfig,
        private val gameWrapper: GameWrapper,
        private val directions: Directions
    ) : Thread() {
        override fun run() {
            val buffer = ByteArray(16 * 1024)
            while (true) {
                val packet = DatagramPacket(buffer, buffer.size)
                context.outputSocket.receive(packet)
                val gameMessage = try {
                    GameMessage.parseFrom(packet.data)
                } catch (e: InvalidProtocolBufferException) {
                    e.printStackTrace()
                    continue
                }

                when (gameMessage.typeCase) {
                    GameMessage.TypeCase.PING -> {
                        println("Получено ping сообщение")
                        // TODO: обновить информацию о node, что связь не потеряна.
                    }

                    GameMessage.TypeCase.STEER -> {
                        println("[Получено][${packet.address.address}:${packet.port}] : Steer")
                        directions.request(packet)
                        println("[Обработано][${packet.address.address}:${packet.port}] : Steer")
                    }

                    GameMessage.TypeCase.ACK -> {
                        println("[Получено][${packet.address.address}:${packet.port}] : Ack")
                        println("[Проигнорировано][${packet.address.address}:${packet.port}] : Ack")
                    }

                    GameMessage.TypeCase.STATE -> {
                        println("[Получено][${packet.address.address}:${packet.port}] : State")
                        println("[Проигнорировано][${packet.address.address}:${packet.port}] : State")
                    }

                    GameMessage.TypeCase.ANNOUNCEMENT -> {
                        println("[Получено][${packet.address.address}:${packet.port}] : Announcement")
                        println("[Проигнорировано][${packet.address.address}:${packet.port}] : Announcement")
                    }

                    GameMessage.TypeCase.JOIN -> {
                        println("[Получено][${packet.address.address}:${packet.port}] : Join")
                        context.outputSocket.send(
                            gameWrapper.createPlayer(packet)
                        )
                        println("[Обработано][${packet.address.address}:${packet.port}] : Join")
                    }

                    GameMessage.TypeCase.ERROR -> {
                        TODO()
                    }
                    GameMessage.TypeCase.ROLE_CHANGE -> TODO()
                    GameMessage.TypeCase.DISCOVER -> {
                        publisher.announcement(
                            packet.address.hostAddress,
                            packet.port,
                            config,
                            gameWrapper.players(),
                            gameWrapper.canJoin()
                        )
                    }

                    else -> {
                        System.err.println("Неизвестное сообщение $packet")
                    }
                }
            }
        }
    }

    init {
        val timer1 = Timer()
        timer1.scheduleAtFixedRate(gameStateUpdate, 0, config.stateDelayMs.toLong())
        val timer2 = Timer()
        timer2.scheduleAtFixedRate(announcementPublisher, 0, 1000)
        socketListener.start()
    }

    override fun leaveGame() {
//        Coords(Field(GameConfig()),4,4).down()
        TODO("Not yet implemented")
    }

    override fun setOnGameStateChangeListener(listener: OnGameStateChangeListener) {
        this.onGameStateChangeListener = listener
    }
}