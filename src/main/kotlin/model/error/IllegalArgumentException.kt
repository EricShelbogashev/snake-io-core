package model.error;

import doc.Contract

class IllegalArgumentException : RuntimeException {
    constructor(violation: String) : super(message = violation)

    constructor(violations: Array<String>) : super(message = v2m(violations))

    companion object {
        @Contract("вызывать только из конструктора")
        fun v2m(violations: Array<String>): String {
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
