package model

import api.v1.dto.*
import me.ippolitov.fit.snakes.SnakesProto
import me.ippolitov.fit.snakes.SnakesProto.GamePlayer
import model.api.v1.dto.GameConfig
import java.net.InetSocketAddress
import java.util.function.Function

object Mapper {
    fun toProtoPing(ping: Ping): SnakesProto.GameMessage {
        val protoPing = SnakesProto.GameMessage.PingMsg.newBuilder().build()
        return SnakesProto.GameMessage.newBuilder()
            .setPing(protoPing)
            .setMsgSeq(ping.msgSeq)
            .setSenderId(ping.senderId)
            .build()
    }

    fun toProtoDirection(direction: Direction): SnakesProto.Direction {
        return when (direction) {
            Direction.UP -> SnakesProto.Direction.UP
            Direction.RIGHT -> SnakesProto.Direction.RIGHT
            Direction.DOWN -> SnakesProto.Direction.DOWN
            Direction.LEFT -> SnakesProto.Direction.LEFT
        }
    }

    fun toProtoAnnouncement(announcement: Announcement): SnakesProto.GameMessage.AnnouncementMsg {
        val protoAnnouncementMsgBuilder = SnakesProto.GameMessage.AnnouncementMsg.newBuilder()

        val protoGames = announcement.games.map(::toProtoGame)
        protoAnnouncementMsgBuilder.addAllGames(protoGames)

        return protoAnnouncementMsgBuilder.build()
    }

    private fun toProtoGamePlayersBuilder(players: Array<Player>): SnakesProto.GamePlayers.Builder {
        val builder = SnakesProto.GamePlayers.newBuilder()
        val protoGamePlayers = players.map(::toProtoPlayer)
        builder.addAllPlayers(protoGamePlayers)
        return builder
    }

    private fun toProtoPlayer(player: Player): GamePlayer {
        return toProtoPlayerBuilder(player).build()
    }

    private fun toProtoPlayerBuilder(player: Player): SnakesProto.GamePlayer.Builder {
        val protoNodeRole = toProtoNodeRole(player.role)
        val protoPlayerType = toProtoPlayerType(player.type)

        return SnakesProto.GamePlayer.newBuilder()
            .setId(player.id)
            .setName(player.name)
            .setIpAddress(player.ip)
            .setPort(player.port)
            .setRole(protoNodeRole)
            .setType(protoPlayerType)
            .setScore(player.score)
    }

    private fun toProtoPlayerType(type: PlayerType): SnakesProto.PlayerType {
        return when (type) {
            PlayerType.HUMAN -> SnakesProto.PlayerType.HUMAN
            PlayerType.ROBOT -> SnakesProto.PlayerType.ROBOT
        }
    }

    private fun toProtoNodeRole(nodeRole: NodeRole): SnakesProto.NodeRole {
        return when (nodeRole) {
            NodeRole.NORMAL -> SnakesProto.NodeRole.NORMAL
            NodeRole.VIEWER -> SnakesProto.NodeRole.VIEWER
            NodeRole.MASTER -> SnakesProto.NodeRole.MASTER
            NodeRole.DEPUTY -> SnakesProto.NodeRole.DEPUTY
        }
    }

    private fun toProtoGameConfigBuilder(config: GameConfig): SnakesProto.GameConfig.Builder {
        return SnakesProto.GameConfig.newBuilder()
            .setWidth(config.width)
            .setHeight(config.height)
            .setFoodStatic(config.foodStatic)
            .setStateDelayMs(config.stateDelayMs)
    }

    private fun toProtoGame(game: Game): SnakesProto.GameAnnouncement? {
        return toProtoGameBuilder(game).build()
    }

    private fun toProtoGameBuilder(game: Game): SnakesProto.GameAnnouncement.Builder {
        val protoPlayers = toProtoGamePlayersBuilder(game.players)
        val protoConfig = toProtoGameConfigBuilder(game.config)

        return SnakesProto.GameAnnouncement.newBuilder()
            .setPlayers(protoPlayers)
            .setConfig(protoConfig)
            .setCanJoin(game.canJoin)
            .setGameName(game.gameName)
    }

    fun toProtoJoin(join: Join): SnakesProto.GameMessage.JoinMsg {
        val protoPlayerType = toProtoPlayerType(join.playerType)
        val protoNodeRole = toProtoNodeRole(join.nodeRole)

        return SnakesProto.GameMessage.JoinMsg.newBuilder()
            .setGameName(join.gameName)
            .setPlayerName(join.playerName)
            .setPlayerType(protoPlayerType)
            .setRequestedRole(protoNodeRole)
            .build()
    }

