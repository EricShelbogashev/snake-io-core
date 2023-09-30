package model.api.v1.dto

import java.net.InetSocketAddress

class RoleChange(
    address: InetSocketAddress,
    senderId: Int,
    receiverId: Int,
    val senderRole: NodeRole?,
    val receiverRole: NodeRole?,
) : Message(
    address = address,
    senderId = senderId,
    receiverId = receiverId
) {
    init {
        if (senderRole == null && receiverRole == null) {
            throw IllegalArgumentException("at least one field from [senderRole, receiverRole] must be non-null")
        }
    }

    override fun toString(): String {
        return "RoleChange(address=$address, senderId=$senderId, senderRole=$senderRole, receiverRole=$receiverRole, receiverId=$receiverId)"
    }
}