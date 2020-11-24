package dev.yekta.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.media.app.NotificationCompat.MediaStyle
import dev.yekta.notify.Notification.Style.*

/**
 * This class creates a notification; which you can show, update, and delete.
 *
 * @property notificationId A unique [Int] for each notification that you must define
 * @property context Current context.
 * @property channelId The notification channel ID.
 * @property smallIcon The notification icon.
 * @property title The notification title.
 * @property text The notification body text.
 * @property clickIntent The intent that will fire when the user taps the notification.
 * @property bigText By default, the notification's text content is truncated to fit one line.
 * If you want your notification to be longer, you can enable an expandable notification by adding [bigText].
 * @property bigPicture Add an image in the notification.
 * @property bigPictureThumbnail Make the [bigPicture] appear as a thumbnail only while the notification is collapsed.
 * @property lines Multiple short summary lines, such as snippets from incoming emails. Note: Maximum lines size is 6.
 * If you add more than 6 lines, only the first 6 are visible.
 * @property messages Sequential messages between any number of people.
 * This is ideal for messaging apps because it provides a consistent layout for each message
 * by handling the sender name and message text separately, and each message can be multiple lines long.
 * Note: When using [MESSAGING], any values given to [title] and [text] are ignored.
 * Note: Only works on Android 7.0 (API level 24) and up.
 * @property user This [Person]'s name will be shown when this app's notification is being replied to.
 * It's used temporarily so the app has time to process the send request and repost the notification with updates to the conversation.
 * Note: [Person] must have a non-empty name.
 * @property conversationTitle Add a title that appears above the conversation.
 * This might be the user-created name of the group or, if it doesn't have a specific name,
 * a list of the participants in the conversation. Do not set a conversation title for one-on-one chats,
 * because the system uses the existence of this field as a hint that the conversation is a group.
 * @property priority Determine how intrusive the notification should be on Android 7.1 and lower.
 * @property autoCancel Automatically remove the notification when the user taps it.
 * @property category If your notification falls into one of the pre-defined notification categories defined in NotificationCompat
 * you should declare it as such by setting [category].
 */
class Notification(
    // MUST HAVES
    private val context: Context,
    private val notificationId: Int,
    private val channelId: String,
    private val smallIcon: Int
) {
    enum class Style { BIG_TEXT, BIG_PICTURE, INBOX, MESSAGING, MEDIA }

    private val builder =
        NotificationCompat
            // SET THE MUST HAVES
            .Builder(context, channelId)
            .setSmallIcon(smallIcon)

    // COMMON PROPERTIES (SIMPLE)
    var title: String? = null
        set(value) {
            field = value
            if (field != null)
                builder.setContentTitle(field)
        }

    var text: String? = null

    var clickIntent: PendingIntent? = null
        set(value) {
            field = value
            if (field != null)
                builder.setContentIntent(field)
        }

    var actionButtons: List<ActionButton>? = null
        set(value) {
            field = value
            if (field != null)
                for (btn in field!!)
                    builder.addAction(btn.action)
        }

    var largeIcon: Bitmap? = null
        set(value) {
            field = value
            if (field != null)
                builder.setLargeIcon(field)
        }

    // EXPANDABLE TEXT STYLE (BIG_TEXT)
    var bigText: String? = null

    // EXPANDABLE PICTURE STYLE (BIG_PICTURE)
    var bigPicture: Bitmap? = null
    var bigPictureThumbnail: Boolean = true

    // EXPANDABLE INBOX-STYLE (INBOX)
    var lines: List<CharSequence>? = null

    // EXPANDABLE MESSAGING STYLE (MESSAGING)
    var messages: List<NotificationCompat.MessagingStyle.Message>? = null
    var user: Person? = null
    var conversationTitle: String? = null

    // EXPANDABLE MEDIA STYLE (MEDIA)
    var token: MediaSessionCompat.Token? = null

    // OPTIONS
    var visibility: Int? = null
        set(value) {
            field = value
            if (field != null)
                builder.setVisibility(field!!)
        }

    var priority: Int? = null
        set(value) {
            field = value
            if (field != null)
                builder.priority = field!!
        }

    var autoCancel: Boolean? = null
        set(value) {
            field = value
            if (field != null)
                builder.setAutoCancel(field!!)
        }

    var category: String? = null
        set(value) {
            field = value
            if (field != null)
                builder.setCategory(field)
        }

    var onGoing: Boolean? = null
        set(value) {
            field = value
            if (field != null)
                builder.setOngoing(field!!)
        }

    /**
     * Submit the notification style
     *
     * This function must only be called when the notification has a style other than the default.
     * Notice: Call this function right before showing the notification.
     * The properties related to the style won't change if you don't call [submit] after changing them.
     *
     * @param style The notification style.
     */
    fun submit(style: Style): NotificationCompat.Builder {
        when (style) {
            BIG_TEXT -> setBigTextProperties()
            BIG_PICTURE -> setBigPictureProperties()
            INBOX -> setInboxProperties()
            MESSAGING -> setMessagingProperties()
            MEDIA -> setMediaProperties()
        }

        return builder
    }

    /**
     * Set the properties related to [Style.MEDIA]
     */
    private fun setMediaProperties() {
        if (token != null) {
            val ms = MediaStyle()
            val indexes: MutableList<Int> = mutableListOf()

            actionButtons?.forEachIndexed { i, btn ->
                if (btn.showInCompactView)
                    indexes.add(i)
            }

            when {
                indexes.size == 1 -> ms.setShowActionsInCompactView(
                    indexes[0]
                )
                indexes.size == 2 -> ms.setShowActionsInCompactView(
                    indexes[0],
                    indexes[1]
                )
                indexes.size >= 3 -> ms.setShowActionsInCompactView(
                    indexes[0],
                    indexes[1],
                    indexes[2]
                )
            }

            builder
                // Show controls on lock screen even when user hides sensitive content.
                // Default visibility for MEDIA style which can later be overwritten by visibility
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                // Apply the media style template
                .setStyle(ms.setMediaSession(token))
        }
    }

    /**
     * Set the properties related to [Style.MESSAGING]
     */
    private fun setMessagingProperties() {
        if (messages != null && user != null) {
            val messagingStyle = NotificationCompat.MessagingStyle(user!!)

            if (conversationTitle != null)
                messagingStyle.conversationTitle = conversationTitle

            for (message in messages!!) {
                messagingStyle.addMessage(message)
            }

            builder.setStyle(messagingStyle)
        }
    }

    /**
     * Set the properties related to [Style.INBOX]
     */
    private fun setInboxProperties() {
        if (lines != null) {
            val inboxStyle = NotificationCompat.InboxStyle()

            for (line in lines!!)
                inboxStyle.addLine(line)

            builder.setStyle(inboxStyle)
        }
    }

    /**
     * Set the properties related to [Style.BIG_PICTURE]
     */
    private fun setBigPictureProperties() {
        if (bigPicture != null)
            if (bigPictureThumbnail)
                builder
                    .setStyle(
                        NotificationCompat.BigPictureStyle()
                            .bigPicture(bigPicture)
                            .bigLargeIcon(null)
                    )
                    .setLargeIcon(bigPicture)
            else
                builder.setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(bigPicture)
                )
    }

    /**
     * Set the properties related to [Style.BIG_TEXT]
     */
    private fun setBigTextProperties() {
        if (bigText != null)
            builder.setStyle(
                NotificationCompat.BigTextStyle().bigText(bigText)
            )
    }

    //builder.setBadgeIconType(NotificationCompat.BADGE_ICON_LARGE)
    //builder.setLights (Color.Red.ToAndroid (), 300, 300)

    /**
     * Show the notification.
     *
     * If the notification has a [Style]: Use this if you already called [submit].
     *
     * If the notification has no [Style]: Just use the function!
     */
    fun show() = Utils.showNotification(context, notificationId, builder)

    /**
     * Show the notification.
     *
     * Use this if the notification has a [Style] and you didn't called [submit].
     *
     * @param style The notification style.
     */
    fun show(style: Style) {
        submit(style)
        Utils.showNotification(context, notificationId, builder)
    }

    /**
     * Delete the notification
     */
    fun delete() = Utils.deleteNotification(context, notificationId)
}

