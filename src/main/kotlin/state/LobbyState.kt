package state

import Context
import GameClientPermissionLayer
import api.v1.dto.Announcement
import controller.LobbyController
import model.GameConfig

class LobbyState internal constructor(
    private val context: Context, gameClientPermissionLayer: GameClientPermissionLayer
) : State(context, gameClientPermissionLayer), LobbyController {

    override fun newGame(config: GameConfig) {
        gameClientPermissionLayer.changeState(
            MasterMatchState(context, config, gameClientPermissionLayer)
        )
    }

    override fun joinGame(gameName: String) {
        TODO("low-level sending message")
    }

    override fun watchGame(gameName: String) {
        TODO("Not yet implemented")
    }

    override fun setGameAnnouncementListener(action: (announcement: Announcement) -> Unit) {
        TODO("Not yet implemented")
    }

    override fun removeGameAnnouncementListener() {
        TODO("Not yet implemented")
    }

}