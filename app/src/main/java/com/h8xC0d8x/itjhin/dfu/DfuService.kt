package com.h8xC0d8x.itjhin.dfu

import no.nordicsemi.android.dfu.DfuBaseService
import android.app.Activity
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
//import no.nordicsemi.android.dfu.DfuCallback
//import no.nordicsemi.android.dfu.DfuProgressInfo
import no.nordicsemi.android.dfu.R


class DfuService : DfuBaseService() {
    private val TAG : String = "DfuService -- Revic"
    private val mLock = Any()
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mDeviceAddress: String? = null
    private var mDeviceName: String? = null
    private var mDisableNotification: Boolean = false

    private var mError: Int = 0

    val myChild = DfuService()

    private var mDfuServiceImpl : BluetoothGattCallback? = null

    fun handle(intent:Intent) = myChild.onHandleIntent(intent)

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
        const val ACTION_PAUSE = DfuBaseService.ACTION_PAUSE
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
        //return BuildConfig.DEBUG;
        return true
    }


    private val mGattCallback : BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            super.onReadRemoteRssi(gatt, rssi, status)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
        }

        override fun onPhyUpdate(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status)
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
        }

        override fun onReliableWriteCompleted(gatt: BluetoothGatt?, status: Int) {
            super.onReliableWriteCompleted(gatt, status)
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicChanged(gatt, characteristic)
        }

        override fun onDescriptorRead(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            super.onDescriptorRead(gatt, descriptor, status)
        }

        override fun onPhyRead(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            super.onPhyRead(gatt, txPhy, rxPhy, status)
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            // Check whether an error occurred
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    //logi("Connected to GATT server")
                    sendLogBroadcast(LOG_LEVEL_INFO, "Connected to $mDeviceAddress")
                    mConnectionState = STATE_CONNECTED

                    /*
					 *  The onConnectionStateChange callback is called just after establishing connection and before sending Encryption Request BLE event in case of a paired device.
					 *  In that case and when the Service Changed CCCD is enabled we will get the indication after initializing the encryption, about 1600 milliseconds later.
					 *  If we discover services right after connecting, the onServicesDiscovered callback will be called immediately, before receiving the indication and the following
					 *  service discovery and we may end up with old, application's services instead.
					 *
					 *  This is to support the buttonless switch from application to bootloader mode where the DFU bootloader notifies the master about service change.
					 *  Tested on Nexus 4 (Android 4.4.4 and 5), Nexus 5 (Android 5), Samsung Note 2 (Android 4.4.2). The time after connection to end of service discovery is about 1.6s
					 *  on Samsung Note 2.
					 *
					 *  NOTE: We are doing this to avoid the hack with calling the hidden gatt.refresh() method, at least for bonded devices.
					 */
                    if (gatt?.device?.bondState == BluetoothDevice.BOND_BONDED) {
                        //logi("Waiting 1600 ms for a possible Service Changed indication...")
                        waitFor(1600)
                        // After 1.6s the services are already discovered so the following gatt.discoverServices() finishes almost immediately.

                        // NOTE: This also works with shorted waiting time. The gatt.discoverServices() must be called after the indication is received which is
                        // about 600ms after establishing connection. Values 600 - 1600ms should be OK.
                    }

                    // Attempts to discover services after successful connection.
                    sendLogBroadcast(LOG_LEVEL_VERBOSE, "Discovering services...")
                    sendLogBroadcast(LOG_LEVEL_DEBUG, "gatt.discoverServices()")
                    val success = gatt?.discoverServices()
                    //logi("Attempting to start service discovery... " + if (success) "succeed" else "failed")

                    if (!success) {
                        mError = ERROR_SERVICE_DISCOVERY_NOT_STARTED
                    } else {
                        // Just return here, lock will be notified when service discovery finishes
                        return
                    }
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    //logi("Disconnected from GATT server")
                    mConnectionState = STATE_DISCONNECTED
                    if (mDfuServiceImpl != null)
                        mDfuServiceImpl.getGattCallback().onDisconnected()
                }
            } else {
                if (status == 0x08 /* GATT CONN TIMEOUT */ || status == 0x13 /* GATT CONN TERMINATE PEER USER */)
                    logw("Target device disconnected with status: $status")
                else
                    loge("Connection state change error: $status newState: $newState")
                mError = ERROR_CONNECTION_STATE_MASK or status
                if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    mConnectionState = STATE_DISCONNECTED
                    if (mDfuServiceImpl != null)
                        mDfuServiceImpl.getGattCallback().onDisconnected()
                }
            }

            // Notify waiting thread
            synchronized(mLock) {
                mLock.notifyAll()
            }
        }

    }


    /**
     * Connects to the BLE device with given address. This method is SYNCHRONOUS, it wait until
     * the connection status change from {@link #STATE_CONNECTING} to
     * {@link #STATE_CONNECTED_AND_READY} or an error occurs.
     * This method returns <code>null</code> if Bluetooth adapter is disabled.
     *
     * @param address the device address.
     * @return The GATT device or <code>null</code> if Bluetooth adapter is disabled.
     */
    override fun connect(address: String): BluetoothGatt? {
        if (!mBluetoothAdapter!!.isEnabled)
            return null

        mConnectionState = STATE_CONNECTING

        Log.i(TAG, "MyDfuService - Connecting to the device...")
        val device = mBluetoothAdapter!!.getRemoteDevice(address)
        sendLogBroadcast(LOG_LEVEL_DEBUG, "gatt = device.connectGatt(autoConnect = false)")
        val gatt = device.connectGatt(this, false, mGattCallback)

        // We have to wait until the device is connected and services are discovered
        // Connection error may occur as well.
        try {

            synchronized(mLock) {
                while ((mConnectionState == STATE_CONNECTING || mConnectionState == STATE_CONNECTED) && mError == 0)
                    mLock.wait()

            }
        } catch (e: InterruptedException) {
            Log.e(TAG, "MyDfuService [connect] - Sleeping Interrupted : " + e.message)
        }

        return gatt
    }

    /**
     * Disconnects from the device and cleans local variables in case of error.
     * This method is SYNCHRONOUS and wait until the disconnecting process will be completed.
     *
     * @param gatt  the GATT device to be disconnected.
     * @param error error number.
     */
    override fun terminateConnection(gatt: BluetoothGatt, error: Int) {
        if (mConnectionState != STATE_DISCONNECTED) {
            // Disconnect from the device
            disconnect(gatt)
        }

        // Close the device
        refreshDeviceCache(gatt, false) // This should be set to true when DFU Version is 0.5 or lower
        close(gatt)
        waitFor(600)
        if (error != 0)
            report(error)
    }


    /**
     * Disconnects from the device. This is SYNCHRONOUS method and waits until the callback returns
     * new state. Terminates immediately if device is already disconnected. Do not call this method
     * directly, use {@link #terminateConnection(android.bluetooth.BluetoothGatt, int)} instead.
     *
     * @param gatt the GATT device that has to be disconnected.
     */
    override fun disconnect(gatt: BluetoothGatt) {
        if (mConnectionState == STATE_DISCONNECTED)
            return

        sendLogBroadcast(LOG_LEVEL_VERBOSE, "Disconnecting...")
        //mProgressInfo.setProgress(PROGRESS_DISCONNECTING)
        mConnectionState = STATE_DISCONNECTING

        //logi("Disconnecting from the device...")
        sendLogBroadcast(LOG_LEVEL_DEBUG, "gatt.disconnect()")
        gatt.disconnect()

        // We have to wait until device gets disconnected or an error occur
        waitUntilDisconnected()
        sendLogBroadcast(LOG_LEVEL_INFO, "Disconnected")
    }


    /**
     * Wait until the connection state will change to {@link #STATE_DISCONNECTED} or until
     * an error occurs.
     */
    override fun waitUntilDisconnected() {
        try {
            synchronized(mLock) {
                while (mConnectionState != STATE_DISCONNECTED && mError == 0)
                    mLock.wait()
            }
        } catch (e: InterruptedException) {
            Log.e(TAG, "MyDfuService [waitUntilDisconnected] - Sleeping Interrupted : " + e.message)
        }

    }

    /**
     * Wait for given number of milliseconds.
     *
     * @param millis waiting period.
     */
    override fun waitFor(millis: Int) {
        synchronized(mLock) {
            try {
                sendLogBroadcast(LOG_LEVEL_DEBUG, "wait($millis)")
                mLock.wait(millis.toLong())
            } catch (e: InterruptedException) {
                Log.e(TAG, "MyDfuService [waitFor] - Sleeping Interrupted : " + e.message)
            }

        }
    }

    /**
     * Closes the GATT device and cleans up.
     *
     * @param gatt the GATT device to be closed.
     */
    override fun close(gatt: BluetoothGatt) {
        //logi("Cleaning up...")
        sendLogBroadcast(LOG_LEVEL_DEBUG, "gatt.close()")
        gatt.close()
        mConnectionState = STATE_CLOSED
    }

    /**
     * Clears the device cache. After uploading new firmware the DFU target will have other
     * services than before.
     *
     * @param gatt  the GATT device to be refreshed.
     * @param force <code>true</code> to force the refresh.
     */
    override fun refreshDeviceCache(gatt: BluetoothGatt, force: Boolean) {
        /*
		 * If the device is bonded this is up to the Service Changed characteristic to notify Android that the services has changed.
		 * There is no need for this trick in that case.
		 * If not bonded, the Android should not keep the services cached when the Service Changed characteristic is present in the target device database.
		 * However, due to the Android bug (still exists in Android 5.0.1), it is keeping them anyway and the only way to clear services is by using this hidden refresh method.
		 */
        if (force || gatt.device.bondState == BluetoothDevice.BOND_NONE) {
            sendLogBroadcast(LOG_LEVEL_DEBUG, "gatt.refresh() (hidden)")
            /*
			 * There is a refresh() method in BluetoothGatt class but for now it's hidden. We will call it using reflections.
			 */
            try {

                val refresh = gatt.javaClass.getMethod("refresh")
                val success = refresh.invoke(gatt) as Boolean
                //logi("Refreshing result: $success")
            } catch (e: Exception) {
                //loge("An exception occurred while refreshing device", e)
                sendLogBroadcast(LOG_LEVEL_WARNING, "Refreshing failed")
            }

        }
    }

    /**
     * Creates or updates the notification in the Notification Manager. Sends broadcast with given
     * error number to the activity.
     *
     * @param error the error number.
     */
    private fun report(error: Int) {
        sendErrorBroadcast(error)

        if (mDisableNotification)
            return

        // create or update notification:
        val deviceAddress = mDeviceAddress
        val deviceName = if (mDeviceName != null) mDeviceName else getString(R.string.dfu_unknown_name)

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_DFU)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOnlyAlertOnce(true)
            .setColor(Color.RED)
            .setOngoing(false)
            .setContentTitle(getString(R.string.dfu_status_error))
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentText(getString(R.string.dfu_status_error_msg))
            .setAutoCancel(true)

        // update the notification
        val intent = Intent(this, notificationTarget)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(EXTRA_DEVICE_ADDRESS, deviceAddress)
        intent.putExtra(EXTRA_DEVICE_NAME, deviceName)
        intent.putExtra(EXTRA_PROGRESS, error) // this may contains ERROR_CONNECTION_MASK bit!
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        builder.setContentIntent(pendingIntent)

        // Any additional configuration?
        updateErrorNotification(builder)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager!!.notify(NOTIFICATION_ID, builder.build())
    }

    /**
     *
     * Broadcasting Progress, Error, and Log
     *
     **/
