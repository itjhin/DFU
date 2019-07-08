package com.h8xC0d8x.itjhin.dfu.scanner

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.os.ParcelUuid
import java.util.*
import android.bluetooth.BluetoothDevice
import android.content.Context
//import com.h8xC0d8x.itjhin.dfu.scanner.ScannerFragment.OnDeviceSelectedListener








class ScannerFragment : DialogFragment() {
    private val PARAM_UUID = "param_uuid"
    private var mUuid: ParcelUuid? = null

    private var mListener: OnDeviceSelectedListener? = null

    fun getInstance(uuid: UUID?): ScannerFragment {
        val fragment = ScannerFragment()

        val args = Bundle()
        if (uuid != null)
            args.putParcelable(PARAM_UUID, ParcelUuid(uuid))
        fragment.arguments = args
        return fragment
    }

    /**
     * Interface required to be implemented by activity.
     */
    interface OnDeviceSelectedListener {
        /**
         * Fired when user selected the device.
         *
         * @param device
         * the device to connect to
         * @param name
         * the device name. Unfortunately on some devices [BluetoothDevice.getName]
         * always returns `null`, i.e. Sony Xperia Z1 (C6903) with Android 4.3.
         * The name has to be parsed manually form the Advertisement packet.
         */
        fun onDeviceSelected(device: BluetoothDevice, name: String)

        /**
         * Fired when scanner dialog has been cancelled without selecting a device.
         */
        fun onDialogCanceled()
    }

    /**
     * This will make sure that [OnDeviceSelectedListener] interface is implemented by activity.
     */
    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            this.mListener = context as OnDeviceSelectedListener
        } catch (e: ClassCastException) {
            throw ClassCastException(context.toString() + " must implement OnDeviceSelectedListener")
        }

    }
}