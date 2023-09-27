package game.engine;

import api.v1.dto.Direction
import api.v1.dto.GameState
import api.v1.dto.Player
import model.GameConfig
import java.net.InetSocketAddress

class Field(
    val config: GameConfig
) {
    val players: MutableMap<Int, Player> = mutableMapOf()
    val snakes: MutableMap<Int, Snake> = mutableMapOf()
    val food: MutableMap<Coords, Food> = mutableMapOf()
    val points: MutableMap<Coords, Int> = mutableMapOf()
    val collisionsResolver = CollisionsResolver(this)

    private var poolIds: Int = config.masterPlayerId + 1
    private var gameStateNum: Int = 0

    fun getId(): Int {
        return poolIds++
    }

    fun calculateStep(directions: Map<Int, Direction>): GameState {
        (0..<poolIds).forEach { id ->
            val direction = directions[id]
            val snake = snakes[id]!!

            if (snake.status == Snake.Status.DEAD) {
                println("Попытка управлять мертвой змеей от пользователя $id.")
                return@forEach
            }

            if (direction == null) {
                snake.moveForward()
            } else {
                snake.move(direction)
            }
        }

        collisionsResolver.resolveAll()

        // TODO: спавнить еду
        // TODO: просчитать место для спавна игрока

        val step = gameStateNum++
        return GameState(
            address = InetSocketAddress(0),
            senderId = config.masterPlayerId,
            number = step,
            players = players.values.toTypedArray(),
            food = food.values.toTypedArray(),
            snakes = snakes.map { entry: Map.Entry<Int, Snake> -> entry.value.toDto() }.toTypedArray()
        )
    }

    fun addPlayer(player: Player, master: Boolean = false): Player {
        if (/*TODO: поле не содержит свободный квадрат*/ false) {
            throw IllegalStateException("not able to create player : no free space")
        }

        // TODO: предоставить точку
        val FAKE_HEAD = Coords(this, 5, 5)

        // Выдаем идентификатор
        player.id = if (master) config.masterPlayerId else getId()
        player.score = 0
        Snake(this, player, FAKE_HEAD)
        return player
    }
}

