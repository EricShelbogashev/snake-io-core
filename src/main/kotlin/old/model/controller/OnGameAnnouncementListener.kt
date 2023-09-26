package model.controller

import me.ippolitov.fit.snakes.SnakesProto

interface OnGameAnnouncementListener {
    fun receiveAnnouncement(announcement: SnakesProto.GameMessage.AnnouncementMsg)
}