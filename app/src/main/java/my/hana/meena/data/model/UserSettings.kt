package my.hana.meena.data.model

import kotlinx.serialization.Serializable

@Serializable
data class WidgetConfig(
    val hubId: String,
    val position: Int,
    val isCollapsed: Boolean = false,
    val maxLinesOrItems: Int = 5,
    val refreshMinutes: Int = 15,
    val isEnabled: Boolean = true,
    val customSettings: Map<String, String> = emptyMap()
)

@Serializable
data class MeenaUserSettings(
    val language: String = "en",
    val isStatusBarHidden: Boolean = false,
    val isLeftHanded: Boolean = false,
    val isReducedMotion: Boolean = false,
    val defaultHubIndex: Int = 0,
    val isHapticEnabled: Boolean = true,
    val baseTheme: String = "dark", // "dark", "light"
    val accentHex: String = "#00A4EF", // Meena Cyan default
    
    val fontScalePercent: Int = 100,
    
    val wallpaperUri: String? = null,
    val wallpaperDimPercent: Int = 40,
    
    val isMonochromeFavourites: Boolean = true,
    val iconPack: String = "", // icon pack app id; empty = system icons
    /** Favourite tiles flip to show counts, the next event or alarm. */
    val liveTiles: Boolean = true,
    /** Live tiles may show who a chat is from (never the message itself). */
    val liveTileNames: Boolean = true,
    /** Title at the top of the home pages; empty hides it. See [HomeTitle]. */
    val homeTitle: String = HomeTitle.DEFAULT,
    val pinnedAppIds: List<String> = emptyList(),
    /** How the apps page is laid out: see [AppsSort]. */
    val appsSortMode: String = AppsSort.AZ,
    val solatZone: String = "",
    val newsFeeds: List<String> = emptyList(),
    /** Feeds marked nsfw in news settings; their pictures are blurred. */
    val nsfwFeeds: List<String> = emptyList(),
    val widgetConfigs: Map<String, WidgetConfig> = defaultWidgetMap(),
    // Calendar settings
    val calendarDaysAhead: Int = 7,
    val calendarShowAllDay: Boolean = true,
    // Date & Time settings
    val is24HourFormat: Boolean = true,
    val dateFormatPattern: String = DEFAULT_DATE_PATTERN,
    val showClockSeconds: Boolean = false,
    val showHijriDate: Boolean = true,
    val hijriDayOffset: Int = 0,
    // Finance privacy
    val showFinanceAmounts: Boolean = true,
    // Health Connect & Companion settings
    val stepGoal: Int = 10000,
    val sleepHoursGoal: Float = 8.0f,
    val healthCompanionPackage: String = "",
    val healthCompanionName: String = "",
    // First-run permission onboarding
    val hasCompletedFirstRunSetup: Boolean = false,
    /** Bumped when a one-time Today layout change is applied, so it never runs twice. */
    val layoutRevision: Int = 0
) {
    /** Paper is for e-ink screens, where animation smears, so it always moves calmly. */
    val reducedMotion: Boolean get() = isReducedMotion || baseTheme == "paper"

    fun migrate(): MeenaUserSettings {
        val updatedConfigs = widgetConfigs.toMutableMap()
        var changed = false

        // Ensure all default widgets exist
        defaultWidgetMap().forEach { (key, defaultConfig) ->
            if (!updatedConfigs.containsKey(key)) {
                updatedConfigs[key] = defaultConfig
                changed = true
            }
        }

        // Drop widgets that no longer exist
        if (updatedConfigs.keys.retainAll(defaultWidgetMap().keys)) {
            changed = true
        }

        // Migrate legacy hubIds
        updatedConfigs.forEach { (key, config) ->
            val targetHubId = when {
                key == "widget-conversations" || key == "widget-notifications" -> {
                    if (config.hubId == "start" || config.hubId == "today") "inbox" else config.hubId
                }
                config.hubId == "start" || config.hubId == "agenda" || config.hubId == "health" -> "today"
                config.hubId == "finance" -> "money"
                else -> config.hubId
            }
            if (targetHubId != config.hubId) {
                updatedConfigs[key] = config.copy(hubId = targetHubId)
                changed = true
            }
        }

        val clampedDefaultHubIndex = defaultHubIndex.coerceIn(0, 4)
        if (clampedDefaultHubIndex != defaultHubIndex) {
            changed = true
        }

        // Malaysian English: the old default "friday, november 13" becomes "friday, 13 november".
        val datePattern = if (dateFormatPattern == OLD_DEFAULT_DATE_PATTERN) DEFAULT_DATE_PATTERN else dateFormatPattern
        if (datePattern != dateFormatPattern) {
            changed = true
        }

        val feeds = newsFeeds.map { if (it in LEGACY_AMANZ_FEEDS) AMANZ_FEED else it }.distinct()
        val nsfw = nsfwFeeds.map { if (it in LEGACY_AMANZ_FEEDS) AMANZ_FEED else it }.distinct()
        if (feeds != newsFeeds || nsfw != nsfwFeeds) {
            changed = true
        }

        // Revision 1 (Nov 2026): favourites sit right under the clock, so an app is one tap away
        // with no scroll. Moved once; any later order the user picks is kept.
        var revision = layoutRevision
        if (revision < FAVOURITES_FIRST_REVISION) {
            moveFavouritesUnderClock(updatedConfigs)
            revision = FAVOURITES_FIRST_REVISION
            changed = true
        }

        return if (changed) {
            copy(
                widgetConfigs = updatedConfigs,
                defaultHubIndex = clampedDefaultHubIndex,
                dateFormatPattern = datePattern,
                newsFeeds = feeds,
                nsfwFeeds = nsfw,
                layoutRevision = revision
            )
        } else {
            this
        }
    }

    private fun moveFavouritesUnderClock(configs: MutableMap<String, WidgetConfig>) {
        val favourites = configs[FAVOURITES_ID] ?: return
        val clock = configs[CLOCK_ID] ?: return
        if (favourites.hubId != clock.hubId) return
        val order = configs.filterValues { it.hubId == clock.hubId }
            .entries.sortedBy { it.value.position }.map { it.key }
            .filter { it != FAVOURITES_ID }.toMutableList()
        order.add(order.indexOf(CLOCK_ID) + 1, FAVOURITES_ID)
        order.forEachIndexed { index, id -> configs[id] = configs.getValue(id).copy(position = index) }
    }

    /**
     * Puts every widget back in the story order of [defaultWidgetMap], keeping which ones are on,
     * which page each is on and its own settings. Only runs when the user asks for it.
     */
    fun withStoryOrder(): MeenaUserSettings {
        val defaults = defaultWidgetMap()
        return copy(widgetConfigs = widgetConfigs.mapValues { (id, config) ->
            defaults[id]?.let { config.copy(position = it.position) } ?: config
        })
    }

    companion object {
        const val FAVOURITES_FIRST_REVISION = 1
        private const val FAVOURITES_ID = "widget-favourites"
        private const val CLOCK_ID = "widget-clock-weather"
        /** The US-style date the app used to default to; moved to Malaysian English (day before month). */
        const val OLD_DEFAULT_DATE_PATTERN = "EEEE, MMMM d"
        const val DEFAULT_DATE_PATTERN = "EEEE, d MMMM"
        /** Must match NewsRssRepository.AMANZ_FEED_URL. Amanz's own feed (amanz.my/feed/ redirects here). */
        const val AMANZ_FEED = "https://cms.amanz.my/feed/"
        /** Google News' copy of Amanz, used while the real feed showed a bot check; now only a fallback. */
        const val AMANZ_GOOGLE_NEWS_FEED = "https://news.google.com/rss/search?q=site:amanz.my&hl=ms-MY&gl=MY&ceid=MY:ms"
        /**
         * Saved Amanz addresses that are moved to [AMANZ_FEED]: the home page (not a feed) and the Google
         * News copy. Real feed addresses the user typed, such as amanz.my/feed, are left as they are.
         */
        val LEGACY_AMANZ_FEEDS = setOf(
            "https://amanz.my", "https://amanz.my/", "https://www.amanz.my", "https://www.amanz.my/",
            AMANZ_GOOGLE_NEWS_FEED
        )

        /**
         * Today reads as a story, top to bottom: now (time, weather, what's next), your apps, the
         * sky for the next few days, your week, prayer times, then the optional extras:
         * notes, month, your body, your gadgets, around you, something to read.
         */
        fun defaultWidgetMap(): Map<String, WidgetConfig> = mapOf(
            "widget-clock-weather" to WidgetConfig("today", 0, isEnabled = true),
            "widget-favourites" to WidgetConfig("today", 1, maxLinesOrItems = 8, isEnabled = true),
            "widget-weather-forecast" to WidgetConfig("today", 2, isEnabled = true),
            "widget-tasks" to WidgetConfig("today", 3, isEnabled = true),
            "widget-solat" to WidgetConfig("today", 4, isEnabled = true),
            "widget-frequent-apps" to WidgetConfig("today", 5, maxLinesOrItems = 8, isEnabled = false),
            "widget-notes" to WidgetConfig("today", 6, isEnabled = false),
            "widget-calendar-month" to WidgetConfig("today", 7, isEnabled = false),
            "widget-health-telemetry" to WidgetConfig("today", 8, isEnabled = false),
            "widget-health-device" to WidgetConfig("today", 9, isEnabled = false),
            "widget-bluetooth-devices" to WidgetConfig("today", 10, isEnabled = false),
            "widget-device-telemetry" to WidgetConfig("today", 11, isEnabled = false),
            "widget-map-radar" to WidgetConfig("today", 12, isEnabled = false),
            "widget-wikipedia" to WidgetConfig("today", 13, isEnabled = false),

            // Inbox Hub (conversations, alerts / notification stream)
            "widget-conversations" to WidgetConfig("inbox", 0, isEnabled = true),
            "widget-notifications" to WidgetConfig("inbox", 1, isEnabled = true),

            // News Hub
            "widget-news-feed" to WidgetConfig("news", 0, isEnabled = true),

            // Money Hub (portfolio & watchlist, exchange rates, spending)
            "widget-finance-charts" to WidgetConfig("money", 0, isEnabled = true),
            "widget-exchange-rates" to WidgetConfig("money", 1, isEnabled = true),
            "widget-spending-summary" to WidgetConfig("money", 2, isEnabled = true)
        )
    }
}

