package model.state.game

data class GameConfig(
    val gameName: String,
    val playerName: String,
    val width: Int = 40,
    val height: Int = 30,
    val foodStatic: Int = 1,
    val stateDelayMs: Int = 1000
) {
    companion object Defaults {
        fun masterIp() = ""
        fun masterPort() = 0
        fun masterId() = 0
    }
}
