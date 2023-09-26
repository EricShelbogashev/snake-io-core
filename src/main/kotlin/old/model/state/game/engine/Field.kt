//package model.state.game.engine;
//
//import model.state.game.GameConfig
//
//internal class Field(
//    val config: GameConfig
//) {
//    val snakes: MutableMap<Int, Snake> = mutableMapOf()
//    val food: MutableMap<Coords, Food> = mutableMapOf()
//    val points: MutableMap<Coords, Int> = mutableMapOf()
//    val collisionsResolver = CollisionsResolver(this)
//
//    private var poolIds: Int = 0
//    private var gameStateNum: Int = 0
//
//    fun getId(): Int {
//        return poolIds++
//    }
//
//    fun calculateStep(directions: Map<Int, Direction>): Int {
//        (0..<poolIds).forEach { id ->
//            val direction = directions[id]
//            val snake = snakes[id]!!
//
//            if (snake.status == Snake.Status.DEAD) {
//                println("Попытка управлять мертвой змеей от пользователя $id.")
//                return@forEach
//            }
//
//            if (direction == null) {
//                snake.moveForward()
//            } else {
//                snake.move(direction)
//            }
//        }
//
//        collisionsResolver.resolveAll()
//
//        // TODO: спавнить еду
//        // TODO: просчитать место для спавна игрока
//
//        return gameStateNum++
//    }
//
//    fun createPlayer(name: String): Snake {
//        if (/*TODO: поле не содержит свободный квадрат*/ false) {
//            throw IllegalStateException("not able to create player : no free space")
//        }
//
//        // TODO: предоставить точку
//        val FAKE_HEAD = Coords(this, 5, 5)
//
//        return Snake(this, name, FAKE_HEAD)
//    }
//}
//
