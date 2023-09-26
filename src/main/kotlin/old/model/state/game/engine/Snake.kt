//package model.state.game.engine
//
//@Suppress("JoinDeclarationAndAssignment")
//internal class Snake(
//    field: Field, name: String, head: Coords
//) {
//    val id: Int
//    val score: Int
//    val name: String
//    private var _status: Status
//    val status: Status
//        get() {
//            return _status
//        }
//
//    val body: ArrayDeque<Coords>
//    private val field: Field
//
//    init {
//        this.body = ArrayDeque(2)
//        this.field = field
//        this.id = field.getId()
//        this.score = 0
//        this.name = name
//        this._status = Status.ALIVE
//
//        // Body initiation
//        this.body.add(head)
//        val tailCell = head.nearest().random()
//        this.body.add(tailCell)
//    }
//
//    enum class Status {
//        ALIVE, DEAD
//    }
//
//    private fun head(): Coords {
//        return body.first()
//    }
//
//    private fun tail(): Coords {
//        return body.last()
//    }
//
//    fun move(direction: Direction) {
//        assert(status == Status.ALIVE) {
//            "После смерти змея не может двигаться."
//        }
//        val newHead = newHead(this, direction)
//
//        // Змея двигается без изменений
//        if (!field.points.containsKey(newHead)) {
//            field.points[newHead] = id
//            field.points.remove(body.last())
//            body.addFirst(newHead)
//            body.removeLast()
//            return
//        }
//
//        val involvedPointCode = field.points[newHead]!!
//
//        // Еда
//        if (involvedPointCode < 0) {
//            val food = field.food[newHead]!!
//            food.eat()
//            body.addFirst(newHead)
//            field.points[newHead] = id
//            return
//        }
//
//        // Другая змея
//        val otherSnake: Snake = field.snakes[involvedPointCode]!!
//
//        assert(otherSnake.status == Status.ALIVE) {
//            "После смерти змея теряет тело, следовательно другая змея не может получить коллизию с телом погибшей змеи."
//        }
//
//        /* Проверка на наличие коллизий. */
//        /* Если столкновение с телом другой змеи, наша змея погибает. */
//        if (newHead != otherSnake.head() && newHead != otherSnake.tail()) {
//            die()
//        }
//
//        /* Столкновение с головой или хвостом может вызвать коллизию, поэтому решение этой проблемы делигируется */
//        /* классу CollisionsResolver. */
//        field.collisionsResolver.report(this, newHead)
//    }
//
//    fun moveForward() {
//        return move(direction())
//    }
//
//    private fun die() {
//        assert(status == Status.ALIVE) {
//            "Змея не может умереть дважды."
//        }
//        for (cell in body) {
//            if (becomeFoodRandom()) {
//                field.points.replace(cell, -1)
//                field.food[cell] = Food(field, cell)
//            } else {
//                field.points.remove(cell)
//            }
//        }
//        body.clear()
//        _status = Status.DEAD
//    }
//
//    fun direction(): Direction {
//        assert(status == Status.ALIVE) {
//            "Мертвая змея не может иметь направления движения."
//        }
//
//        val head = body[0]
//        val second = body[1]
//        return when (head) {
//            second.up() -> {
//                Direction.UP
//            }
//
//            second.right() -> {
//                Direction.RIGHT
//            }
//
//            second.down() -> {
//                Direction.DOWN
//            }
//
//            else -> {
//                Direction.LEFT
//            }
//        }
//    }
//
//    private companion object Utils {
//        private val randomArray = arrayOf(true, false)
//        fun newHead(snake: Snake, direction: Direction): Coords {
//            return when (direction) {
//                Direction.UP -> {
//                    snake.body.first().up()
//                }
//
//                Direction.RIGHT -> {
//                    snake.body.first().right()
//                }
//
//                Direction.DOWN -> {
//                    snake.body.first().down()
//                }
//
//                Direction.LEFT -> {
//                    snake.body.first().left()
//                }
//            }
//        }
//
//        fun becomeFoodRandom(): Boolean {
//            return randomArray.random()
//        }
//    }
//}