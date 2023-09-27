package state

import Context
import GameClientPermissionLayer

// all inherited states that use state switching must call only the changeState method
abstract class State internal constructor(context: Context, protected val gameClientPermissionLayer: GameClientPermissionLayer) {
}