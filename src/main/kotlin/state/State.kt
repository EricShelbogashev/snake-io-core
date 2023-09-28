package state

import Context
import api.v1.controller.GameNetController
import java.net.NetworkInterface

// all inherited states that use state switching must call only the changeState method
abstract class State internal constructor(
    val context: Context
)