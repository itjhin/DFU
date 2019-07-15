
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

package com.h8xC0d8x.itjhin.dfu.settings

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.h8xC0d8x.itjhin.dfu.R
import no.nordicsemi.android.dfu.DfuSettingsConstants
import android.os.Build
import android.text.TextUtils
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceScreen
import no.nordicsemi.android.dfu.DfuServiceInitiator
import androidx.preference.Preference


class SettingsFragment : PreferenceFragmentCompat(), DfuSettingsConstants,
    SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        const val SETTINGS_KEEP_BOND = "settings_keep_bond"
        const val SETTINGS_ASSUME_DFU_NODE = DfuSettingsConstants.SETTINGS_ASSUME_DFU_NODE
        const val SETTINGS_PACKET_RECEIPT_NOTIFICATION_ENABLED = DfuSettingsConstants.SETTINGS_PACKET_RECEIPT_NOTIFICATION_ENABLED
        const val SETTINGS_NUMBER_OF_PACKETS = DfuSettingsConstants.SETTINGS_NUMBER_OF_PACKETS
        const val SETTINGS_MBR_SIZE = DfuSettingsConstants.SETTINGS_MBR_SIZE
        const val SETTINGS_NUMBER_OF_PACKETS_DEFAULT = DfuSettingsConstants.SETTINGS_NUMBER_OF_PACKETS_DEFAULT
    }


    override fun onResume() {
        super.onResume()

        // attach the preference change listener. It will update the summary below interval preference
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()

        // unregister listener
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    /**
     *  PreferenceFragmentCompat interface
     */
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings_dfu);

        // set initial values
        updateNumberOfPacketsSummary();
        updateMBRSize();
    }


    /**
     *  SharedPreferences interface
     */

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

        val preferences = preferenceManager.sharedPreferences

        if (SETTINGS_PACKET_RECEIPT_NOTIFICATION_ENABLED == key) {
            val disabled =
                !preferences.getBoolean(SETTINGS_PACKET_RECEIPT_NOTIFICATION_ENABLED, true)
            if (disabled && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                AlertDialog.Builder(requireContext()).setMessage(R.string.dfu_settings_dfu_number_of_packets_info)
                    .setTitle(R.string.dfu_settings_dfu_information)
                    .setPositiveButton(R.string.ok, null).show()
            }
        } else if (SETTINGS_NUMBER_OF_PACKETS == key) {
            updateNumberOfPacketsSummary()
        } else if (SETTINGS_MBR_SIZE == key) {
            updateMBRSize()
        } else if (SETTINGS_ASSUME_DFU_NODE == key && sharedPreferences!!.getBoolean(key, false)) {
            AlertDialog.Builder(requireContext()).setMessage(R.string.dfu_settings_dfu_assume_dfu_mode_info)
                .setTitle(R.string.dfu_settings_dfu_information)
                .setPositiveButton(R.string.ok, null)
                .show()
        }
    }

    private fun updateNumberOfPacketsSummary() {
        val screen : PreferenceScreen = preferenceScreen
        val preferences : SharedPreferences = preferenceManager.sharedPreferences

        var value : String? = preferences.getString(
            SETTINGS_NUMBER_OF_PACKETS,
            SETTINGS_NUMBER_OF_PACKETS_DEFAULT.toString()
        )

        // Security check
        if (TextUtils.isEmpty(value)) {
            value = SETTINGS_NUMBER_OF_PACKETS_DEFAULT.toString()
            preferences.edit().putString(SETTINGS_NUMBER_OF_PACKETS, value).apply()
        }
        screen.findPreference<Preference>(SETTINGS_NUMBER_OF_PACKETS)!!.summary = value

        val valueInt = Integer.parseInt(value!!)
        if (valueInt > 200 && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            AlertDialog.Builder(requireContext()).setMessage(R.string.dfu_settings_dfu_number_of_packets_info)
                .setTitle(R.string.dfu_settings_dfu_information)
                .setPositiveButton(R.string.ok, null)
                .show()
        }
    }

    private fun updateMBRSize() {
        val screen = preferenceScreen
        val preferences = preferenceManager.sharedPreferences

        val value = preferences.getString(
            SETTINGS_MBR_SIZE,
            DfuServiceInitiator.DEFAULT_MBR_SIZE.toString()
        )
        screen.findPreference<Preference>(DfuSettingsConstants.SETTINGS_MBR_SIZE)!!.summary = value
    }

}
