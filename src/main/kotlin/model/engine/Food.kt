package model.engine

class Food(
    private val field: Field,
    private var coords: Coords?
) : model.api.v1.dto.Food(coords?.x ?: 0, coords?.y ?: 0) {

    init {
        coords?.let { initializeFood(it) }
    }

    private fun initializeFood(coords: Coords) {
        if (!field.points.containsKey(coords)) {
            field.points[coords] = -1
            field.food[coords] = this
        }
    }

    fun eat() {
        assert(coords != null) {
            throw IllegalStateException("Food is already eaten")
        }
        field.food.remove(coords)
        field.points.remove(coords)
        coords = null
    }

    fun getCoords(): Coords {
        assert(coords != null) {
            throw IllegalStateException("Food is already eaten")
        }
        return coords!!
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
