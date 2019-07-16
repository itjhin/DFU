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
package com.h8xC0d8x.itjhin.dfu.scanner

import android.content.Context
import android.widget.BaseAdapter
import android.bluetooth.BluetoothDevice;
import android.util.Log
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import com.h8xC0d8x.itjhin.dfu.R
import android.widget.TextView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView


/**
 * DeviceListAdapter class is list adapter for showing scanned Devices name, address and RSSI image based on RSSI values.
 */
class DeviceListAdapter() : BaseAdapter() {

    private val TAG : String = "DeviceListAdapter"

    private val TYPE_TITLE = 0
    private val TYPE_ITEM = 1
    private val TYPE_EMPTY = 2

    private val mListBondedValues = ArrayList<ExtendedBluetoothDevice>()
    private val mListValues = ArrayList<ExtendedBluetoothDevice>()
    private var mContext: Context? = null

    constructor(context: Context?) : this() {
        this.mContext = context
    }

    /**
     * Sets a list of bonded devices.
     * @param devices list of bonded devices.
     */
    fun addBondedDevices(devices: Set<BluetoothDevice>) {
        val bondedDevices = mListBondedValues
        for (device in devices) {
            bondedDevices.add(ExtendedBluetoothDevice(device))
        }
        notifyDataSetChanged()
    }
    /**
     * Updates the list of not bonded devices.
     * @param results list of results from the scanner
     */
    fun update(results: List<ScanResult>) {
        for (result in results) {
            val device = findDevice(result)
            if (device == null) {
                mListValues.add(ExtendedBluetoothDevice(result))
            } else {
                device!!.name = if (result.scanRecord != null) result.scanRecord!!.deviceName else null
                device!!.rssi = result.rssi
            }
        }
        notifyDataSetChanged()
    }

    private fun findDevice(result: ScanResult): ExtendedBluetoothDevice? {
        for (device in mListBondedValues)
            if (device.matches(result))
                return device
        for (device in mListValues)
            if (device.matches(result))
                return device
        return null
    }

    fun clearDevices() {
        mListValues.clear()
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        val bondedCount = mListBondedValues.size + 1 // 1 for the title
        val availableCount = if (mListValues.isEmpty()) 2 else mListValues.size+ 1 // 1 for title, 1 for empty text
        return if (bondedCount == 1) availableCount else bondedCount + availableCount
    }


    override fun getItem(position: Int): Any {
        val bondedCount = mListBondedValues.size + 1 // 1 for the title
        if (mListBondedValues.isEmpty()) {
            return if (position == 0)
                R.string.scanner_subtitle_not_bonded
            else
                mListValues[position - 1]
        } else {
            if (position == 0)
                return R.string.scanner_subtitle_bonded
            if (position < bondedCount)
                return mListBondedValues[position - 1]
            return if (position == bondedCount) R.string.scanner_subtitle_not_bonded else mListValues[position - bondedCount - 1]
        }
    }

    override fun getViewTypeCount(): Int {
        return 3
    }

    override fun areAllItemsEnabled(): Boolean {
        return false
    }

    override fun isEnabled(position: Int): Boolean {
        return getItemViewType(position) == TYPE_ITEM
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0)
            return TYPE_TITLE

        if (!mListBondedValues.isEmpty() && position == mListBondedValues.size + 1)
            return TYPE_TITLE

        return if (position == count - 1 && mListValues.isEmpty()) TYPE_EMPTY else TYPE_ITEM

    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, oldView: View?, parent: ViewGroup): View {
        val inflater = LayoutInflater.from(mContext)
        val type = getItemViewType(position)

        var view: View? = oldView
        when (type) {
            TYPE_EMPTY -> if (view == null) {
                view = inflater.inflate(R.layout.device_list_empty, parent, false)
            }
            TYPE_TITLE -> {
                if (view == null) {
                    view = inflater.inflate(R.layout.device_list_title, parent, false)
                }
                val title = view as TextView?
                title!!.setText(getItem(position) as Int)
            }
            else -> {
                if (view == null) {
                    view = inflater.inflate(R.layout.device_list_row, parent, false)
                    val holder = ViewHolder()
                    holder.name = view!!.findViewById(R.id.name)
                    holder.address = view!!.findViewById(R.id.address)
                    holder.rssi = view!!.findViewById(R.id.rssi)
                    view!!.setTag(holder)
                }

                val device = getItem(position) as ExtendedBluetoothDevice
                val holder = view!!.getTag() as ViewHolder
                val name = device.name
                holder.name?.setText(name ?: mContext?.getString(R.string.not_available))
                holder.address?.setText(device.device!!.address)
                if (!device.isBonded || device.rssi != ExtendedBluetoothDevice.NO_RSSI) {
                    val rssiPercent = (100.0f * (127.0f + device.rssi) / (127.0f + 20.0f)).toInt()
                    holder.rssi?.setImageLevel(rssiPercent)
                    holder.rssi?.setVisibility(View.VISIBLE)
                } else {
                    holder.rssi?.setVisibility(View.GONE)
                }
            }
        }

        return view!!
    }

    private inner class ViewHolder {
        var name: TextView? = null
        var address: TextView? = null
        var rssi: ImageView? = null
    }
}