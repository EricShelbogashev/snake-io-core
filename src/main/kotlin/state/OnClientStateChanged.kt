package state

interface OnClientStateChanged {
    fun handleState(state: State)
}