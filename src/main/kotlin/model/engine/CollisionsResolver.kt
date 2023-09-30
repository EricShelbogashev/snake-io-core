package model.engine;

/**
 * Соглашение: "завершение обхода" -> "завершение рассчета перемещения змей на новом шаге"
 * Может быть несколько коллизий
 * 1. Несколько змей попадают головами в одну клетку:
 * 1.1. Первая шагнувшая змея попадает в клетку с едой или пустую, шаг проходит успешно;
 * 1.2. Последующие змеи попадают в клетку с головой змеи
 * 1.2.1. Если голова первой змеи на момент завершения обхода изменилась, значит "врезались в нее",
 *        следовательно змеи с шага 1.2 должны погибнуть;
 * 1.2.2. Если голова первой змеи не изменилась, значит погибают все змеи, попавшие в клетку.
 * 2. Змея попадает в клетку, где был хвост:
 * 2.1. Если на момент завершения обхода змея
 * */
class CollisionsResolver(
    private var field: Field
) {
    private var collisions: MutableMap<Coords, Snake> = mutableMapOf()

    fun report(snake: Snake, requestedPoint: Coords) {
        collisions[requestedPoint] = snake
    }

    fun resolveAll() {
        while (true) {
            val iterator = collisions.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val resolved = resolve(entry.value, entry.key)
                if (!resolved) continue

                iterator.remove()
            }
            if (collisions.isEmpty()) {
                break
            }
        }
        refresh()
    }

    private fun resolve(snake: Snake, newHead: Coords): Boolean {
        TODO("разобрать все случи коллизий")
    }

    private fun refresh() {
        collisions.clear()
    }
}