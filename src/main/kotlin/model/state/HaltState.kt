package model.state

import doc.Contract

@Contract("не должен иметь Controller")
@Contract("не должен иметь наследников")
object HaltState : State {
    override fun close() {}
}