package com.h8xC0d8x.itjhin.dfu

import no.nordicsemi.android.dfu.DfuBaseService
import android.app.Activity



class DfuService : DfuBaseService() {

    companion object{
        /**
         *  Adding companion object because kotlin can't procure static variable from DfuBaseService.java
         */
        const val EXTRA_DEVICE_NAME = DfuBaseService.EXTRA_DEVICE_NAME
        const val EXTRA_FILE_PATH = DfuBaseService.EXTRA_FILE_PATH
        const val EXTRA_INIT_FILE_PATH = DfuBaseService.EXTRA_INIT_FILE_PATH
        const val EXTRA_FILE_TYPE = DfuBaseService.EXTRA_FILE_TYPE
        // fileType
        const val TYPE_AUTO = DfuBaseService.TYPE_AUTO
        const val TYPE_BOOTLOADER = DfuBaseService.TYPE_BOOTLOADER
        const val TYPE_APPLICATION = DfuBaseService.TYPE_APPLICATION
        const val TYPE_SOFT_DEVICE = DfuBaseService.TYPE_SOFT_DEVICE

        const val EXTRA_KEEP_BOND = DfuBaseService.EXTRA_KEEP_BOND
        const val EXTRA_DEVICE_ADDRESS = DfuBaseService.EXTRA_DEVICE_ADDRESS
        const val EXTRA_UNSAFE_EXPERIMENTAL_BUTTONLESS_DFU = DfuBaseService.EXTRA_UNSAFE_EXPERIMENTAL_BUTTONLESS_DFU
        const val BROADCAST_ACTION = DfuBaseService.BROADCAST_ACTION
        const val EXTRA_ACTION = DfuBaseService.EXTRA_ACTION
        const val ACTION_RESUME = DfuBaseService.ACTION_RESUME
        const val ACTION_ABORT = DfuBaseService.ACTION_ABORT
        const val NOTIFICATION_ID = DfuBaseService.NOTIFICATION_ID

        const val MIME_TYPE_OCTET_STREAM = DfuBaseService.MIME_TYPE_OCTET_STREAM
        const val MIME_TYPE_ZIP = DfuBaseService.MIME_TYPE_ZIP
    }

    override fun getNotificationTarget(): Class<out Activity>? {
        /*
                 * As a target activity the NotificationActivity is returned, not the MainActivity. This is because the notification must create a new task:
                 *
                 * intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                 *
                 * when user press it. Using NotificationActivity we can check whether the new activity is a root activity (that means no other activity was open before)
                 * or that there is other activity already open. In the later case the notificationActivity will just be closed. System will restore the previous activity.
                 * However if the application has been closed during upload and user click the notification a NotificationActivity will be launched as a root activity.
                 * It will create and start the main activity and terminate itself.
                 *
                 * This method may be used to restore the target activity in case the application was closed or is open. It may also be used to recreate an activity
                 * history (see NotificationActivity).
                 */
        return NotificationActivity::class.java
    }

    override fun isDebug(): Boolean {
        // return BuildConfig.DEBUG;
        return true
    }

}