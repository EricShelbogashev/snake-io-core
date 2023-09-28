package state

import Context
import api.v1.dto.Announcement
import controller.LobbyController
import model.GameConfig

class LobbyState internal constructor(context: Context) : State(context), LobbyController {
    private var gameAnnouncementListener: ((announcement: Announcement) -> Unit)? = null

    init {
        context.gameNetController.setOnAnnouncementHandler { announcement ->
            gameAnnouncementListener?.invoke(
                announcement
            )
        }
    }

    override fun newGame(playerName: String, gameName: String, config: GameConfig) {
        this.gameAnnouncementListener = null
        context.gameClientPermissionLayer.changeState(
            MasterMatchState(context, playerName, gameName, config)
        )
    }

    override fun joinGame(playerName: String, gameName: String) {
        TODO("Not yet implemented")
    }

    override fun watchGame(playerName: String, gameName: String) {
        TODO("Not yet implemented")
    }

    override fun setGameAnnouncementListener(action: (announcement: Announcement) -> Unit) {
        gameAnnouncementListener = action
    }

    override fun removeGameAnnouncementListener() {
        TODO("Not yet implemented")
    }

}