import model.DirectionsHolder
import model.api.v1.dto.GameConfig
import model.api.v1.dto.NodeRole
import model.api.v1.dto.Player
import model.api.v1.dto.PlayerType
import model.engine.Field

fun main() {
    val field = Field(
        GameConfig(),
        Player(
            "23",
            342,
            NodeRole.MASTER,
            type = PlayerType.HUMAN,
            0,
            "32423",
            0
        )
    )

    val directions = DirectionsHolder()

    field.addPlayer(
        Player(
            "23",
            423,
            NodeRole.NORMAL,
            type = PlayerType.HUMAN,
            0,
            "TEST",
            -1
        )
    )

    val deque1 = ArrayDeque<model.engine.Coords>(6)
    deque1.addLast(model.engine.Coords(field, 0, 0))
    deque1.addLast(model.engine.Coords(field, 0, 1))
    deque1.addLast(model.engine.Coords(field, 0, 2))
    deque1.addLast(model.engine.Coords(field, 0, 3))
    deque1.addLast(model.engine.Coords(field, 0, 4))
    deque1.addLast(model.engine.Coords(field, 0, 5))
    deque1.forEach {coords ->
        field.points[coords] = 0
    }
    field.snakes[0]?.body = deque1

    val deque2 = ArrayDeque<model.engine.Coords>(5)
    deque2.addLast(model.engine.Coords(field, 1, 2))
    deque2.addLast(model.engine.Coords(field, 2, 2))
    deque2.addLast(model.engine.Coords(field, 3, 2))
    deque2.addLast(model.engine.Coords(field, 4, 2))
    deque2.addLast(model.engine.Coords(field, 5, 2))
    deque2.forEach {coords ->
        field.points[coords] = 1
    }
    field.snakes[1]?.body = deque2

    field.calculateStep(directions.readAll())
    field.calculateStep(directions.readAll())
    field.calculateStep(directions.readAll())

}