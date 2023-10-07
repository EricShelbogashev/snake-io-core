package model.error

import doc.Contract

/**
 * Класс исключения для недопустимых аргументов.
 *
 * @param violation Описание нарушения.
 */
class IllegalArgumentException : RuntimeException {
    constructor(violation: String) : super(violation)

    constructor(violations: Array<String>) : super(combineViolations(violations))

    companion object {
        /**
         * Преобразует массив нарушений в одну строку с разделителями.
         *
         * @param violations Массив нарушений.
         * @return Строка с нарушениями, разделенными символами новой строки.
         */
        @Contract("вызывать только из конструктора")
        private fun combineViolations(violations: Array<String>): String {
            if (violations.isEmpty()) {
                return ""
            }

            val stringBuilder = StringBuilder()
            violations.forEach { violation ->
                stringBuilder.append(violation)
                stringBuilder.append("\n")
            }
            return stringBuilder.toString()
        }
    }
}