/*
    private fun sendProgressBroadcast(info: DfuProgressInfo) {
        val broadcast = Intent(BROADCAST_PROGRESS)
        broadcast.putExtra(EXTRA_DATA, info.getProgress())
        broadcast.putExtra(EXTRA_DEVICE_ADDRESS, mDeviceAddress)
        broadcast.putExtra(EXTRA_PART_CURRENT, info.getCurrentPart())
        broadcast.putExtra(EXTRA_PARTS_TOTAL, info.getTotalParts())
        broadcast.putExtra(EXTRA_SPEED_B_PER_MS, info.getSpeed())
        broadcast.putExtra(EXTRA_AVG_SPEED_B_PER_MS, info.getAverageSpeed())
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast)
    }
*/
    private fun sendErrorBroadcast(error: Int) {
        val broadcast = Intent(BROADCAST_ERROR)
        if (error and ERROR_CONNECTION_MASK > 0) {
            broadcast.putExtra(EXTRA_DATA, error and ERROR_CONNECTION_MASK.inv())
            broadcast.putExtra(EXTRA_ERROR_TYPE, ERROR_TYPE_COMMUNICATION)
        } else if (error and ERROR_CONNECTION_STATE_MASK > 0) {
            broadcast.putExtra(EXTRA_DATA, error and ERROR_CONNECTION_STATE_MASK.inv())
            broadcast.putExtra(EXTRA_ERROR_TYPE, ERROR_TYPE_COMMUNICATION_STATE)
        } else if (error and ERROR_REMOTE_MASK > 0) {
            broadcast.putExtra(EXTRA_DATA, error and ERROR_REMOTE_MASK.inv())
            broadcast.putExtra(EXTRA_ERROR_TYPE, ERROR_TYPE_DFU_REMOTE)
        } else {
            broadcast.putExtra(EXTRA_DATA, error)
            broadcast.putExtra(EXTRA_ERROR_TYPE, ERROR_TYPE_OTHER)
        }
        broadcast.putExtra(EXTRA_DEVICE_ADDRESS, mDeviceAddress)
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast)
    }

    private fun sendLogBroadcast(level: Int, message: String) {
        val fullMessage = "[myDFU] $message"
        val broadcast = Intent(BROADCAST_LOG)
        broadcast.putExtra(EXTRA_LOG_MESSAGE, fullMessage)
        broadcast.putExtra(EXTRA_LOG_LEVEL, level)
        broadcast.putExtra(EXTRA_DEVICE_ADDRESS, mDeviceAddress)
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast)
    }


    /**
     * Ways to cast kotlin object to java.lang.Object to get concurency at lower level.
     * Alternatively, use ReentrantLock !
     **/
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private fun Any.wait() = (this as java.lang.Object).wait()

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private fun Any.wait(timeout: Long) = (this as java.lang.Object).wait(timeout)

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private fun Any.notifyAll() = (this as java.lang.Object).notifyAll()

}