    fun toProtoError(error: Error): SnakesProto.GameMessage.ErrorMsg {
        return SnakesProto.GameMessage.ErrorMsg.newBuilder()
            .setErrorMessage(error.message)
            .build()
    }

    fun toProtoRoleChange(roleChange: RoleChange): SnakesProto.GameMessage.RoleChangeMsg {
        val roleChangeMsgBuilder = SnakesProto.GameMessage.RoleChangeMsg.newBuilder()

        if (roleChange.senderRole != null) {
            val protoRole = toProtoNodeRole(roleChange.senderRole)
            roleChangeMsgBuilder.setSenderRole(protoRole)
        }

        if (roleChange.receiverRole != null) {
            val protoRole = toProtoNodeRole(roleChange.receiverRole)
            roleChangeMsgBuilder.setReceiverRole(protoRole)
        }

        return roleChangeMsgBuilder.build()
    }

    fun toProtoState(gameState: GameState): SnakesProto.GameMessage.StateMsg {
        val protoGameState = toProtoGameState(gameState)

        return SnakesProto.GameMessage.StateMsg.newBuilder()
            .setState(protoGameState)
            .build()
    }

    private fun toProtoGameState(gameState: GameState): SnakesProto.GameState {
        val protoPlayers = toProtoGamePlayersBuilder(gameState.players)
        val protoFood = toProtoFoodsBuilder(gameState.food)
        val protoSnakes = toProtoSnakesBuilder(gameState.snakes)

        return SnakesProto.GameState.newBuilder()
            .setStateOrder(gameState.number)
            .setPlayers(protoPlayers)
            .addAllFoods(protoFood)
            .addAllSnakes(protoSnakes)
            .build()
    }

    private fun toProtoSnakesBuilder(snakes: Array<Snake>): Iterable<SnakesProto.GameState.Snake> {
        return toIterableProtoEntityBuilder(snakes, Mapper::toProtoSnake)
    }

    private fun toProtoSnake(snake: Snake): SnakesProto.GameState.Snake {
        val protoDirection = toProtoDirection(snake.direction)
        val protoSnakeState = toProtoSnakeState(snake.state)
        val protoPoints = toProtoPoints(snake.points)

        return SnakesProto.GameState.Snake.newBuilder()
            .setHeadDirection(protoDirection)
            .setPlayerId(snake.playerId)
            .setState(protoSnakeState)
            .addAllPoints(protoPoints)
            .build()
    }

    private fun toProtoPoints(points: Array<Coords>): Iterable<SnakesProto.GameState.Coord> {
        return toIterableProtoEntityBuilder(points, Mapper::toProtoCoord)
    }

    private fun toProtoSnakeState(state: Snake.State): SnakesProto.GameState.Snake.SnakeState {
        return when (state) {
            Snake.State.ALIVE -> SnakesProto.GameState.Snake.SnakeState.ALIVE
            Snake.State.ZOMBIE -> SnakesProto.GameState.Snake.SnakeState.ZOMBIE
        }
    }

    private fun toProtoCoord(food: Food): SnakesProto.GameState.Coord {
        return SnakesProto.GameState.Coord.newBuilder()
            .setX(food.x)
            .setY(food.y)
            .build()
    }

    private fun toProtoCoord(point: Coords): SnakesProto.GameState.Coord {
        return SnakesProto.GameState.Coord.newBuilder()
            .setX(point.x)
            .setY(point.y)
            .build()
    }

    private fun toProtoFoodsBuilder(food: Array<Food>): Iterable<SnakesProto.GameState.Coord> {
        return toIterableProtoEntityBuilder(food, Mapper::toProtoCoord)
    }

    private fun <F, T> toIterableProtoEntityBuilder(food: Array<F>, mapper: Function<F, T>): Iterable<T> {
        return with(ArrayList<T>(food.size)) {
            food.forEach { unit ->
                val protoCoord = mapper.apply(unit)
                this.add(protoCoord)
            }
            this
        }
    }

    fun toMessage(address: InetSocketAddress, gameMessage: SnakesProto.GameMessage): Message {
        val message = if (gameMessage.hasAck()) {
            toAck(address, gameMessage)
        } else if (gameMessage.hasAnnouncement()) {
            toAnnouncement(address, gameMessage)
        } else if (gameMessage.hasDiscover()) {
            toDiscover(address, gameMessage)
        } else if (gameMessage.hasError()) {
            toError(address, gameMessage)
        } else if (gameMessage.hasState()) {
            toGameState(address, gameMessage)
        } else if (gameMessage.hasJoin()) {
            toJoin(address, gameMessage)
        } else if (gameMessage.hasPing()) {
            toPing(address, gameMessage)
        } else if (gameMessage.hasRoleChange()) {
            toRoleChange(address, gameMessage)
        } else if (gameMessage.hasSteer()) {
            toSteer(address, gameMessage)
        } else {
            return Message(address, gameMessage.senderId)
        }
        message.msgSeq = gameMessage.msgSeq
        return message
    }

