package com.hashcore.pool.data.remote.push

/**
 * Firebase Cloud Messaging preparation.
 *
 * To enable push notifications:
 * 1. Add `google-services` plugin and `app/google-services.json`
 * 2. Uncomment Firebase dependencies in app/build.gradle.kts
 * 3. Uncomment the service entry in AndroidManifest.xml
 * 4. Implement [HashCoreFirebaseMessagingService] extending FirebaseMessagingService
 * 5. Send the FCM token to `POST /api/v1/devices` on login
 *
 * Notification types planned:
 * - Worker offline / problem
 * - Payout sent / confirmed
 * - Balance threshold alerts
 */
object FcmPlaceholder {
    const val CHANNEL_ID_ALERTS = "hashcore_alerts"
    const val CHANNEL_ID_PAYOUTS = "hashcore_payouts"
}
