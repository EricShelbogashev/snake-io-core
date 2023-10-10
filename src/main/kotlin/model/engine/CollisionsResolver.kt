package model.engine;

import model.error.CriticalException
import mu.KotlinLogging

class CollisionsResolver(
    private var field: Field
) {
    private var collisions: MutableMap<Coords, MutableList<Snake>> = mutableMapOf()
    private var logger = KotlinLogging.logger {}

    fun report(snake: Snake, requestedPoint: Coords) {
        if (!collisions.containsKey(requestedPoint)) {
            collisions[requestedPoint] = mutableListOf(snake)
        } else {
            collisions[requestedPoint]?.add(snake)
        }
    }

    fun resolveAll() {
        for ((point, snakes) in collisions) {
            resolve(point, snakes)
        }
        refresh()
    }

    private fun resolve(point: Coords, snakes: MutableList<Snake>) {
        if (snakes.isEmpty()) {
            logger.error { "репорт на разрешение коллизий не может быть пустой, должна быть хотя бы одна змея" }
            throw CriticalException("репорт на разрешение коллизий не может быть пустой, должна быть хотя бы одна змея")
        }

        // Съесть еду.
        if (field.points[point] == -1) {
            logger.debug { "Съесть еду: ${field.points[point]}" }
            field.food[point]!!.eat()
        }

        // Просто проехалась по полю.
        if (snakes.size == 1) {
            val headOrTail = field.points[point]
            val snake = snakes[0]
            logger.error {
                "headOrTail=$headOrTail, snake.playerId=${snake.playerId}"
            }
            if (headOrTail != null && headOrTail != snake.playerId && headOrTail >= 0) {
                logger.info { "Лобовое столкновение." }
                // Место головы занято телом другой змеи (еда уже съедена, так что >= 0)
                // На месте хвоста не может быть другая змея, так что headOrTail - это head
                snake.die()
                logger.debug { field.snakes }
                field.snakes[headOrTail]!!.die()
                logger.debug { "Лобовое столкновение." }
                return
            }
            field.points[point] = snakes[0].playerId
            return
        }

        val tail = snakes.find {
            if (it.status == Snake.Status.DEAD) return@find false
            it.tail() == point
        }
        // Если есть хвост, значит головы в него врезались.
        if (tail != null) {
            field.points[point] = tail.playerId
            for (snake in snakes) {
                if (snake != tail && snake.status == Snake.Status.ALIVE) {
                    snake.die()
                }
            }
//            logger.info { "Если есть хвост, значит головы в него врезались." }
            return
        }

        // Если хвоста нет, значит встретились головами
        for (snake in snakes) {
            if (snake.status == Snake.Status.ALIVE) {
                snake.die()
            }
//            logger.info { "Если хвоста нет, значит встретились головами" }
        }
    }

    private fun refresh() {
        collisions.clear()
    }
}