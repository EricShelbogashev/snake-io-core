package model.engine

class Food(
    field: Field,
    coords: Coords
) : api.v1.dto.Food(
    x = coords.x,
    y = coords.y
) {
    private val field: Field
    private var coords: Coords?

    init {
        this.field = field
        this.coords = coords
    }

    fun eat() {
        assert(coords != null) {
            throw IllegalStateException("food is already eaten")
        }
        field.food.remove(coords)
        field.points.remove(coords)
        coords = null
    }

    fun coords(): Coords {
        assert(coords != null) {
            throw IllegalStateException("food is already eaten")
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