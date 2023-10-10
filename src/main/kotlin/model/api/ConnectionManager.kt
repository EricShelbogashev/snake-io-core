@file:Suppress("RemoveRedundantQualifierName")

package model.api

import me.ippolitov.fit.snakes.SnakesProto
import model.Mapper
import model.api.controller.RequestController
import model.api.v1.dto.*
import model.error.CriticalException
import mu.KotlinLogging
import java.io.Closeable
import java.net.*
import java.time.Instant
import java.util.NoSuchElementException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong


// Держатель нод.
class ConnectionManager(
    private val multicastReceiveSocket: MulticastSocket,
    private val generalSocket: DatagramSocket,
    private val networkInterface: NetworkInterface,
    private val multicastGroup: InetSocketAddress
) : Closeable {
    // Для приема сообщений
    private var onAnnouncementHandler: ((announcement: Announcement) -> Unit)? = null
    private var onErrorHandler: ((error: model.api.v1.dto.Error) -> Unit)? = null
    private var onGameStateHandler: ((gameState: GameState) -> Unit)? = null

    //    private var onJoinHandler: ((join: Join) -> Unit)? = null
    private var onJoinRequestHandler: ((request: JoinRequest) -> Unit)? = null
    private var onRoleChangeHandler: ((address: InetSocketAddress, role: NodeRole) -> Unit)? = null
    private var onSteerHandler: ((steer: Steer) -> Unit)? = null
    private var onOtherHandler: ((message: Message) -> Unit)? = null
    private var onJoinAccepted: ((join: Join, playerId: Int) -> Unit)? = null
    private var onNodeRemovedHandler: ((address: InetSocketAddress, role: NodeRole) -> Unit)? = null
    private var cachedState: Game? = null
    private var cachedStateLock = Any()
    private val receiveExecutor = Executors.newFixedThreadPool(RECEIVE_DEFAULT_THREADS_NUM)
    private val scheduledExecutor = Executors.newScheduledThreadPool(SCHEDULED_DEFAULT_THREADS_NUM)
    private val playerId = AtomicInteger()
    private val delayMs = AtomicLong(DEFAULT_DELAY_MS)
    private val logger = KotlinLogging.logger {  }

    // Общее
    private var msgSeq = AtomicLong(Long.MIN_VALUE)
    private val requestController: RequestController = RequestController(generalSocket, networkInterface)
    private val sentMessages: MutableMap<Long, Pair<Message, Long>> = mutableMapOf()
    private val sentMessagesLock = Any()

    private var lastMessage: Long = 0

    // Ноды
    private val nodesHolder =
        NodesHolder((delayMs.get() * MAX_DELAY_COEFFICIENT).toLong()) { address: InetSocketAddress, role: NodeRole ->
            onNodeRemovedHandler?.invoke(address, role)
            nodeRemoveHandle(role)
        }

    private fun nodeRemoveHandle(role: NodeRole) {
        if (role == NodeRole.MASTER) {
            nodesHolder.freeze(delayMs.get() * 5)
            if (cachedState != null) {
                for (player in cachedState!!.players) {
                    // Мастер покинул игру, следовательно игнорируем.
                    if (player.role == NodeRole.MASTER) {
                        continue
                    }
                    // Если среди нод были мы, игнорируем. Нельзя хранить информацию о нашей ноде среди других.
                    if (player.role == NodeRole.DEPUTY) {
                        continue
                    }
                    logger.warn { "put1() : address=${player.address}, role=$role"}
                    nodesHolder.put(player.address, player.role)
                }
            }
            if (nodesHolder.deputy() != null) {
                logger.warn { "put2() : address=${nodesHolder.deputy()!!}, role=$role"}
                nodesHolder.put(nodesHolder.deputy()!!, NodeRole.MASTER)
            }
        }
    }

    fun trackNode(address: InetSocketAddress, role: NodeRole) {
        logger.warn { "put3() : address=${address}, role=$role"}
        nodesHolder.put(address, role)
    }

    private val connectionWatchTask = {
        val noAnswered: MutableList<Pair<Message, Long>> = mutableListOf()
        val now = System.currentTimeMillis()
        val minTimeHasPassed = (delayMs.get() * DELAY_UPDATE_COEFFICIENT).toLong()
        synchronized(sentMessagesLock) {
            for (message in sentMessages) {
                if (now - message.value.second > minTimeHasPassed) {
                    noAnswered.add(message.value)
                }
            }
        }
        val maxTimeHasPassed = (delayMs.get() * MAX_DELAY_COEFFICIENT).toLong()
        noAnswered.forEach { message ->
            // Обработчик вызывается с фиксированной задеркой, поэтому нет необходимости обновлять время после обхода.
            // Но есть смысл проверить превышение максимально допустимой задержки.
            sendDirectly(message.first)

            if (now - message.second > maxTimeHasPassed) {
                synchronized(sentMessagesLock) {
                    sentMessages.remove(message.first.msgSeq)
                }
            }
        }
    }

    private val pingTask = {
        val now = System.currentTimeMillis()
        if (now - lastMessage > delayMs.get()) {
            lastMessage = now
            for (address in nodesHolder.addresses) {
                requestController.ping(
                    Ping(
                        address,
                        0
                    )
                )
            }
        }
    }

    private var scheduleConnectionWatchTask: ScheduledFuture<*>
    private var schedulePingTask: ScheduledFuture<*>

    init {
        this.multicastReceiveSocket.joinGroup(multicastGroup, networkInterface)
        this.generalSocket.joinGroup(multicastGroup, networkInterface)

        // Задача прослушивания сокета, отвечающего за multicast-группу.
        receiveExecutor.execute {
            while (true) {
                val gameMessage = try {
                    recieve(generalSocket)
                } catch (e: Throwable) {
                    e.printStackTrace()
                    continue
                }
                dispatch(gameMessage)
            }
        }
        // Задача прослушивания сокета, отвечающего за остальные сообщения.
        receiveExecutor.execute {
            while (true) {
                val gameMessage = try {
                    recieve(multicastReceiveSocket)
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
        scheduleConnectionWatchTask =
            scheduleConnectionWatchTask((DELAY_UPDATE_COEFFICIENT * delayMs.get()).toLong())
        schedulePingTask = schedulePingTask((DELAY_UPDATE_COEFFICIENT * delayMs.get()).toLong())
    }

    private fun scheduleConnectionWatchTask(delay: Long): ScheduledFuture<*> {
        return scheduledExecutor.scheduleAtFixedRate(
            connectionWatchTask,
            delay,
            delay,
            TimeUnit.MILLISECONDS
        )
    }

    private fun schedulePingTask(delay: Long): ScheduledFuture<*> {
        return scheduledExecutor.scheduleAtFixedRate(
            pingTask,
            delay,
            delay,
            TimeUnit.MILLISECONDS
        )
    }

    companion object {
        const val DEFAULT_DELAY_MS = 1000L
        const val DEFAULT_PACKET_LENGTH = 4 * 1024
        const val RECEIVE_DEFAULT_THREADS_NUM = 2
        const val SCHEDULED_DEFAULT_THREADS_NUM = 1
        const val MAX_DELAY_COEFFICIENT: Double = 0.8
        const val DELAY_UPDATE_COEFFICIENT: Double = 0.095
    }

    // Остальные сообщения реализуются ConnectionManager'ом.
    fun send(message: Message) {
        try {
            if (message.msgSeq == Message.DEFAULT_MESSAGE_SEQUENCE_NUMBER) {
                message.msgSeq = msgSeq.incrementAndGet()
            }
            val msgSeq = sendDirectly(message)
            synchronized(sentMessagesLock) {
                if (message !is Announcement) {
                    sentMessages[msgSeq] = message to Instant.now().toEpochMilli()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendDirectly(message: Message): Long {
        return when (message) {
            is Announcement -> {
                // Подстраиваемся под время задержки
                val del = message.games.maxOfOrNull { game -> game.config.stateDelayMs }
                if (del != null) setStateDelayMs(del.toLong())
                if (message.games.isNotEmpty()) {
                    cachedState = message.games.first()
                }
                requestController.announcement(message)
            }

            is model.api.v1.dto.Error -> requestController.error(message)
            is GameState -> {
                if (!nodesHolder.contains(message.address)) {
                    val found = message.players.find { player -> player.address == message.address }
                    if (found == null) {
                        logger.warn { "попытка отправить состояние не игроку" }
                    } else {
                        nodesHolder.put(found.address, found.role)
                    }
                }
                requestController.state(message)
            }
            is Join -> {
                if (onJoinAccepted == null) {
                    throw CriticalException("onJoinAccepted is not set")
                }
                requestController.join(message)
            }

            is RoleChange -> {
                if (message.receiverRole != null) {
                    logger.warn { "put5() : address=${message.address}, role=${message.receiverRole}"}
                    nodesHolder.put(message.address, message.receiverRole)
                }
                requestController.roleChange(message)
            }
            is Steer -> requestController.steer(message)
            is Ack -> throw CriticalException("ack is not supported")
            is Discover -> throw CriticalException("ack")
            is Ping -> throw CriticalException("ping message is not supported")
            else -> throw CriticalException("message is not supported")
        }
    }

    private fun dispatch(gameMessage: Message) {
        try {
            when (gameMessage) {
                is Announcement -> {
                    (onAnnouncementHandler ?: onOtherHandler)?.invoke(gameMessage)
                }
                is model.api.v1.dto.Error -> (onErrorHandler ?: onOtherHandler)?.invoke(gameMessage)
                is GameState -> {
                    if (onGameStateHandler == null) {
                        logger.warn { "onGameStateHandler == null" }
                        return
                    }

                    Ack(
                        address = gameMessage.address,
                        senderId = playerId.get(),
                        receiverId = gameMessage.senderId,
                        msgSeq = gameMessage.msgSeq
                    )

                    // Насытим мастера его адресом
                    gameMessage.master().ip = gameMessage.address.hostName
                    gameMessage.master().port = gameMessage.address.port
                    if (cachedState != null) {
                        cachedState!!.players = gameMessage.players
                    }
                    nodesHolder.actualize(gameMessage.address)
                    (onGameStateHandler ?: onOtherHandler)?.invoke(gameMessage)
                }

                is Join -> {
                    val joinRequest = JoinRequest(
                        join = gameMessage,
                        acceptAction = { player ->
                            requestController.ack(
                                Ack(
                                    address = player.address,
                                    senderId = playerId.get(),
                                    receiverId = player.id,
                                    msgSeq = gameMessage.msgSeq
                                )
                            )

                            // Если добавили нового игрока в игру.
                            logger.warn { "put8() : address=${player.address}, role=${player.role}"}
                            nodesHolder.put(player.address, player.role)
                        },
                        declineAction = {}
                    )
                    nodesHolder.freeze(delayMs.get() * 3)

                    onJoinRequestHandler?.invoke(joinRequest)
//                    (onJoinHandler ?: onOtherHandler)?.invoke(gameMessage)
                }

                is RoleChange -> {
                    handleRoleChange(gameMessage)
                    gameMessage.receiverRole?.let { onRoleChangeHandler?.invoke(InetSocketAddress(0), it) }
                    onOtherHandler?.invoke(gameMessage)
                }

                is Steer -> {
                    if (onSteerHandler == null) return

                    nodesHolder.actualize(gameMessage.address)
                    // Если сообщение получено от ноды, не учавствующей в игровом процессе.
                    val nodeRole = nodesHolder.get(gameMessage.address) ?: return
                    // Если зритель пытается управлять змеей (хотя у него нет на это прав, *даже если это его змея).
                    // * - к примеру, если MASTER покинул игру и вернулся.
                    if (nodeRole == NodeRole.VIEWER) return
                    (onSteerHandler ?: onOtherHandler)?.invoke(gameMessage)
                }

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
            if (gameMessage !is Ack && gameMessage !is Discover && gameMessage !is Announcement && gameMessage !is Ping && gameMessage !is GameState) {
                requestController.ack(
                    Ack(
                        address = gameMessage.address,
                        senderId = playerId.get(),
                        receiverId = gameMessage.senderId,
                        msgSeq = gameMessage.msgSeq
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
                    logger.warn { "put6() : address=${player.address}, role=${player.role}"}
                    nodesHolder.put(player.address, player.role)
                }
            }
        }
        // Если мы были MASTER, тогда сейчас происходит отключение и обрабатывать это не нужно.
        // Если мастер поменялся, то поймем мы это при удалении ноды (мастер умрет).
    }

    private fun inGame() : Boolean {
        return onNodeRemovedHandler != null && onGameStateHandler != null && onAnnouncementHandler == null
    }
    private fun inLobby() : Boolean {
        return onAnnouncementHandler != null
    }

    private fun handlePing(ping: Ping) {
        if (!inGame()) {
            return
        }
        requestController.ack(
            Ack(
                address = ping.address,
                senderId = playerId.get(),
                receiverId = ping.senderId,
                msgSeq = ping.msgSeq
            )
        )

        nodesHolder.actualize(ping.address)
    }

    private fun handleAck(ack: Ack) {
        val message = synchronized(sentMessagesLock) {
            nodesHolder.actualize(ack.address)
            sentMessages.remove(ack.msgSeq) ?: return
        }

        val join = message.first
        if (join is Join) {
            onJoinAccepted?.invoke(join, ack.receiverId)
            // Если успешно подключились к игре.
            logger.warn { "put7() : address=${ack.address}, NodeRole.MASTER"}
            nodesHolder.put(ack.address, NodeRole.MASTER)
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

    private fun recieve(socket: DatagramSocket): Message {
        val buffer = ByteArray(DEFAULT_PACKET_LENGTH)
        val packet = DatagramPacket(buffer, buffer.size)
        socket.receive(packet)

        val parsed = SnakesProto.GameMessage.parseFrom(buffer.copyOf(packet.length))
        return Mapper.toMessage(InetSocketAddress(packet.address, packet.port), parsed)
    }

    fun setPlayerId(id: Int) {
        playerId.set(id)
    }

    fun setStateDelayMs(stateDelayMs: Long) {
        val delay = (MAX_DELAY_COEFFICIENT * stateDelayMs).toLong()
        nodesHolder.delay(delay)
        delayMs.set(delay)

        schedulePingTask.cancel(true)
        scheduleConnectionWatchTask.cancel(true)
        schedulePingTask = schedulePingTask(delay)
        scheduleConnectionWatchTask = scheduleConnectionWatchTask(delay)
    }

    fun setOnOtherHandler(onOtherHandler: ((message: Message) -> Unit)?) {
        this.onOtherHandler = onOtherHandler
    }

    fun setOnNodeRemovedHandler(onNodeRemovedHandler: ((address: InetSocketAddress, role: NodeRole) -> Unit)?) {
        this.onNodeRemovedHandler = onNodeRemovedHandler
    }

    fun setOnJoinAccepted(onJoinAccepted: ((join: Join, playerId: Int) -> Unit)?) {
        this.onJoinAccepted = onJoinAccepted
    }

    fun setOnJoinRequestHandler(onJoinRequestHandler: ((joinRequest: JoinRequest) -> Unit)?) {
        this.onJoinRequestHandler = onJoinRequestHandler
    }

    fun setOnAnnouncementHandler(onAnnouncementHandler: ((announcement: Announcement) -> Unit)?) {
        this.nodesHolder.clearNodes()
        this.onAnnouncementHandler = onAnnouncementHandler
    }

    fun setOnErrorHandler(onErrorHandler: ((error: model.api.v1.dto.Error) -> Unit)?) {
        this.onErrorHandler = onErrorHandler
    }

    fun setOnGameStateHandler(onGameStateHandler: ((gameState: GameState) -> Unit)?) {
        this.onGameStateHandler = onGameStateHandler
    }

    fun setOnRoleChangeHandler(onRoleChangeHandler: ((address: InetSocketAddress, role: NodeRole) -> Unit)?) {
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
        try {
            this.generalSocket.leaveGroup(multicastGroup, networkInterface)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun closeHandlers() {
        setOnOtherHandler(null)
        setOnNodeRemovedHandler(null)
        setOnJoinAccepted(null)
        setOnAnnouncementHandler(null)
        setOnErrorHandler(null)
        setOnGameStateHandler(null)
        setOnRoleChangeHandler(null)
        setOnSteerHandler(null)
    }

    override fun close() {
        endReceive()
        nodesHolder.close()
        closeHandlers()
    }
}