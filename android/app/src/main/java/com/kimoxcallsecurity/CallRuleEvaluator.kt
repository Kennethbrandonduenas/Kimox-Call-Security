package com.kimoxcallsecurity

data class CallRuleInput(
    val number: String?,
    val isPrivate: Boolean,
    val isKnownContact: Boolean?,
    val whitelist: Set<String>,
    val blockedNumbers: Set<String>,
    val blockedPrefixes: List<String>,
    val blockedCountries: List<String>,
    val blockPrivate: Boolean,
    val blockUnknown: Boolean,
    val blockInternational: Boolean,
    val allowInternationalContacts: Boolean,
    val isForeignInternational: Boolean,
)

object CallRuleEvaluator {
    fun evaluate(input: CallRuleInput): String? {
        val number = input.number
        if (number != null && number in input.whitelist) return null
        return when {
            input.isPrivate && input.blockPrivate -> "Número privado"
            number != null && number in input.blockedNumbers -> "Número bloqueado"
            number != null && input.blockedPrefixes.any(number::startsWith) -> "Prefijo bloqueado"
            number != null && input.blockedCountries.any(number::startsWith) -> "País bloqueado"
            input.blockInternational && input.isForeignInternational &&
                !(input.allowInternationalContacts && input.isKnownContact == true) -> "Llamada internacional"
            number != null && input.blockUnknown && input.isKnownContact == false -> "Número desconocido"
            else -> null
        }
    }
}
