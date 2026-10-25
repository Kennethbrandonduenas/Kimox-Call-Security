package com.kimoxcallsecurity

import android.content.Context
import android.telephony.TelephonyManager
import java.util.Locale

data class DeviceCountry(val iso: String, val name: String, val dialingCode: String, val source: String)

object DeviceCountryDetector {
    fun detect(context: Context): DeviceCountry {
        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val candidates = listOf(
            telephony.simCountryIso to "SIM",
            telephony.networkCountryIso to "RED_MOVIL",
            context.resources.configuration.locales[0]?.country to "REGION_ANDROID",
        )
        val detected = candidates.firstNotNullOfOrNull { (value, source) ->
            value?.trim()?.uppercase(Locale.US)?.takeIf { it.length == 2 }?.let { it to source }
        } ?: ("PE" to "PREDETERMINADO")
        val iso = detected.first
        return DeviceCountry(
            iso,
            Locale("es", iso).displayCountry.replaceFirstChar { it.uppercase() },
            DIALING_CODES[iso].orEmpty(),
            detected.second,
        )
    }

    private val DIALING_CODES = mapOf(
        "AR" to "54", "BO" to "591", "BR" to "55", "CL" to "56", "CO" to "57",
        "CR" to "506", "CU" to "53", "DO" to "1", "EC" to "593", "SV" to "503",
        "GT" to "502", "HN" to "504", "MX" to "52", "NI" to "505", "PA" to "507",
        "PY" to "595", "PE" to "51", "PR" to "1", "UY" to "598", "US" to "1",
        "VE" to "58", "CA" to "1", "ES" to "34", "FR" to "33", "DE" to "49",
        "IT" to "39", "PT" to "351", "GB" to "44", "IE" to "353", "NL" to "31",
        "BE" to "32", "CH" to "41", "AT" to "43", "SE" to "46", "NO" to "47",
        "DK" to "45", "FI" to "358", "PL" to "48", "RO" to "40", "UA" to "380",
        "RU" to "7", "TR" to "90", "CN" to "86", "JP" to "81", "KR" to "82",
        "IN" to "91", "ID" to "62", "PH" to "63", "TH" to "66", "VN" to "84",
        "MY" to "60", "SG" to "65", "AU" to "61", "NZ" to "64", "ZA" to "27",
        "EG" to "20", "MA" to "212", "NG" to "234", "GH" to "233", "KE" to "254",
        "IL" to "972", "SA" to "966", "AE" to "971", "PK" to "92", "BD" to "880",
    )
}
