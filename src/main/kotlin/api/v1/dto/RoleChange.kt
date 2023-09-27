package api.v1.dto

import java.net.InetSocketAddress

class RoleChange(
    override var address: InetSocketAddress,
    override var senderId: Int,
    val senderRole: NodeRole?,
    val receiverRole: NodeRole?,
    override val receiverId: Int,
) : Ack(address, senderId, receiverId) {
    init {
        if (senderRole == null && receiverRole == null) {
            throw IllegalArgumentException("at least one field from [senderRole, receiverRole] must be non-null")
        }
    }

    override fun toString(): String {
        return "RoleChange(address=$address, senderId=$senderId, senderRole=$senderRole, receiverRole=$receiverRole, receiverId=$receiverId)"
    }
}