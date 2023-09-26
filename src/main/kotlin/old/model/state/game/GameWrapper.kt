package model.state.game;

import me.ippolitov.fit.snakes.SnakesProto.*
import model.state.game.engine.Field
import model.state.game.engine.Snake
import java.net.DatagramPacket

class GameWrapper(config: GameConfig) {
    private val field = Field(config)
    private val nodes: MutableMap<Int, NodeInfo> = mutableMapOf()
    private var state: GameState? = null

    init {
        val info = NodeInfo(GameConfig.masterIp(), GameConfig.masterPort(), NodeRole.MASTER, PlayerType.HUMAN)
        val player = field.createPlayer(config.playerName)
        this.nodes[player.id] = info
    }

    @Synchronized
    fun getState(): GameState? = state

    @Synchronized
    fun calculateStep(directions: Directions): GameState {
        val read = directions.readAll()
        val step = field.calculateStep(read)

        val builder = GameState.newBuilder()
        field.snakes.forEach { (_, snake) ->
            if (snake.status != Snake.Status.ALIVE) {
                return@forEach
            }

            val protoSnake = Mapper.toSnakeBuilder(snake)
            builder.snakesBuilderList.add(protoSnake)
        }

        field.food.forEach { (coord, _) ->
            val built = GameState.Coord.newBuilder()
                .setX(coord.x)
                .setY(coord.y)

            builder.foodsBuilderList.add(built)
        }

        state = builder.build()!!
        return state!!
    }

    @Synchronized
    fun createPlayer(
        joinMessage: DatagramPacket
    ): DatagramPacket {
        val snake = field.createPlayer(joinMessage.gameName)
        return GamePlayer.newBuilder()
            .setId(snake.id)
            .setName(snake.name)
            .setScore(snake.score)
            .setType(joinMessage.playerType)
            .setIpAddress(ip)
            .setPort(port)
            .setRole(role)
            .build()
    }

    @Synchronized
    fun canJoin(): Boolean {
        return true
    }

    @Synchronized
    fun players(): Array<GamePlayer.Builder> {
        val result: MutableList<GamePlayer.Builder> = mutableListOf()
        nodes.forEach { (id, node) ->
            val snake = field.snakes[id]!!
            result.add(
                Mapper.toGamePlayerBuilder(snake, node)
            )
        }
        return result.toTypedArray()
    }

    fun handleError(message) {

    }
}
