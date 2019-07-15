/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.h8xC0d8x.itjhin.dfu

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.app.ActivityManager
import android.app.NotificationManager
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import android.os.Parcelable
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.provider.MediaStore
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.loader.content.CursorLoader
import androidx.preference.PreferenceManager


import com.h8xC0d8x.itjhin.dfu.R
import com.h8xC0d8x.itjhin.dfu.utility.FileHelper
import com.h8xC0d8x.itjhin.dfu.settings.SettingActivity
import com.h8xC0d8x.itjhin.dfu.scanner.ScannerFragment
import com.h8xC0d8x.itjhin.dfu.adapter.FileBrowserAppsAdapter


import no.nordicsemi.android.dfu.DfuProgressListener
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter
import no.nordicsemi.android.dfu.DfuServiceListenerHelper
import no.nordicsemi.android.dfu.DfuServiceInitiator

import java.io.File


class DfuActivity : AppCompatActivity(), LoaderManager.LoaderCallbacks<Cursor>, ScannerFragment.OnDeviceSelectedListener,
    UploadCancelFragment.CancelFragmentListener, PermissionRationaleFragment.PermissionDialogListener
{
    private val TAG = "DfuActivity"


    private val PREFS_DEVICE_NAME = "com.h8xC0d8x.itjhin.dfu.PREFS_DEVICE_NAME"
    private val PREFS_FILE_NAME = "com.h8xC0d8x.itjhin.dfu.PREFS_FILE_NAME"
    private val PREFS_FILE_TYPE = "com.h8xC0d8x.itjhin.dfu.PREFS_FILE_TYPE"
    private val PREFS_FILE_SCOPE = "com.h8xC0d8x.itjhin.dfu.PREFS_FILE_SCOPE"
    private val PREFS_FILE_SIZE = "com.h8xC0d8x.itjhin.dfu.PREFS_FILE_SIZE"

    private val DATA_DEVICE = "device"
    private val DATA_FILE_TYPE = "file_type"
    private val DATA_FILE_TYPE_TMP = "file_type_tmp"
    private val DATA_FILE_PATH = "file_path"
    private val DATA_FILE_STREAM = "file_stream"
    private val DATA_INIT_FILE_PATH = "init_file_path"
    private val DATA_INIT_FILE_STREAM = "init_file_stream"
    private val DATA_STATUS = "status"
    private val DATA_SCOPE = "scope"
    private val DATA_DFU_COMPLETED = "dfu_completed"
    private val DATA_DFU_ERROR = "dfu_error"

    private val EXTRA_URI = "uri"

    private val PERMISSION_REQ = 25
    private val ENABLE_BT_REQ = 0
    private val SELECT_FILE_REQ = 1
    private val SELECT_INIT_FILE_REQ = 2

    private var mDeviceNameView: TextView? = null
    private var mFileNameView: TextView? = null
    private var mFileTypeView: TextView? = null
    private var mFileScopeView: TextView? = null
    private var mFileSizeView: TextView? = null
    private var mFileStatusView: TextView? = null
    private var mTextPercentage: TextView? = null
    private var mTextUploading: TextView? = null
    private var mProgressBar: ProgressBar? = null

    private var mSelectFileButton: Button? = null
    private var mUploadButton: Button? = null
    private var mConnectButton: Button? = null

    private var mSelectedDevice: BluetoothDevice? = null
    private var mFilePath: String? = null
    private var mFileStreamUri: Uri? = null
    private var mInitFilePath: String? = null
    private var mInitFileStreamUri: Uri? = null
    private var mFileType: Int = 0
    private var mFileTypeTmp: Int = 0 // This value is being used when user is selecting a file not to overwrite the old value (in case he/she will cancel selecting file)

    private var mScope: Int? = null
    private var mStatusOk: Boolean = false
    /** Flag set to true in [.onRestart] and to false in [.onPause].  */
    private var mResumed: Boolean = false
    /** Flag set to true if DFU operation was completed while [.mResumed] was false.  */
    private var mDfuCompleted: Boolean = false
    /** The error message received from DFU service while [.mResumed] was false.  */
    private var mDfuError: String? = null

    private var fileHelper : FileHelper? = null


    /** Instantiating Object and overriding the method */
    private val mDfuProgressListener : DfuProgressListener = object: DfuProgressListenerAdapter() {

        override fun onDeviceConnecting(deviceAddress: String) {
            mProgressBar?.isIndeterminate = true
            mTextPercentage?.text = getString(R.string.dfu_status_connecting)
            //super.onDeviceConnecting(deviceAddress)
        }

        override fun onDfuProcessStarting(deviceAddress: String) {
            mProgressBar?.isIndeterminate = true
            mTextPercentage?.text = getString(R.string.dfu_status_starting)
            //super.onDfuProcessStarting(deviceAddress)
        }

        override fun onEnablingDfuMode(deviceAddress: String) {
            mProgressBar?.isIndeterminate = true
            mTextPercentage?.text = getString(R.string.dfu_status_switching_to_dfu)
            //super.onEnablingDfuMode(deviceAddress)
        }

        override fun onFirmwareValidating(deviceAddress: String) {
            mProgressBar?.isIndeterminate = true
            mTextPercentage?.text = getString(R.string.dfu_status_validating)
            //super.onFirmwareValidating(deviceAddress)
        }

        override fun onDeviceDisconnecting(deviceAddress: String?) {
            mProgressBar?.isIndeterminate = true
            mTextPercentage?.text = getString(R.string.dfu_status_disconnecting)
            //super.onDeviceDisconnecting(deviceAddress)
        }

        override fun onDfuCompleted(deviceAddress: String) {
            mTextPercentage?.text = getString(R.string.dfu_status_completed)
            if(mResumed) {

                // let's wait a bit until we cancel the notification. When canceled immediately it will be recreated by service again.
                Handler().postDelayed({
                    onTransferCompleted()

                    val manager : NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.cancel(DfuService.NOTIFICATION_ID)
                }, 200)

            } else {
                // Save that the DFU process has finished
                mDfuCompleted = true
            }
        }

        override fun onDfuAborted(deviceAddress: String) {
            mTextPercentage?.text = getString(R.string.dfu_status_aborted)

            // let's wait a bit until we cancel the notification. When canceled immediately it will be recreated by service again.
            Handler().postDelayed({
                onUploadCanceled()

                val manager : NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.cancel(DfuService.NOTIFICATION_ID)
            },200)
        }

        override fun onProgressChanged( deviceAddress: String, percent: Int, speed: Float,
            avgSpeed: Float, currentPart: Int, partsTotal: Int) {
            mProgressBar?.isIndeterminate = false
            mProgressBar?.progress = percent
            mTextPercentage?.text = getString(R.string.dfu_uploading_percentage, percent)

            if (partsTotal > 1) {
                mTextUploading?.text = getString(R.string.dfu_status_uploading_part, partsTotal)
            } else {
                mTextUploading?.text = getString(R.string.dfu_status_uploading)
            }
        }

        override fun onError(deviceAddress: String, error: Int, errorType: Int, message: String?) {
            if(mResumed) {
                showErrorMessage(message!!)

                // let's wait a bit until we cancel the notification. When canceled immediately it will be recreated by service again.
                Handler().postDelayed({
                    val manager : NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.cancel(DfuService.NOTIFICATION_ID)
                }, 200)
            } else {
                mDfuError = message
            }
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feature_dfu)
        isBLESupported()
        if (!isBLEEnabled()) {
            showBLEDialog()
        }
        setGUI()

        fileHelper = FileHelper()
        // Try to create sample files
        if (fileHelper!!.newSamplesAvailable(this)) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fileHelper!!.createSamples(this)
            } else {
                val dialog = PermissionRationaleFragment().getInstance(
                    R.string.permission_sd_text,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                dialog.show(supportFragmentManager, null)
            }
        }

        // restore saved state
        mFileType = DfuService.TYPE_AUTO // Default
        if (savedInstanceState != null) {
            mFileType = savedInstanceState.getInt(DATA_FILE_TYPE)
            mFileTypeTmp = savedInstanceState.getInt(DATA_FILE_TYPE_TMP)
            mFilePath = savedInstanceState.getString(DATA_FILE_PATH)
            mFileStreamUri = savedInstanceState.getParcelable<Parcelable>(DATA_FILE_STREAM) as Uri
            mInitFilePath = savedInstanceState.getString(DATA_INIT_FILE_PATH)
            mInitFileStreamUri = savedInstanceState.getParcelable<Parcelable>(DATA_INIT_FILE_STREAM) as Uri
            mSelectedDevice = savedInstanceState.getParcelable<Parcelable>(DATA_DEVICE) as BluetoothDevice
            mStatusOk = mStatusOk || savedInstanceState.getBoolean(DATA_STATUS)
            mScope = if (savedInstanceState.containsKey(DATA_SCOPE)) savedInstanceState.getInt(DATA_SCOPE) else null
            mUploadButton!!.isEnabled = (mSelectedDevice != null && mStatusOk)
            mDfuCompleted = savedInstanceState.getBoolean(DATA_DFU_COMPLETED)
            mDfuError = savedInstanceState.getString(DATA_DFU_ERROR)
        }

        DfuServiceListenerHelper.registerProgressListener(this, mDfuProgressListener)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(DATA_FILE_TYPE, mFileType)
        outState.putInt(DATA_FILE_TYPE_TMP, mFileTypeTmp)
        outState.putString(DATA_FILE_PATH, mFilePath)
        outState.putParcelable(DATA_FILE_STREAM, mFileStreamUri)
        outState.putString(DATA_INIT_FILE_PATH, mInitFilePath)
        outState.putParcelable(DATA_INIT_FILE_STREAM, mInitFileStreamUri)
        outState.putParcelable(DATA_DEVICE, mSelectedDevice)
        outState.putBoolean(DATA_STATUS, mStatusOk)
        if (mScope != null) outState.putInt(DATA_SCOPE, mScope!!)
        outState.putBoolean(DATA_DFU_COMPLETED, mDfuCompleted)
        outState.putString(DATA_DFU_ERROR, mDfuError)
    }

    private fun setGUI() {
        val toolbar: Toolbar = findViewById(R.id.toolbar_actionbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        mDeviceNameView = findViewById(R.id.device_name)
        mFileNameView = findViewById(R.id.file_name)
        mFileTypeView = findViewById(R.id.file_type)
        mFileScopeView = findViewById(R.id.file_scope)
        mFileSizeView = findViewById(R.id.file_size)
        mFileStatusView = findViewById(R.id.file_status)
        mSelectFileButton = findViewById(R.id.action_select_file)
        mUploadButton = findViewById(R.id.action_upload)
        mConnectButton = findViewById(R.id.action_connect)
        mTextPercentage = findViewById(R.id.textviewProgress)
        mTextUploading = findViewById(R.id.textviewUploading)
        mProgressBar = findViewById(R.id.progressbar_file)

        val preferences : SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        if(isDfuServiceRunning()) {
            // Restore image file information
            mDeviceNameView!!.text = preferences.getString(PREFS_DEVICE_NAME, "")
            mFileNameView!!.text = preferences.getString(PREFS_FILE_NAME, "")
            mFileTypeView!!.text = preferences.getString(PREFS_FILE_TYPE, "")
            mFileScopeView!!.text = preferences.getString(PREFS_FILE_SCOPE, "")
            mFileSizeView!!.text = preferences.getString(PREFS_FILE_SIZE, "")
            mFileStatusView!!.text = getString(R.string.dfu_file_status_ok)
            mStatusOk = true
            showProgressBar()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        DfuServiceListenerHelper.unregisterProgressListener(this, mDfuProgressListener)
    }

    override fun onResume() {
        super.onResume()
        mResumed = true;
        if (mDfuCompleted)
            onTransferCompleted();
        if (mDfuError != null)
            showErrorMessage(mDfuError!!);
        if (mDfuCompleted || mDfuError != null) {

            // if this activity is still open and upload process was completed, cancel the notification
            val manager : NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(DfuService.NOTIFICATION_ID)
            mDfuCompleted = false
            mDfuError = null
        }
    }

    override fun onPause() {
        super.onPause()
        mResumed = false
    }

    override fun onRestart() {
        super.onRestart()
    }

    /**
     *  Menu and options
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.settings_and_about, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.action_about -> {
                val fragment = AppHelpFragment().getInstance(R.string.dfu_about_text, false)
                fragment.show(supportFragmentManager, "help_fragment")
            }
            R.id.action_settings -> {
                val intent = Intent(this, SettingActivity::class.java)
                startActivity(intent)
            }
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != RESULT_OK)
                        return

        when(requestCode) {
            SELECT_FILE_REQ -> {
                // clear previous data
                mFileType = mFileTypeTmp
                mFilePath = null
                mFileStreamUri = null
                /*
                 * The URI returned from application may be in 'file' or 'content' schema.
                 * 'File' schema allows us to create a File object and read details from if directly.
                 *  Data from 'Content' schema must be read by Content Provider. To do that we are using a Loader.
                 */
                val uri : Uri = data?.data!!
                if(uri.scheme.equals("file")) {
                    val path : String = uri!!.path!!
                    val file : File = File(path)
                    mFilePath = path

                    updateFileInfo(file.name, file.length(), mFileType)

                } else if (uri.scheme.equals("content")) {
                    mFileStreamUri = uri

                    // if application returned Uri for streaming, let's us it. Does it works?
                    // FIXME both Uris works with Google Drive app. Why both? What's the difference? How about other apps like DropBox?
                    val extras : Bundle = data?.extras!!
                    if (extras != null && extras.containsKey(Intent.EXTRA_STREAM)) {
                        mFileStreamUri = extras.getParcelable(Intent.EXTRA_STREAM)
                    }

                    // file name and size must be obtained from Content Provider
                    val bundle : Bundle = Bundle()
                    bundle.putParcelable(EXTRA_URI, uri)
                    //FIXME !!
                    //loaderManager.restartLoader<Cursor>(SELECT_FILE_REQ, bundle, this)
                }
            }

            SELECT_INIT_FILE_REQ -> {
                mInitFilePath = null
                mInitFileStreamUri = null

                val uri : Uri = data?.data!!
                /*
                 * The URI returned from application may be in 'file' or 'content' schema. 'File' schema allows us to create a File object and read details from if
                 * directly. Data from 'Content' schema must be read by Content Provider. To do that we are using a Loader.
                 */

                if (uri.scheme.equals("file")) {
                    mInitFilePath = uri.path
                    mFileStatusView?.text = getString(R.string.dfu_file_status_ok_with_init)
                } else  if (uri.scheme.equals("content")) {
                    mInitFileStreamUri = uri
                    val extras : Bundle = data.extras!!
                    if (extras != null && extras.containsKey(Intent.EXTRA_STREAM))
                        mInitFileStreamUri = extras.getParcelable(Intent.EXTRA_STREAM)

                    mFileStatusView?.text = getString(R.string.dfu_file_status_ok_with_init)
                }
            }

        }
    }

    private fun isBLESupported() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            showToast(R.string.no_ble)
            finish()
        }
    }

    private fun isBLEEnabled(): Boolean {
        val manager : BluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter : BluetoothAdapter = manager.adapter
        // TODO: Check on the manager adapter return
        return adapter != null && adapter.isEnabled
    }

    private fun showBLEDialog() {
        val enableIntent : Intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableIntent, ENABLE_BT_REQ)
    }


    private fun showToast(messageResId: Int) {
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


    private fun isDfuServiceRunning(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (DfuService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }


    private fun showProgressBar() {
        mProgressBar!!.visibility = View.VISIBLE
        mTextPercentage!!.visibility = View.VISIBLE
        mTextPercentage!!.text = null
        mTextUploading!!.text = getString(R.string.dfu_status_uploading)
        mTextUploading!!.visibility = View.VISIBLE
        mConnectButton!!.isEnabled = false
        mSelectFileButton!!.isEnabled = false
        mUploadButton!!.isEnabled = true
        mUploadButton!!.text = getString(R.string.dfu_action_upload_cancel)
    }

    /** Listener Method Helper */
    private fun onTransferCompleted() {
        clearUI(true)
        showToast(R.string.dfu_success)
    }

    fun onUploadCanceled() {
        clearUI(false)
        showToast(R.string.dfu_aborted)
    }

    private fun showErrorMessage(message: String) {
        clearUI(false)
        showToast("Upload failed: $message")
    }

    private fun clearUI(clearDevice: Boolean) {
        mProgressBar!!.visibility = View.INVISIBLE
        mTextPercentage!!.visibility = View.INVISIBLE
        mTextUploading!!.visibility = View.INVISIBLE
        mConnectButton!!.isEnabled = true
        mSelectFileButton!!.isEnabled = true
        mUploadButton!!.isEnabled = false
        mUploadButton!!.text = getString(R.string.dfu_action_upload)
        if (clearDevice) {
            mSelectedDevice = null
            mDeviceNameView!!.text = getString(R.string.dfu_default_name)
        }
        // Application may have lost the right to these files if Activity was closed during upload (grant uri permission). Clear file related values.
        mFileNameView!!.text = null
        mFileTypeView!!.text = null
        mFileScopeView!!.text = null
        mFileSizeView!!.text = null
        mFileStatusView!!.text = getString(R.string.dfu_file_status_no_file)
        mFilePath = null
        mFileStreamUri = null
        mInitFilePath = null
        mInitFileStreamUri = null
        mStatusOk = false
    }

    fun showDeviceScanningDialog() {
        val dialog : ScannerFragment = ScannerFragment().getInstance(null)
        // Device that is advertising directly does not have the GENERAL_DISCOVERABLE nor LIMITED_DISCOVERABLE flag set.
        dialog.show(supportFragmentManager,"scan_fragment")
    }


    /********************************************************************
     *
     *  Loader Interface
     *
     ********************************************************************/

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val uri : Uri = args?.getParcelable<Uri>(EXTRA_URI)!!
        /*
         * Some apps, f.e. Google Drive allow to select file that is not on the device.
         * There is no "_data" column handled by that provider.
         * Let's try to obtain all columns and then check which columns are present.
         */
        return CursorLoader(applicationContext,uri,null /* all columns, instead of projection */,null,null,null)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        if(data != null && data.moveToNext()) {
            /*
             * Here we have to check the column indexes by name as we have requested for all. The order may be different.
             */
            val fileName : String = data.getString(data.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)) /*0 DISPLAY_NAME */
            val fileSize : Int = data.getInt(data.getColumnIndex(MediaStore.MediaColumns.SIZE)) /* 1 SIZE */
            var filePath : String? = null
            val dataIndex : Int = data.getColumnIndex(MediaStore.MediaColumns.DATA)
            if (dataIndex != -1)
                filePath = data.getString(dataIndex)

            if(!TextUtils.isEmpty(filePath))
                mFilePath = filePath

            updateFileInfo(fileName, fileSize as Long, mFileType)

        } else {
            mFileNameView?.text = null
            mFileTypeView?.text = null
            mFileSizeView?.text = null
            mFilePath = null
            mFileStreamUri = null
            mFileStatusView?.text = getString(R.string.dfu_file_status_error)
            mStatusOk = false
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        mFileNameView?.text = null
        mFileTypeView?.text = null
        mFileSizeView?.text = null
        mFilePath = null
        mFileStreamUri = null
        mStatusOk = false
    }


    /**
     * Updates the file information on UI
     *
     * @param fileName file name
     * @param fileSize file length
     */
    private fun updateFileInfo(fileName : String, fileSize : Long, fileType : Int)
    {
        mFileNameView?.text = fileName

        when(fileType) {
            DfuService.TYPE_AUTO -> mFileTypeView?.text = resources.getStringArray(R.array.dfu_file_type)[0]
            DfuService.TYPE_SOFT_DEVICE -> mFileTypeView?.text = resources.getStringArray(R.array.dfu_file_type)[1]
            DfuService.TYPE_BOOTLOADER -> mFileTypeView?.text = resources.getStringArray(R.array.dfu_file_type)[2]
            DfuService.TYPE_APPLICATION -> mFileTypeView?.text = resources.getStringArray(R.array.dfu_file_type)[3]
        }

        mFileSizeView?.text = getString(R.string.dfu_file_size_text, fileSize)
        mFileScopeView?.text = getString(R.string.not_available)

        val extension : Regex = if (mFileType == DfuService.TYPE_AUTO) "(?i)ZIP".toRegex() else "(?i)HEX|BIN".toRegex() // (?i) =  case insensitive
        val statusOK : Boolean = MimeTypeMap.getFileExtensionFromUrl(fileName).matches(extension)
        mStatusOk = statusOK

        mFileStatusView?.text = if(statusOK) getString(R.string.dfu_file_status_ok) else getString(R.string.dfu_file_status_invalid)
        mUploadButton?.isEnabled = (mSelectedDevice != null && statusOK)

        // Ask the user for the Init packet file if HEX or BIN files are selected.
        // In case of a ZIP file the Init packets should be included in the ZIP.
        if (statusOK) {
            if(fileType != DfuService.TYPE_AUTO) {
                mScope = null
                mFileScopeView?.text = getString(R.string.not_available)
                AlertDialog.Builder(this).setTitle(R.string.dfu_file_init_title).setMessage(R.string.dfu_file_init_message)
                    .setNegativeButton(R.string.no) {dialog, which ->
                        mInitFilePath = null
                        mInitFileStreamUri = null
                    }
                    .setPositiveButton(R.string.yes) { dialog, which ->
                        val intent = Intent(Intent.ACTION_GET_CONTENT)
                        intent.type = DfuService.MIME_TYPE_OCTET_STREAM
                        intent.addCategory(Intent.CATEGORY_OPENABLE)
                        startActivityForResult(intent, SELECT_INIT_FILE_REQ)
                    }
                    .show()
            } else {
                AlertDialog.Builder(this).setTitle(R.string.dfu_file_scope_title).setCancelable(false)
                    .setSingleChoiceItems(R.array.dfu_file_scope, 0) { dialog, which ->
                        when (which) {
                            0 -> mScope = null
                            1 -> mScope = DfuServiceInitiator.SCOPE_SYSTEM_COMPONENTS
                            2 -> mScope = DfuServiceInitiator.SCOPE_APPLICATION
                        }
                    }
                    .setPositiveButton(R.string.ok) { dialogInterface, i ->
                        val index: Int
                        if (mScope == null) {
                            index = 0
                        } else if (mScope === DfuServiceInitiator.SCOPE_SYSTEM_COMPONENTS) {
                            index = 1
                        } else {
                            index = 2
                        }
                        mFileScopeView?.text = resources.getStringArray(R.array.dfu_file_scope)[index]
                    }
                    .show()
            }
        }
    }

    /********************************************************************
     *
     *  ScannerFragment Interface
     *
     ********************************************************************/

    override fun onDeviceSelected(device: BluetoothDevice, name: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onDialogCanceled() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /********************************************************************
     *
     *  UploadCancelFragment Interface
     *
     ********************************************************************/

    override fun onCancelUpload() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    /********************************************************************
     *
     *  PermissionRationaleFragment Interface
     *
     ********************************************************************/

    override fun onRequestPermission(permission: String) {
        ActivityCompat.requestPermissions(this, arrayOf(permission), PERMISSION_REQ);
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQ ->
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // We have been granted the Manifest.permission.WRITE_EXTERNAL_STORAGE permission. Now we may proceed with exporting.
                    FileHelper().createSamples(this);
                } else {
                    Toast.makeText(this, R.string.no_required_permission, Toast.LENGTH_SHORT).show();
                }
        }
    }

    /**
     * Called when the question mark was pressed
     *
     * @param view a button that was pressed
     */
    fun onSelectFileHelpClicked(view: View) {
        AlertDialog.Builder(this).setTitle(R.string.dfu_help_title).setMessage(R.string.dfu_help_message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    	/**
	 * Called when Select File was pressed
	 *
	 * @param view a button that was pressed
	 */
    fun onSelectFileClicked(view : View) {
            mFileTypeTmp = mFileType
            var index : Int = 0

            when(mFileType) {
                DfuService.TYPE_AUTO -> index = 0
                DfuService.TYPE_SOFT_DEVICE -> index = 1
                DfuService.TYPE_BOOTLOADER -> index = 2
                DfuService.TYPE_APPLICATION -> index = 3
            }

            AlertDialog.Builder(this).setTitle(R.string.dfu_file_type_title)
                .setSingleChoiceItems(R.array.dfu_file_type,index) { dialog, which ->
                    when (which) {
                        0 -> mFileTypeTmp = DfuService.TYPE_AUTO
                        1 -> mFileTypeTmp = DfuService.TYPE_SOFT_DEVICE
                        2 -> mFileTypeTmp = DfuService.TYPE_BOOTLOADER
                        3 -> mFileTypeTmp = DfuService.TYPE_APPLICATION
                    }
                }
                .setPositiveButton(R.string.ok){ dialog, which ->
                    openFileChooser()
                }
                .setNeutralButton(R.string.dfu_file_info) { dialog,which ->
                    val fragment : ZipInfoFragment = ZipInfoFragment()
                    fragment.show(getSupportFragmentManager(), "help_fragment");
                }
                .setNegativeButton(R.string.cancel,null)
                .show()
        }


    private fun openFileChooser() {
        val intent : Intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.setType(if (mFileTypeTmp == DfuService.TYPE_AUTO) DfuService.MIME_TYPE_ZIP else DfuService.MIME_TYPE_OCTET_STREAM)
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        if (intent.resolveActivity(packageManager) != null) {
            // file browser has been found on the device
            startActivityForResult(intent, SELECT_FILE_REQ)
        } else {
            // there is no any file browser app, let's try to download one
            val customView : View = layoutInflater.inflate(R.layout.app_file_browser, null)
            val appsList : ListView = customView.findViewById(android.R.id.list)
            appsList.adapter = FileBrowserAppsAdapter(this)
            appsList.choiceMode = ListView.CHOICE_MODE_SINGLE
            appsList.setItemChecked(0, true)

            AlertDialog.Builder(this).setTitle(R.string.dfu_alert_no_filebrowser_title).setView(customView)
                .setNegativeButton(R.string.no) { dialog, which ->
                    dialog.dismiss()
                }
                .setPositiveButton(R.string.ok) { dialog, which ->
                    val pos : Int = appsList.checkedItemPosition
                    if (pos >= 0) {
                        val query : String = resources.getStringArray(R.array.dfu_app_file_browser_action)[pos]
                        val storeIntent : Intent = Intent(Intent.ACTION_VIEW, Uri.parse(query))
                        startActivity(storeIntent)
                    }
                }
                .show()
        }
    }

}

