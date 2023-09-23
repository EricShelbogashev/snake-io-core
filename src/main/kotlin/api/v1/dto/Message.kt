package api.v1.dto

import java.net.InetSocketAddress

open class Message(
    open val address: InetSocketAddress,
    open val senderId: Int,
    open var msgSeq: Long = -1
)