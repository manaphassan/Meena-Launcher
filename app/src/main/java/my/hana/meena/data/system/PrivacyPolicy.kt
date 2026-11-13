package my.hana.meena.data.system

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Meena's public privacy policy: the same page linked in Play Console, shown from settings and
 * from Health Connect's "privacy policy" link. It lives in the public repo on the main branch.
 */
object PrivacyPolicy {
    const val URL = "https://github.com/manaphassan/Meena-Launcher/blob/main/docs/privacy-policy.md"

    /** Health Connect's privacy policy link (up to Android 13, and the Android 14+ alias). */
    val RATIONALE_ACTIONS = setOf(
        "androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE",
        "android.intent.action.VIEW_PERMISSION_USAGE"
    )

    /** Opens the policy in the browser; false when no app can show a web page. */
    fun open(context: Context): Boolean = try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: Exception) {
        false
    }
}
