package model.error;

import doc.Contract

@Contract("выбрасывать только в случае, когда дальнейшая работа всего клиента невозможна")
class CriticalException(message: String) : RuntimeException(message)