class NotificationChannel(
    private val channelId: String,
    private val channelName: String,
    private val channelImportance: Int
) {
    var channel: NotificationChannel? = null

    var channelDescription: String? = null
        set(value) {
            field = value
            if (field != null && Build.VERSION.SDK_INT >= 26)
                channel?.description = field
        }

    var showBadge: Boolean? = null
        set(value) {
            field = value
            if (field != null && Build.VERSION.SDK_INT >= 26)
                channel?.setShowBadge(field!!)
        }

    var vibration: Boolean? = null
        set(value) {
            field = value
            if (field != null && Build.VERSION.SDK_INT >= 26)
                channel?.enableVibration(field!!)
        }

    var lights: Boolean? = null
        set(value) {
            field = value
            if (field != null && Build.VERSION.SDK_INT >= 26)
                channel?.enableLights(field!!)
        }

    var lightColor: Int? = null
        set(value) {
            field = value
            if (field != null && Build.VERSION.SDK_INT >= 26)
                channel?.lightColor = field!!
        }

    var lockScreenVisibility: Int? = null
        set(value) {
            field = value
            if (field != null && Build.VERSION.SDK_INT >= 26)
                channel?.lockscreenVisibility = field!!
        }

    init {
        if (Build.VERSION.SDK_INT >= 26)
            channel = NotificationChannel(channelId, channelName, channelImportance)
    }

    /**
     * Safely create the NotificationChannel, but only on **API 26+** because
     * the NotificationChannel class is new and not in the support library
     *
     * Because you must create the notification channel before posting any notifications on Android 8.0 and higher,
     * you should execute this code as soon as your app starts.
     * It's safe to call this repeatedly because creating an existing notification channel performs no operation.
     */
    fun create(context: Context) {
        if (channel != null)
            Utils.createChannel(context, channel!!)
    }

    /**
     * Delete the NotificationChannel
     */
    fun delete(context: Context) = Utils.deleteChannel(context, channelId)
}

object Utils {
    /**
     * Show a Notification
     */
    fun showNotification(context: Context, notificationId: Int, builder: NotificationCompat.Builder) =
        NotificationManagerCompat.from(context).notify(notificationId, builder.build())

    /**
     * Delete a Notification
     */
    fun deleteNotification(context: Context, notificationId: Int) = NotificationManagerCompat.from(context).cancel(notificationId)

    /**
     * Create a NotificationChannel
     */
    fun createChannel(context: Context, notificationChannel: NotificationChannel) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Register the channel with the system
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            nm.createNotificationChannel(notificationChannel)
        }
    }

    /**
     * Delete a NotificationChannel
     */
    fun deleteChannel(context: Context, channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.deleteNotificationChannel(channelId)
        }
    }
}

data class ActionButton(
    var action: NotificationCompat.Action,
    var showInCompactView: Boolean = false
) {
    constructor(
        icon: Int,
        title: CharSequence,
        intent: PendingIntent,
        showInCompactView: Boolean = false
    )
            : this(NotificationCompat.Action(icon, title, intent), showInCompactView)
}