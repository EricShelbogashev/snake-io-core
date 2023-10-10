package model.engine

class Food(
    private val field: Field,
    private var coords: Coords
) : model.api.v1.dto.Food(coords.x, coords.y) {
    private var isEeaten = false
    init {
        initializeFood(coords)
    }

    private fun initializeFood(coords: Coords) {
        if (field.points.containsKey(coords)) {
            throw IllegalArgumentException("выбранная точка на поле уже занята. Невозможно разместить еду")
        }
        field.points[coords] = -1
        field.food[coords] = this
    }

    fun eat() {
        assert(isEeaten) {
            throw IllegalStateException("Food is already eaten")
        }
        field.food.remove(coords)
        field.points.remove(coords)
        isEeaten = true
    }

    fun getCoords(): Coords {
        assert(isEeaten) {
            throw IllegalStateException("Food is already eaten")
        }
        return coords
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        other as Food
        return coords == other.coords
    }

    override fun hashCode(): Int {
        return coords.hashCode()
    }

    override fun toString(): String {
        return "Food(coords=$coords)"
    }
}
