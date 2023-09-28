package game.engine;

import api.v1.dto.Direction
import api.v1.dto.GameState
import api.v1.dto.Player
import model.GameConfig
import java.net.InetSocketAddress
import kotlin.random.Random
import kotlin.random.nextInt

class Field(
    val config: GameConfig,
    private val masterPlayerId: Int
) {
    val players: MutableMap<Int, Player> = hashMapOf()
    val snakes: MutableMap<Int, Snake> = hashMapOf()
    val food: MutableMap<Coords, Food> = hashMapOf()
    val points: MutableMap<Coords, Int> = hashMapOf()
    val collisionsResolver = CollisionsResolver(this)

    private var poolIds: Int = masterPlayerId + 1
    private var gameStateNum: Int = 0

    fun getId(): Int {
        return poolIds++
    }

    fun calculateStep(directions: Map<Int, Direction>): GameState {
        // Свиг змеек.
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

        // Спавн еды.
        val diff = players.size + config.foodStatic - food.size
        if (diff > 0) {
            for (i in 1..diff) {
                val foodObj = Food(
                    this, Coords(
                        this,
                        x = Random.nextInt(0..<config.width),
                        y = Random.nextInt(0..<config.height)
                    )
                )
                if (!points.containsKey(foodObj.coords())) {
                    points[foodObj.coords()] = -1
                    food[foodObj.coords()] = foodObj
                }
            }
        }
        // TODO: просчитать место для спавна игрока

        val step = gameStateNum++
        return GameState(
            address = InetSocketAddress(0),
            senderId = masterPlayerId,
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
        player.id = if (master) masterPlayerId else getId()
        player.score = 0
        players[player.id] = player
        Snake(this, player, FAKE_HEAD)
        return player
    }
}

