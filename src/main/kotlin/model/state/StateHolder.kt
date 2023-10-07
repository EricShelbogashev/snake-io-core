package model.state

import doc.Contract

/**
 * Класс, отвечающий за управление различными состояниями в приложении.
 *
 * Контракт подразумевает под собой необходимость в возможности восстановить StateHolder после HaltState. Необходимо
 * для удобства использования.
 *
 * @param transitionListener Функция обратного вызова для обработки переходов между состояниями.
 */
@Contract("Не должен менять поведение в зависимости от состояния.")
data class StateHolder(
    private var state: State = HaltState,
    private val transitionListener: (state: State) -> Unit
) {
    /**
     * Состояния после завершения работы обязаны становиться непригодными для переиспользования.
     * Ранее пользователь получал доступ к состоянию, поэтому в случае некорректного использования владелец состоянием
     * мог вызывать функции, противоречащие логике стейт-машины клиента. Однако, в настоящий момент, запросы
     * проксируются через ProtectedController, тем самым нарушить логику работы невозможно и контракт функции
     * change(state: State) скорее комильфо, чем необходимость.
     *
     * @param state Новое состояние для перехода.
     */
    @Contract("Параметр 'state' должен быть объектом, который ранее не передавался в 'change(state: State)'.")
    fun change(state: State) {
        this.state.close()
        this.state = state
        this.state.initialize()
        transitionListener(state)
    }

    /**
     * Получает текущее состояние.
     *
     * @return Текущее состояние.
     */
    fun state(): State = state
}
