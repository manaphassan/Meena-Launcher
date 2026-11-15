package my.hana.meena.service

import android.app.admin.DeviceAdminReceiver

/**
 * Lets Meena lock the screen for double tap to lock. It declares only the force-lock policy
 * (res/xml/device_admin.xml), which Android still allows for ordinary apps
 * (https://developers.google.com/android/work/device-admin-deprecation).
 * Don't rename or move this class: Android forgets the admin when its receiver name changes.
 */
class MeenaDeviceAdminReceiver : DeviceAdminReceiver()
