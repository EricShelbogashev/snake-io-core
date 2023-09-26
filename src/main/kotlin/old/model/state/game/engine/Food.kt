package model.state.game.engine

internal class Food(
    field: Field,
    coords: Coords
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
}