package state

import Context
import GameClientPermissionLayer

class HaltState(
    context: Context,
    gameClientPermissionLayer: GameClientPermissionLayer
) : State(
    context,
    gameClientPermissionLayer
) {
}