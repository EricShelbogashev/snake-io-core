package model

data class GameConfig(
    val gameName: String,
    val playerName: String,
    val width: Int = width(),
    val height: Int = height(),
    val foodStatic: Int = foodStatic(),
    val stateDelayMs: Int = stateDelayMs(),
    val masterPlayerId: Int = masterPlayerId(),
    val initialMatchDelay: Int = initialMatchDelay()
) {
    companion object Defaults {
        fun width() = 40
        fun height() = 30
        fun foodStatic() = 1
        fun stateDelayMs() = 1000
        fun masterIp() = ""
        fun masterPort() = 0
        fun masterId() = 0
        fun masterPlayerId() = 0
        fun initialMatchDelay() = 1000
    }
}
