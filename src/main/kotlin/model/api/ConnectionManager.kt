package model.api

import me.ippolitov.fit.snakes.SnakesProto
import model.Mapper
import model.api.v1.dto.*
import java.net.DatagramPacket
import java.net.InetSocketAddress

class ConnectionManager {
    private var recieveTask: Thread? = null
    private var onAckHandler: ((ack: Ack) -> Unit)? = null
    private var onAnnouncementHandler: ((announcement: Announcement) -> Unit)? = null
    private var onErrorHandler: ((error: Error) -> Unit)? = null
    private var onGameStateHandler: ((gameState: GameState) -> Unit)? = null
    private var onJoinHandler: ((join: Join) -> Unit)? = null
    private var onPingHandler: ((ping: Ping) -> Unit)? = null
    private var onRoleChangeHandler: ((roleChange: RoleChange) -> Unit)? = null
    private var onSteerHandler: ((steer: Steer) -> Unit)? = null
    private var onOtherHandler: ((message: Message) -> Unit)? = null
    private var cachedState: Game? = null

    init {
        this.multicastReceiveSocket.joinGroup(multicastGroup, networkInterface)

        this.recieveTask = Thread {
            while (true) {
                val gameMessage = try {
                    recieve()
                } catch (e: Throwable) {
                    e.printStackTrace()
                    continue
                }

                dispatch(gameMessage)
            }
        }
    }

    fun endReceive() {
        try {
            this.recieveTask!!.interrupt()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            this.multicastReceiveSocket.leaveGroup(multicastGroup, networkInterface)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dispatch(gameMessage: Message) = when (gameMessage) {
        is Ack -> (onAckHandler ?: onOtherHandler)?.invoke(gameMessage)
        is Announcement -> (onAnnouncementHandler ?: onOtherHandler)?.invoke(gameMessage)
        is Discover -> announcement(
            Announcement(
                gameMessage.address,
                gameMessage.senderId,
                if (cachedState != null) arrayOf(cachedState!!) else arrayOf()
            )
        )

        is Error -> (onErrorHandler ?: onOtherHandler)?.invoke(gameMessage)
        is GameState -> (onGameStateHandler ?: onOtherHandler)?.invoke(gameMessage)
        is Join -> (onJoinHandler ?: onOtherHandler)?.invoke(gameMessage)
        is Ping -> (onPingHandler ?: onPingHandler)?.invoke(gameMessage)
        is RoleChange -> (onRoleChangeHandler ?: onOtherHandler)?.invoke(gameMessage)
        is Steer -> (onSteerHandler ?: onOtherHandler)?.invoke(gameMessage)
        else -> {
            System.err.println(
                "Undefined message from ${gameMessage.address}, senderId=${gameMessage.senderId}, seq=$msgSeq"
            )
        }
    }

    private fun recieve(): Message {
        val buffer = ByteArray(4 * 1024)
        val packet = DatagramPacket(buffer, buffer.size)
        multicastReceiveSocket.receive(packet)

        val parsed = SnakesProto.GameMessage.parseFrom(buffer.copyOf(packet.length))
        return Mapper.toMessage(InetSocketAddress(packet.address, packet.port), parsed)
    }

    fun setOnOtherHandler(onOtherHandler: ((message: Message) -> Unit)?) {
        this.onOtherHandler = onOtherHandler
    }

    fun setOnAckHandler(onAckHandler: ((ack: Ack) -> Unit)?) {
        this.onAckHandler = onAckHandler
    }

    fun setOnAnnouncementHandler(onAnnouncementHandler: ((announcement: Announcement) -> Unit)?) {
        this.onAnnouncementHandler = onAnnouncementHandler
    }

    fun setOnErrorHandler(onErrorHandler: ((error: Error) -> Unit)?) {
        this.onErrorHandler = onErrorHandler
    }

    fun setOnGameStateHandler(onGameStateHandler: ((gameState: GameState) -> Unit)?) {
        this.onGameStateHandler = onGameStateHandler
    }

    fun setOnJoinHandler(onJoinHandler: ((join: Join) -> Unit)?) {
        this.onJoinHandler = onJoinHandler
    }

    fun setOnRoleChangeHandler(onRoleChangeHandler: ((roleChange: RoleChange) -> Unit)?) {
        this.onRoleChangeHandler = onRoleChangeHandler
    }

    fun setOnPingHandler(onPingHandler: ((ping: Ping) -> Unit)?) {
        this.onPingHandler = onPingHandler
    }

    fun setOnSteerHandler(onSteerHandler: ((steer: Steer) -> Unit)?) {
        this.onSteerHandler = onSteerHandler
    }
}