    private fun toSteer(address: InetSocketAddress, gameMessage: SnakesProto.GameMessage): Steer {
        return Steer(address, gameMessage.senderId, toDirection(gameMessage.steer.direction))
    }

    private fun toDirection(direction: SnakesProto.Direction): Direction {
        return when (direction) {
            SnakesProto.Direction.UP -> Direction.UP
            SnakesProto.Direction.RIGHT -> Direction.RIGHT
            SnakesProto.Direction.DOWN -> Direction.DOWN
            SnakesProto.Direction.LEFT -> Direction.LEFT
        }
    }

    private fun toPing(address: InetSocketAddress, gameMessage: SnakesProto.GameMessage): Ping {
        return Ping(address, gameMessage.senderId)
    }

    private fun toRoleChange(address: InetSocketAddress, gameMessage: SnakesProto.GameMessage): RoleChange {
        return RoleChange(
            address,
            gameMessage.senderId,
            gameMessage.receiverId,
            toNodeRole(gameMessage.roleChange.senderRole),
            toNodeRole(gameMessage.roleChange.receiverRole)
        )
    }

    private fun toNodeRole(nodeRole: SnakesProto.NodeRole): NodeRole {
        return when (nodeRole) {
            SnakesProto.NodeRole.MASTER -> NodeRole.MASTER
            SnakesProto.NodeRole.DEPUTY -> NodeRole.DEPUTY
            SnakesProto.NodeRole.NORMAL -> NodeRole.NORMAL
            SnakesProto.NodeRole.VIEWER -> NodeRole.VIEWER
        }
    }

    private fun toJoin(address: InetSocketAddress, gameMessage: SnakesProto.GameMessage): Discover {
        return Discover(address, gameMessage.senderId)
    }

    private fun toGameState(address: InetSocketAddress, gameMessage: SnakesProto.GameMessage): Discover {
        return Discover(address, gameMessage.senderId)
    }


    private fun toError(address: InetSocketAddress, gameMessage: SnakesProto.GameMessage): Discover {
        return Discover(address, gameMessage.senderId)
    }

    private fun toDiscover(address: InetSocketAddress, gameMessage: SnakesProto.GameMessage): Discover {
        return Discover(address, gameMessage.senderId)
    }

    private fun toAck(address: InetSocketAddress, gameMessage: SnakesProto.GameMessage): Ack {
        return Ack(address, gameMessage.senderId, gameMessage.receiverId)
    }

    private fun toAnnouncement(address: InetSocketAddress, gameMessage: SnakesProto.GameMessage): Announcement {
        return Announcement(address, gameMessage.senderId, toGames(gameMessage.announcement))
    }

    private fun toGames(announcement: SnakesProto.GameMessage.AnnouncementMsg): Array<Game> {
        return announcement.gamesList.map { gameAnnouncement: SnakesProto.GameAnnouncement ->
            Game(
                gameName = gameAnnouncement.gameName,
                config = GameConfig(
                    gameAnnouncement.config.width,
                    gameAnnouncement.config.height,
                    gameAnnouncement.config.foodStatic,
                    gameAnnouncement.config.stateDelayMs
                ),
                canJoin = gameAnnouncement.canJoin,
                players = gameAnnouncement.players.playersList.map { protoGamePlayer ->
                    toPlayer(protoGamePlayer)
                }.toTypedArray()
            )
        }.toTypedArray()
    }

    private fun toPlayer(protoGamePlayer: GamePlayer): Player {
        return Player(
            ip = protoGamePlayer.ipAddress,
            port = protoGamePlayer.port,
            role = toNodeRole(protoGamePlayer.role),
            type = toPlayerType(protoGamePlayer.type),
            score = protoGamePlayer.score,
            name = protoGamePlayer.name,
            id = protoGamePlayer.id
        )
    }

    private fun toPlayerType(type: SnakesProto.PlayerType): PlayerType {
        return when (type) {
            SnakesProto.PlayerType.HUMAN -> PlayerType.HUMAN
            SnakesProto.PlayerType.ROBOT -> PlayerType.ROBOT
        }
    }
}
