@file:Suppress("RemoveRedundantQualifierName")

package model.api

import me.ippolitov.fit.snakes.SnakesProto
import model.Mapper
import model.api.controller.RequestController
import model.api.v1.dto.*
import model.error.CriticalException
import java.io.Closeable
import java.net.DatagramPacket
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.NetworkInterface
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong


// Держатель нод.
class ConnectionManager(
    private val multicastReceiveSocket: MulticastSocket,
    private val networkInterface: NetworkInterface,
    private val multicastGroup: InetSocketAddress
) : Closeable {
    // Для приема сообщений
    private var onAnnouncementHandler: ((announcement: Announcement) -> Unit)? = null
    private var onErrorHandler: ((error: model.api.v1.dto.Error) -> Unit)? = null
    private var onGameStateHandler: ((gameState: GameState) -> Unit)? = null

    //    private var onJoinHandler: ((join: Join) -> Unit)? = null
    private var onJoinRequestHandler: ((request: JoinRequest) -> Unit)? = null
    private var onRoleChangeHandler: ((roleChange: RoleChange) -> Unit)? = null
    private var onSteerHandler: ((steer: Steer) -> Unit)? = null
    private var onOtherHandler: ((message: Message) -> Unit)? = null
    private var onJoinAccepted: ((playerId: Int) -> Unit)? = null
    private var onNodeRemovedHandler: ((address: InetSocketAddress, role: NodeRole) -> Unit)? = null
    private var cachedState: Game? = null
    private var cachedStateLock = Any()
    private val receiveExecutor = Executors.newFixedThreadPool(RECEIVE_DEFAULT_THREADS_NUM)
    private val scheduledExecutor = Executors.newScheduledThreadPool(SCHEDULED_DEFAULT_THREADS_NUM)
    private val playerId = AtomicInteger()
    private val delayMs = AtomicLong(DEFAULT_DELAY_MS)

    // Общее
    private val requestController: RequestController = RequestController(multicastReceiveSocket, networkInterface)
    private val sentMessages: MutableMap<Long, Pair<Message, Instant>> = mutableMapOf()
    private val sentMessagesLock = Any()

    // Ноды
    private val nodesHolder = NodesHolder()
    private val connectionWatchTask = {
        val noAnswered: MutableList<Pair<Message, Instant>> = mutableListOf()
        val now = Instant.now()
        val minTimeHasPassed = (delayMs.get() * DELAY_UPDATE_COEFFICIENT).toLong()
        synchronized(sentMessagesLock) {
            for (message in sentMessages) {
                if (message.value.second.plusMillis(minTimeHasPassed).isBefore(now)) {
                    noAnswered.add(message.value)
                }
            }
        }
        val maxTimeHasPassed = (delayMs.get() * MAX_DELAY_COEFFICIENT).toLong()
        noAnswered.forEach { message ->
            // Обработчик вызывается с фиксированной задеркой, поэтому нет необходимости обновлять время после обхода.
            // Но есть смысл проверить превышение максимально допустимой задержки.
            if (message.second.plusMillis(maxTimeHasPassed).isBefore(now)) {
                nodesHolder.removeNode(message.first.address)

            } else {
                send(message.first)
            }
        }
    }

    init {
        this.multicastReceiveSocket.joinGroup(multicastGroup, networkInterface)

        // Задача прослушивания сокета, отвечающего за multicast-группу.
        receiveExecutor.execute {
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

        // Задача, контролирующая подключенные ноды. По умолчанию предполагается,
        // что все ноды, с которыми происходит общение, подключены (таким образом, если они отключаются, происходит
        // уведомление об этом.
        scheduleConnectionWatchTask()
    }

    private fun scheduleConnectionWatchTask() {
        scheduledExecutor.scheduleAtFixedRate(
            connectionWatchTask,
            0,
            (DELAY_UPDATE_COEFFICIENT * delayMs.get()).toLong(),
            TimeUnit.MILLISECONDS
        )
    }

    companion object {
        const val DEFAULT_DELAY_MS = 1000L
        const val DEFAULT_PACKET_LENGTH = 4 * 1024
        const val RECEIVE_DEFAULT_THREADS_NUM = 1
        const val SCHEDULED_DEFAULT_THREADS_NUM = 1
        const val MAX_DELAY_COEFFICIENT: Double = 0.8
        const val DELAY_UPDATE_COEFFICIENT: Double = 0.095
    }

    // Остальные сообщения реализуются ConnectionManager'ом.
    fun send(message: Message) {
        when (message) {
            is Announcement -> requestController.announcement(message)
            is model.api.v1.dto.Error -> requestController.error(message)
            is GameState -> requestController.state(message)
            is Join -> {
                if (onJoinAccepted == null) {
                    throw IllegalStateException("onJoinAccepted is not set")
                }
                requestController.join(message)
            }

            is RoleChange -> requestController.roleChange(message)
            is Steer -> requestController.steer(message)
            is Ack -> throw CriticalException("ack is not supported")
            is Discover -> throw CriticalException("ack")
            is Ping -> throw CriticalException("ping message is not supported")
            else -> throw CriticalException("message is not supported")
        }
        synchronized(sentMessagesLock) {
            if (message !is Announcement) {
                sentMessages[message.msgSeq] = message to Instant.now()
            }
        }
    }

    private fun dispatch(gameMessage: Message) {
        try {
            when (gameMessage) {
                is Announcement -> {
                    (onAnnouncementHandler ?: onOtherHandler)?.invoke(gameMessage)
                }

                is model.api.v1.dto.Error -> (onErrorHandler ?: onOtherHandler)?.invoke(gameMessage)
                is GameState -> (onGameStateHandler ?: onOtherHandler)?.invoke(gameMessage)
                is Join -> {
                    val joinRequest = JoinRequest(
                        join = gameMessage,
                        acceptAction = { player ->
                            requestController.ack(
                                Ack(
                                    address = player.address(),
                                    senderId = playerId.get(),
                                    receiverId = player.id
                                )
                            )
                            // Если добавили нового игрока в игру.
                            nodesHolder.addNode(player.address(), player.role)
                        },
                        declineAction = {}
                    )

                    onJoinRequestHandler?.invoke(joinRequest)
//                    (onJoinHandler ?: onOtherHandler)?.invoke(gameMessage)
                }

                is RoleChange -> {
                    handleRoleChange(gameMessage)
                    (onRoleChangeHandler ?: onOtherHandler)?.invoke(gameMessage)
                }

                is Steer -> (onSteerHandler ?: onOtherHandler)?.invoke(gameMessage)
                is Ack -> handleAck(gameMessage)
                is Discover -> handleDiscover(gameMessage)
                is Ping -> handlePing(gameMessage)
                else -> {
                    System.err.println(
                        "Undefined message from ${gameMessage.address}, senderId=${gameMessage.senderId}, seq=${gameMessage.msgSeq}"
                    )
                }
            }
        } finally {
            if (gameMessage !is Ack && gameMessage !is Discover && gameMessage !is Announcement) {
                requestController.ack(
                    Ack(
                        address = gameMessage.address,
                        senderId = playerId.get(),
                        receiverId = gameMessage.senderId
                    )
                )
            }
        }
    }

    private fun handleRoleChange(roleChange: RoleChange) {
        // Если мы стали MASTER, нужно добавить в ноды всех, кто раньше был у мастера.
        if (roleChange.receiverRole == NodeRole.MASTER) {
            synchronized(cachedStateLock) {
                if (cachedState == null) {
                    // Этот случай обозначает, что мы подключились к серверу и были сразу обозначены как DEPUTY.
                    // До начала получения игровых обновлений мастер покинул сервер и отправил RoleChange.
                    // Формально такое возможно, особенно при больших задержках между переходами состояний, проблему
                    // можно частично* решить, отправив предыдущее состояние поля сразу после ответа на Join (Ask) или если нода
                    // была на View и заспросила присоединение, но вероятность события достаточно низкая, а игровой
                    // статус мы не успели получить (потому что еще не начали играть), следовательно просто покинуть игру -
                    // небольшая потеря для игрока. Если проигнорировать клиентом сообщение на receiverRole, мы по умолчанию
                    // будем думать, что находимся в состоянии Normal (View не может поддерживать игру), оперируя старыми
                    // данными из Announcement, следовательно, отсутствие сообщений от Master направит нас на общение с
                    // Deputy, который не будет присылать игровое поле, хотя обязан ответить Ack. Предлагается внести
                    // задачу в State, которая будет проверять частоту обновления игрового поля и в случае, если обновления
                    // не поступают в течение интервала stateDelayMs * 3 завершать игру.
                    //
                    // *частично - т.к. гарантий соблюдения этого другими клиентами нет.
                    //
                    // Здесь же можно просто проигнорировать логику и покинуть функцию.
                    return
                }

                for (player in cachedState!!.players) {
                    // Мастер покинул игру, следовательно игнорируем.
                    if (player.role == NodeRole.MASTER) {
                        continue
                    }
                    // Если среди нод были мы, игнорируем. Нельзя хранить информацию о нашей ноде среди других.
                    if (player.role == NodeRole.DEPUTY) {
                        continue
                    }
                    nodesHolder.addNode(player.address(), player.role)
                }
            }
        }
        // Если мы были MASTER, тогда сейчас происходит отключение и обрабатывать это не нужно.
        // Если мастер поменялся, то поймем мы это при удалении ноды (мастер умрет).
    }

    private fun handlePing(ping: Ping) {
        requestController.ack(
            Ack(
                address = ping.address,
                senderId = playerId.get(),
                receiverId = ping.senderId
            )
        )
    }

    private fun handleAck(ack: Ack) {
        val message = synchronized(sentMessagesLock) {
            sentMessages.remove(ack.msgSeq) ?: return
        }

        if (message.first is Join) {
            onJoinAccepted?.invoke(message.first.receiverId)
            // Если успешно подключились к игре.
            nodesHolder.addNode(ack.address, NodeRole.MASTER)
        }
    }

    private fun handleDiscover(discover: Discover) {
        requestController.announcement(
            Announcement(
                address = discover.address,
                senderId = discover.receiverId,
                games = if (cachedState != null) arrayOf(cachedState!!) else arrayOf()
            )
        )
    }

    private fun recieve(): Message {
        val buffer = ByteArray(DEFAULT_PACKET_LENGTH)
        val packet = DatagramPacket(buffer, buffer.size)
        multicastReceiveSocket.receive(packet)

        val parsed = SnakesProto.GameMessage.parseFrom(buffer.copyOf(packet.length))
        return Mapper.toMessage(InetSocketAddress(packet.address, packet.port), parsed)
    }

    fun setPlayerId(id: Int) {
        playerId.set(id)
    }

    fun setStateDelayMs(stateDelayMs: Long) {
        delayMs.set(stateDelayMs)
    }

    fun setOnOtherHandler(onOtherHandler: ((message: Message) -> Unit)?) {
        this.onOtherHandler = onOtherHandler
    }

    fun setOnNodeRemovedHandler(onNodeRemovedHandler: ((address: InetSocketAddress, role: NodeRole) -> Unit)?) {
        this.onNodeRemovedHandler = onNodeRemovedHandler
    }

    fun setOnJoinAccepted(onJoinAccepted: ((playerId: Int) -> Unit)?) {
        this.onJoinAccepted = onJoinAccepted
    }

    fun setOnAnnouncementHandler(onAnnouncementHandler: ((announcement: Announcement) -> Unit)?) {
        this.onAnnouncementHandler = onAnnouncementHandler
    }

    fun setOnErrorHandler(onErrorHandler: ((error: model.api.v1.dto.Error) -> Unit)?) {
        this.onErrorHandler = onErrorHandler
    }

    fun setOnGameStateHandler(onGameStateHandler: ((gameState: GameState) -> Unit)?) {
        this.onGameStateHandler = onGameStateHandler
    }

//    fun setOnJoinHandler(onJoinHandler: ((join: Join) -> Unit)?) {
//        this.onJoinHandler = onJoinHandler
//    }

    fun setOnRoleChangeHandler(onRoleChangeHandler: ((roleChange: RoleChange) -> Unit)?) {
        this.onRoleChangeHandler = onRoleChangeHandler
    }

    fun setOnSteerHandler(onSteerHandler: ((steer: Steer) -> Unit)?) {
        this.onSteerHandler = onSteerHandler
    }

    private fun endReceive() {
        try {
            this.receiveExecutor.shutdown()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            this.scheduledExecutor.shutdown()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            this.multicastReceiveSocket.leaveGroup(multicastGroup, networkInterface)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun close() {
        endReceive()
    }
}