package my.hana.meena.data.system

/**
 * Picks a JAKIM prayer zone from the phone's own address lookup (state, district, town), with no
 * network call. Zones and their districts are from JAKIM e-Solat (https://www.e-solat.gov.my/).
 * Returns null when the place can't be placed in one zone, so the widget asks instead of guessing.
 */
object SolatZoneMatcher {

    /** State prefix of the zone codes, matched against the address lookup's state name. */
    private val STATE_PREFIX = listOf(
        "labuan" to "WLY02",
        "kuala lumpur" to "WLY01",
        "putrajaya" to "WLY01",
        "selangor" to "SGR",
        "johor" to "JHR",
        "kedah" to "KDH",
        "kelantan" to "KTN",
        "melaka" to "MLK",
        "malacca" to "MLK",
        "negeri sembilan" to "NGS",
        "pahang" to "PHG",
        "perlis" to "PLS",
        "pulau pinang" to "PNG",
        "penang" to "PNG",
        "perak" to "PRK",
        "sabah" to "SBH",
        "sarawak" to "SWK",
        "terengganu" to "TRG"
    )

    /** Words in zone and address names that don't help tell places apart. */
    private val FILLER = setOf("daerah", "mukim", "bahagian", "jajahan", "kecil", "bandar", "timur", "barat", "atas", "bawah", "zon", "khas", "dan")

    fun zoneFor(state: String?, places: List<String?>, zones: List<SolatZone> = JakimSolatRepository.AVAILABLE_ZONES): String? {
        val stateName = normalise(state ?: return null)
        val prefix = STATE_PREFIX.firstOrNull { (name, _) -> stateName.contains(name) }?.second ?: return null
        if (prefix.length == 5) return prefix // a federal territory is one zone

        val stateZones = zones.filter { it.code.startsWith(prefix) }
        if (stateZones.size == 1) return stateZones.first().code

        val candidates = places.mapNotNull { it?.let(::normalise)?.takeIf(String::isNotBlank) }
        if (candidates.isEmpty()) return null
        // The longest district name found in any address part wins, so "kuala langat" beats "langat".
        return stateZones
            .flatMap { zone -> districts(zone).map { zone.code to it } }
            .filter { (_, district) -> candidates.any { containsWords(it, district) } }
            .maxByOrNull { (_, district) -> district.length }
            ?.first
    }

    private fun districts(zone: SolatZone): List<String> =
        zone.name.replace("(", ",").replace(")", ",").split(",")
            .map(::normalise)
            .filter { it.isNotBlank() }

    private fun normalise(text: String): String =
        text.lowercase()
            .replace("sg.", "sungai ")
            .replace("kg ", "kampung ")
            .replace(Regex("[^a-z ]"), " ")
            .split(" ")
            .filter { it.isNotBlank() && it !in FILLER }
            .joinToString(" ")

    private fun containsWords(haystack: String, needle: String): Boolean =
        " $haystack ".contains(" $needle ")
}
