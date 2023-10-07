package model.engine

import model.api.v1.dto.*
import mu.KotlinLogging
import java.net.InetSocketAddress
import kotlin.random.Random
import kotlin.random.nextInt

class Field(
    val config: GameConfig,
    val master: Player
) {
    val players: MutableMap<Int, Player> = hashMapOf()
    val snakes: MutableMap<Int, Snake> = hashMapOf()
    val food: MutableMap<Coords, Food> = hashMapOf()
    val points: MutableMap<Coords, Int> = hashMapOf()
    val collisionsResolver = CollisionsResolver(this)
    private var poolIds: Int = master.id + 1
    private var gameStateNum: Int = Int.MIN_VALUE
    private val logger = KotlinLogging.logger {}

    init {
        addMaster(master)
    }

    constructor(config: GameConfig, master: Player, gameState: GameState) : this(config, master) {
        poolIds = (gameState.players.maxOfOrNull { selector -> selector.id } ?: master.id) + 1

        gameState.players.forEach { player ->
            val role = if (player.id == master.id) {
                NodeRole.MASTER
            } else {
                NodeRole.NORMAL
            }
            players[player.id] = Player(
                ip = player.ip,
                port = player.port,
                role = role,
                type = player.type,
                score = player.score,
                name = player.name,
                id = player.id
            )
        }

        gameState.snakes.forEach { snake ->
            snakes[snake.playerId] = Snake(
                field = this,
                playerId = snake.playerId,
                body = snake.points.map { coords -> Coords(this, coords.x, coords.y) }.toTypedArray()
            )
        }

        gameStateNum = gameState.number + 1
        gameState.food.forEach { food ->
            val coords = Coords(this, food.x, food.y)
            this.food[coords] = Food(this, coords)
        }
    }

    fun getId(): Int {
        return poolIds++
    }

    fun calculateStep(directions: Map<Int, Direction>): GameState {
        moveSnakes(directions)
        collisionsResolver.resolveAll()
        spawnFood()

        val step = gameStateNum++
        val sortedPlayers = players.values.sortedByDescending { it.score }

        return GameState(
            address = InetSocketAddress(0),
            senderId = master.id,
            number = step,
            players = sortedPlayers.toTypedArray(),
            food = food.values.toTypedArray(),
            snakes = snakes.values.map { it.toDto() }.toTypedArray()
        )
    }

    private fun moveSnakes(directions: Map<Int, Direction>) {
        (0 until poolIds).forEach { id ->
            val direction = directions[id]
            val snake = snakes[id] ?: return@forEach

            if (snake.status == Snake.Status.DEAD) {
                logger.warn("Attempt to control a dead snake by user $id.")
                return@forEach
            }

            if (direction == null) {
                snake.moveForward()
            } else {
                snake.move(direction)
            }
        }
    }

    private fun spawnFood() {
        val diff = players.size + config.foodStatic - food.size
        if (diff > 0) {
            repeat(diff) {
                Food(
                    this, Coords(
                        this,
                        x = Random.nextInt(0 until config.width),
                        y = Random.nextInt(0 until config.height)
                    )
                )
            }
        }
        // TODO: Calculate spawn points for players
    }

    fun addPlayer(player: Player): Player {
        if (/*TODO: Field doesn't have free squares*/ false) {
            throw IllegalStateException("Unable to create player: no free space")
        }

        // TODO: Provide a point
        val FAKE_HEAD = Coords(this, 1, 1)

        // Assign an identifier
        player.id = getId()
        player.score = 0
        players[player.id] = player
        Snake(this, player.id, FAKE_HEAD)
        return player
    }

    fun removePlayer(id: Int) {
        players.remove(id)
    }

    fun getPlayerByAddress(address: InetSocketAddress): Player? {
        return players.values.find { it.address == address }
    }

    private fun addMaster(player: Player) {
        val head = Coords(this, config.height / 2, config.width / 2)

        players[player.id] = player
        Snake(this, player.id, head)
    }
}
