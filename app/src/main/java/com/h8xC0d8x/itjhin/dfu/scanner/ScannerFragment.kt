package com.h8xC0d8x.itjhin.dfu.scanner

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.os.ParcelUuid
import java.util.*
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.Button


import com.h8xC0d8x.itjhin.dfu.R
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat
import no.nordicsemi.android.support.v18.scanner.ScanCallback
import no.nordicsemi.android.support.v18.scanner.ScanFilter
import no.nordicsemi.android.support.v18.scanner.ScanResult
import no.nordicsemi.android.support.v18.scanner.ScanSettings
import android.os.Handler
import android.view.LayoutInflater
import android.widget.ListView
import android.widget.Toast



class ScannerFragment : DialogFragment() {
    private val PARAM_UUID = "param_uuid"
    private val SCAN_DURATION: Long = 5000

    private var mUuid: ParcelUuid? = null
    private val REQUEST_PERMISSION_REQ_CODE = 34 // any 8-bit number

    private var mListener: OnDeviceSelectedListener? = null
    private var mPermissionRationale: View? = null
    private var mBluetoothAdapter : BluetoothAdapter? = null

    private var mAdapter: DeviceListAdapter? = null
    private val mHandler = Handler()

    private var mScanButton: Button? = null
    private var mIsScanning = false


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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var args : Bundle? = getArguments();
        if (args != null && args.containsKey(PARAM_UUID)) {
            mUuid = args.getParcelable(PARAM_UUID);
        }

        val manager : BluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        if (manager != null) {
            mBluetoothAdapter = manager.getAdapter();
        }
    }

    override fun onDestroyView() {
        stopScan()
        super.onDestroyView()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder : AlertDialog.Builder = AlertDialog.Builder(requireContext())
        val dialogView : View = LayoutInflater.from(activity).inflate(R.layout.fragment_device_selection, null)
        val listview : ListView = dialogView.findViewById(android.R.id.list)

        listview.setEmptyView(dialogView.findViewById(android.R.id.empty))
        mAdapter = DeviceListAdapter(activity)
        listview.adapter = mAdapter

        builder.setTitle(R.string.scanner_title)
        val dialog : AlertDialog = builder.setView(dialogView).create()

        listview.setOnItemClickListener { parent, view, position, id ->
            stopScan()
            dialog.dismiss()
            val d : ExtendedBluetoothDevice = mAdapter!!.getItem(position) as ExtendedBluetoothDevice
            mListener!!.onDeviceSelected(d.device!!,d.name!!)
        }

        mPermissionRationale = dialogView.findViewById(R.id.permission_rationale) // this is not null only on API23+

        mScanButton = dialogView.findViewById(R.id.action_cancel)
        mScanButton!!.setOnClickListener { v ->
            if (v.id == R.id.action_cancel) {
                if(mIsScanning) {
                    dialog.cancel()
                } else {
                    startScan()
                }
            }
        }

        addBoundDevices();
        if (savedInstanceState == null)
                startScan();

        return dialog;
        //return super.onCreateDialog(savedInstanceState)
    }

    override fun onCancel(dialog: DialogInterface?) {
        super.onCancel(dialog)

        mListener?.onDialogCanceled()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_PERMISSION_REQ_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // We have been granted the Manifest.permission.ACCESS_COARSE_LOCATION permission. Now we may proceed with scanning.
                    startScan()
                } else {
                    mPermissionRationale?.setVisibility(View.VISIBLE)
                    Toast.makeText(activity, R.string.no_required_permission, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Scan for 5 seconds and then stop scanning when a BluetoothLE device is found then mLEScanCallback
     * is activated This will perform regular scan for custom BLE Service UUID and then filter out.
     * using class ScannerServiceParser
     */
    private fun startScan() {
        // Since Android 6.0 we need to obtain either Manifest.permission.ACCESS_COARSE_LOCATION or Manifest.permission.ACCESS_FINE_LOCATION to be able to scan for
        // Bluetooth LE devices. This is related to beacons as proximity devices.
        // On API older than Marshmallow the following code does nothing.
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // When user pressed Deny and still wants to use this functionality, show the rationale
            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) && mPermissionRationale?.visibility == View.GONE) {
                mPermissionRationale?.visibility = View.VISIBLE
                return
            }
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),REQUEST_PERMISSION_REQ_CODE)
            return
        }

        // Hide the rationale message, we don't need it anymore.
        if (mPermissionRationale != null)
            mPermissionRationale!!.setVisibility(View.GONE)

        mAdapter?.clearDevices()
        mScanButton?.text = getString(R.string.scanner_action_cancel)

        val scanner = BluetoothLeScannerCompat.getScanner()
        val settings = ScanSettings.Builder()
            .setLegacy(false)
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(1000)
            .setUseHardwareBatchingIfSupported(false).build()

        val filters = ArrayList<ScanFilter>()
        filters.add(ScanFilter.Builder().setServiceUuid(mUuid).build())
        scanner.startScan(filters, settings, scanCallback)

        mIsScanning = true
        mHandler.postDelayed({
            if (mIsScanning) {
                stopScan()
            }
        }, SCAN_DURATION)

    }

    /**
     * Stop scan if user tap Cancel button
     */
    fun stopScan() {
        if (mIsScanning) {
            mScanButton?.text= getText(R.string.scanner_action_scan)
            val scanner = BluetoothLeScannerCompat.getScanner()
            scanner.stopScan(scanCallback)

            mIsScanning = false
        }
    }

    fun addBoundDevices() {
        val devices : Set<BluetoothDevice> = mBluetoothAdapter!!.bondedDevices
        mAdapter?.addBondedDevices(devices)
    }

    private var scanCallback : ScanCallback = object: ScanCallback() {

        override fun onScanResult(callbackType : Int, result : ScanResult) {
            // do nothing
        }

        override fun onBatchScanResults(results : List<ScanResult> ) {
            mAdapter?.update(results);
        }

        override fun onScanFailed(errorCode : Int) {
            // should never be called
        }
    }

}