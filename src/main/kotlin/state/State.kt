package state

import Context

// all inherited states that use state switching must call only the changeState method
abstract class State internal constructor(context: Context)