package model.state

import doc.Contract

/*
    Контракт подразумевает под собой необходимость в возможности восстановить StateHolder после HaltState. Необходимо
    для удобства использования.
*/
@Contract("не должен менять поведение в зависимости от состояния")
data class StateHolder(
    private var state: State = HaltState,
    private val transitionListener: (state: State) -> Unit
) {
    /*
        Состояния после завершения работы обязаны становиться непригодными для переиспользования.
        Ранее пользователь получал доступ к состоянию, поэтому в случае некорректного использования владелец состоянием
        мог вызывать функции, противоречащие логике стейт-машины клиента. Однако, в настоящий момент, запросы
        проксируются через ProtectedController, тем самым нарушить логику работы невозможно и контракт функции
        change(state: State) скорее комильфо, чем необходимость.
    */
    @Contract("state обязаны быть объектами, никогда не передадавшимися в функцию change(state: State)")
    fun change(state: State) {
//        this.state.close()
        this.state = state
        transitionListener(state)
    }

    fun state(): State = state
}