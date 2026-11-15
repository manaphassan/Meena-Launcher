package my.hana.meena.data.system

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import my.hana.meena.service.MeenaDeviceAdminReceiver

/**
 * Double tap to lock, through the device admin force-lock policy
 * (https://developer.android.com/reference/android/app/admin/DevicePolicyManager#lockNow()).
 * After lockNow() Android asks for the PIN, pattern or password, not a fingerprint, for the next unlock.
 * While the admin is active Meena can't be uninstalled, so turning the setting off removes it.
 */
object DeviceLock {
    private const val TAG = "DeviceLock"

    private fun manager(context: Context): DevicePolicyManager? =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager

    private fun component(context: Context) = ComponentName(context, MeenaDeviceAdminReceiver::class.java)

    fun isActive(context: Context): Boolean =
        runCatching { manager(context)?.isAdminActive(component(context)) == true }.getOrDefault(false)

    /** Android's own "allow meena to lock the screen?" page. */
    fun requestIntent(context: Context, explanation: String): Intent =
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component(context))
            .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, explanation)

    /** Hands the lock back. Android finishes this a moment later, so read [isActive] again afterwards. */
    fun deactivate(context: Context) {
        if (!isActive(context)) return
        runCatching { manager(context)?.removeActiveAdmin(component(context)) }
            .onFailure { Log.w(TAG, "Couldn't remove device admin", it) }
    }

    /** Turns the screen off and locks; false if Meena isn't allowed to (anymore). */
    fun lock(context: Context): Boolean {
        if (!isActive(context)) return false
        return try {
            manager(context)?.lockNow()
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "lockNow refused", e)
            false
        }
    }
}
