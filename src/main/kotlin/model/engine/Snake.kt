package model.engine

import model.api.v1.dto.Direction
import model.api.v1.dto.NodeRole
import mu.KotlinLogging

class Snake{
    private var _status: Status
    val playerId: Int
    val status: Status
        get() {
            return _status
        }

    var body: ArrayDeque<Coords>
    private val field: Field
    private val logger = KotlinLogging.logger {}

    constructor(field: Field, playerId: Int, head: Coords) {
        this.body = ArrayDeque(2)
        this.field = field
        this.playerId = playerId
        this._status = Status.ALIVE

        // Body initiation
        this.body.add(head)
        val tailCell = head.nearest().random()
        this.body.add(tailCell)
        field.snakes[this.playerId] = this
    }

    constructor(field: Field, playerId: Int, body: Array<Coords>) {
        this.body = ArrayDeque(body.size)
        this.body.addAll(body)
        this.field = field
        this.playerId = playerId
        this._status = Status.ALIVE
        field.snakes[playerId] = this
    }

    enum class Status {
        ALIVE, DEAD
    }

    private fun head(): Coords {
        assert(status == Status.ALIVE) {
            "Змея мертва и не имеет головы."
        }
        return body.first()
    }

    fun tail(): Coords {
        assert(status == Status.ALIVE) {
            "Змея мертва и не имеет хвоста."
        }
        return body.last()
    }

    fun move(direction: Direction) {
        assert(status == Status.ALIVE) {
            "После смерти змея не может двигаться."
        }
        if (direction().reverse() == direction) return moveForward()
        val newHead = newHead(this, direction)
        body.addFirst(newHead)

        logger.debug { "Змея двигается без изменений" }
        // Змея двигается без изменений
        if (!field.points.containsKey(newHead)) {
            // Голову не добавляем на поле, так как она будет просчитываться позже.
            field.points.remove(tail())
            body.removeLast()
            field.collisionsResolver.report(this, head())
            field.collisionsResolver.report(this, tail())
            return
        }

        val involvedPointCode = field.points[newHead]!!

        logger.debug { "Еда" }
        // Еда
        if (involvedPointCode < 0) {
            val player = field.players[playerId]
            player?.score = body.size - 2
            field.collisionsResolver.report(this, head())
            field.collisionsResolver.report(this, tail())
            logger.debug { "змея $playerId съела еду по координатам ${head()}" }
            return
        }

        logger.debug { "Другая змея" }
        // Другая змея
        val otherSnake: Snake = field.snakes[involvedPointCode]!!

        assert(otherSnake.status == Status.ALIVE) {
            "После смерти змея теряет тело, следовательно другая змея не может получить коллизию с телом погибшей змеи."
        }

        logger.debug { "Проверка на наличие коллизий." }
        /* Проверка на наличие коллизий. */
        /* Если столкновение с телом другой змеи, наша змея погибает. */
        if (newHead != otherSnake.head() && newHead != otherSnake.tail()) {
            logger.debug { "newHead=$newHead, otherSnake=$otherSnake" }
            die()
            return
        }

        logger.debug { " Столкновение с головой или хвостом может вызвать коллизию, поэтому решение этой проблемы делигируется " }
        /* Столкновение с головой или хвостом может вызвать коллизию, поэтому решение этой проблемы делигируется */
        /* классу CollisionsResolver. */
        field.collisionsResolver.report(this, newHead)
        field.collisionsResolver.report(this, tail())
    }

    fun moveForward() {
        assert(status == Status.ALIVE) {
            "После смерти змея не может двигаться."
        }
        return move(direction())
    }

    /* После вызова этого метода змею необходимо удалить из владения пользователя для утилизации GC. */
    fun die() {
        logger.debug { "Змея $playerId умерла" }
        assert(status == Status.ALIVE) {
            "Змея не может умереть дважды."
        }
        // Удаление змейки с поля.
        for (cell in body) {
            field.points.remove(cell)
            if (becomeFoodRandom() && cell != tail() && cell != head()) {
                field.food[cell] = Food(field, cell)
            }
        }
        body.clear()
        _status = Status.DEAD
        field.snakes.remove(playerId)

        if (field.players[playerId]?.role == NodeRole.MASTER) {
            field.masterDie()
            return
        }
        // Изменение статуса игрока на зрителя. Данное состояние не участвует ни в каких вычислениях, поэтому
        // необходимо удалить игрока при обнаружении этого статуса во время прочтения состояния игры.
        field.players[playerId]?.role = NodeRole.VIEWER
    }

    fun direction(): Direction {
        assert(status == Status.ALIVE) {
            "Мертвая змея не может иметь направления движения."
        }
        val head = body[0]
        val second = body[1]
        return when (head) {
            second.up() -> {
                Direction.UP
            }

            second.right() -> {
                Direction.RIGHT
            }

            second.down() -> {
                Direction.DOWN
            }

            else -> {
                Direction.LEFT
            }
        }
    }

    override fun toString(): String {
        return "Snake(player=$playerId, status=$status, body=$body)"
    }

    private companion object Utils {
        private val randomArray = arrayOf(true, false)
        fun newHead(snake: Snake, direction: Direction): Coords {
            return when (direction) {
                Direction.UP -> {
                    snake.body.first().up()
                }

                Direction.RIGHT -> {
                    snake.body.first().right()
                }

                Direction.DOWN -> {
                    snake.body.first().down()
                }

                Direction.LEFT -> {
                    snake.body.first().left()
                }
            }
        }

        fun becomeFoodRandom(): Boolean {
            return randomArray.random()
        }
    }
}