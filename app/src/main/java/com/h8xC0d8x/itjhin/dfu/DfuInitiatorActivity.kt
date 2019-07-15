

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

import android.bluetooth.BluetoothDevice
import androidx.appcompat.app.AppCompatActivity
import com.h8xC0d8x.itjhin.dfu.scanner.ScannerFragment
import android.content.Intent
import android.os.Bundle




/**
 * The activity is started only by a remote connected computer using ADB. It shows a list of DFU-supported devices in range and allows user to select target device. The HEX file will be uploaded to
 * selected device using {@link DfuService}.
 */
class DfuInitiatorActivity : AppCompatActivity(), ScannerFragment.OnDeviceSelectedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The activity must be started with a path to the HEX file
        val intent = intent
        if (!intent.hasExtra(DfuService.EXTRA_FILE_PATH))
            finish()


        if (savedInstanceState == null) {

            val fragment : ScannerFragment = ScannerFragment.getInstance(null) // Device that is advertising directly does not have the GENERAL_DISCOVERABLE nor LIMITED_DISCOVERABLE flag set.
            fragment.show(supportFragmentManager, null)
        }
    }

    /**
     *  ScannerFragment Interface
     */

    override fun onDeviceSelected(device: BluetoothDevice, name: String?) {
        val intent = Intent()
        val overwrittenName = intent.getStringExtra(DfuService.EXTRA_DEVICE_NAME)
        val path = intent.getStringExtra(DfuService.EXTRA_FILE_PATH)
        val initPath = intent.getStringExtra(DfuService.EXTRA_INIT_FILE_PATH)
        val address = device.address
        val finalName = overwrittenName ?: (name ?: getString(R.string.not_available))
        val type = intent.getIntExtra(DfuService.EXTRA_FILE_TYPE, DfuService.TYPE_AUTO)
        val keepBond = intent.getBooleanExtra(DfuService.EXTRA_KEEP_BOND, false)

        // Start DFU service with data provided in the intent
        val service = Intent(this, DfuService::class.java)
        service.putExtra(DfuService.EXTRA_DEVICE_ADDRESS, address)
        service.putExtra(DfuService.EXTRA_DEVICE_NAME, finalName)
        service.putExtra(DfuService.EXTRA_FILE_TYPE, type)
        service.putExtra(DfuService.EXTRA_FILE_PATH, path)
        if (intent.hasExtra(DfuService.EXTRA_INIT_FILE_PATH))
            service.putExtra(DfuService.EXTRA_INIT_FILE_PATH, initPath)
        service.putExtra(DfuService.EXTRA_KEEP_BOND, keepBond)
        service.putExtra(DfuService.EXTRA_UNSAFE_EXPERIMENTAL_BUTTONLESS_DFU, true)
        startService(service)
        finish()
    }

    override fun onDialogCanceled() {
        finish()
    }

}