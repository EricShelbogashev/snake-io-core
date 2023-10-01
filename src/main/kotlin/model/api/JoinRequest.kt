package model.api

import model.api.v1.dto.Join
import model.api.v1.dto.Player

class JoinRequest(
    val join: Join,
    private val acceptAction: (player: Player) -> Unit,
    private val declineAction: (Join) -> Unit
) {
    fun accept(player: Player) {
        acceptAction(player)
    }

    fun decline() {
        declineAction(join)
    }
}