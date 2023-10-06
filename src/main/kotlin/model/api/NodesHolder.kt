package model.api

import model.api.v1.dto.NodeRole
import mu.KotlinLogging
import java.io.Closeable
import java.net.InetSocketAddress
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

// 1. Если мы получили подтверждение входа, тогда мы подключаемся к ноде master.
// 1.1. Если мы имеем роль NORMAL или VIEWER, тогда нас не должны волновать другие ноды и достаточно хранить мастера.
// 1.2. Если мы имеем роль заместителя, в таком случае необходимо сохранять ноды других игроков во временном хранилище
//      ИЛИ создать функцию sendAll(gameState: GameState), которая будет извлекать адреса пользователей из сообщения
//      GameState, тем самым понимая, кому отправлять, а отсюда же будет получать информацию для актуализации нод.
// 2. Если мы приняли Join (для отслеживания этих действий используется специальный интерфейс), тогда нода также
//    добавляется.
//
// Резюмируя, получаем, что в простых случаях нода добавляется когда мы отправляем + получаем подтверждение на
// присоединение (MASTER NODE) и когда принимаем запрос на подключение.
// В сложных случаях (когда наша роль меняется на заместителя), необходимо извне получить информацию о нодах.
// Чтобы сохранить интерфейс чистым (только от join все зависит), предлагается внедрить получение инофрмации о нодах
// во время смены роли из кешированного состояния игры, а само состояние игры кешировать каждый раз, когда происходит
// его получение или отправление. Функция же sendAll также должна присутствовать, однако она лишь будет производить
// то, что от нее и ожидается - итерироваться по игрокам и отправлять им состояние игры.
//
// Если мы Deputy, Normal или Viewer, нужно отправлять сообщения мастеру. Если же мастер отвалился, нужно получить
// нового из кеша => необходимо кешировать Game. Так DEPUTY узнавая, что MASTER-нода отвалилась,
// сразу назначает себя на исполнителя и подгружает из кеша ноды, ожидая, что они теперь будут обращаться к нему, а
// Normal и View используют кеш, чтобы найти мастера.
class NodesHolder(
    delay: Long,
    private val onNodeRemovedHandler: ((address: InetSocketAddress, role: NodeRole) -> Unit)
) : Closeable {
    private val connectedNodes: MutableMap<InetSocketAddress, Pair<NodeRole, Long>> = mutableMapOf()
    private val connectedNodesLock = Any()
    private var master: InetSocketAddress? = null
    private var deputy: InetSocketAddress? = null
    val size get() = connectedNodes.size
    val addresses get() = connectedNodes.keys
    private val scheduledExecutor = Executors.newScheduledThreadPool(SCHEDULED_DEFAULT_THREADS_NUM)
    private var scheduledFuture: ScheduledFuture<*>
    private val logger = KotlinLogging.logger { }
    private val delay: AtomicLong

    init {
        this.delay = AtomicLong(delay)
    }

    private val removingTask = {
        val now = System.currentTimeMillis()

        val toDelete: MutableList<InetSocketAddress> = mutableListOf()
        synchronized(connectedNodesLock) {
            for (node in connectedNodes) {
                if (now - node.value.second < delay) continue

                // Удалить узел в связи с превышением времени с последней актуализации.
                toDelete.add(node.key)
            }
        }
        for (deleted in toDelete) {
            // Не может быть, чтобы роль была null у известного узла.
            remove(deleted)
        }
    }

    companion object {
        const val SCHEDULED_DEFAULT_THREADS_NUM = 1
    }

    init {
        scheduledFuture =
            scheduledExecutor.scheduleAtFixedRate(removingTask, delay, delay, TimeUnit.MILLISECONDS)
    }

    fun put(address: InetSocketAddress, role: NodeRole) {
        synchronized(connectedNodesLock) {
            connectedNodes[address] = role to System.currentTimeMillis()
            if (role == NodeRole.MASTER) {
                master = address
            } else if (role == NodeRole.DEPUTY) {
                deputy = address
            }
        }
    }

    fun delay(value: Long) {
        delay.set(value)
        scheduledFuture.cancel(true)
        scheduledFuture =
            scheduledExecutor.scheduleAtFixedRate(removingTask, value, value, TimeUnit.MILLISECONDS)
    }

    fun remove(address: InetSocketAddress): NodeRole? {
        // По-факту мы общаемся только с теми нодами, с которыми мы общаемся. Обычно других коммуникаций не наблюдается.
        // Мультикаст не стоит учитывать, а все юникасты сохраняем и если нода молчит -> удалена -> сообщения через лямбду об этом.
        // Сообщаем игровому состоянию, потому что именно оно будет ставить статусы зомби на змейку и управлять пользователями
        // в игре.

        return synchronized(connectedNodesLock) {
            if (master == address) {
                master = null
            } else if (deputy == address) {
                deputy = null
            }
            val pair = connectedNodes.remove(address) ?: return null
            onNodeRemovedHandler(address, pair.first)
            pair.first
        }
    }

    fun actualize(address: InetSocketAddress) {
        val pair = connectedNodes[address] ?: return

        connectedNodes[address] = pair.first to System.currentTimeMillis()
    }

    fun get(address: InetSocketAddress): NodeRole? {
        return synchronized(connectedNodesLock) {
            connectedNodes[address]?.first
        }
    }

    // без синхронизации
    fun master(): InetSocketAddress? {
        return synchronized(connectedNodesLock) {
            master
        }
    }

    // без синхронизации
    fun deputy(): InetSocketAddress? {
        return synchronized(connectedNodesLock) {
            deputy
        }
    }

    fun clearNodes() {
        synchronized(connectedNodesLock) {
            connectedNodes.clear()
        }
    }

    override fun close() {
        scheduledExecutor.shutdown()
        connectedNodes.clear()
    }
}