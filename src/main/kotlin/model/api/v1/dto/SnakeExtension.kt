@file:Suppress("RemoveRedundantQualifierName")

package model.api.v1.dto

fun model.engine.Snake.toDto(): model.api.v1.dto.Snake {
    return model.api.v1.dto.Snake(
        direction = direction(),
        playerId = player.id,
        state = model.api.v1.dto.Snake.State.ALIVE, /* Иначе недопустимое состояние.
                                                     После вызова die() змея должна быть утилизирована. */
        points = body.toTypedArray()
    )
}