
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

import androidx.fragment.app.DialogFragment
import android.os.Bundle
import android.app.Dialog
import android.content.pm.PackageManager
import androidx.annotation.NonNull
import androidx.appcompat.app.AlertDialog


class AppHelpFragment : DialogFragment() {
    private val ARG_TEXT = "ARG_TEXT"
    private val ARG_VERSION = "ARG_VERSION"

    fun getInstance(aboutResId: Int, appendVersion: Boolean): AppHelpFragment {
        val fragment = AppHelpFragment()

        val args = Bundle()
        args.putInt(ARG_TEXT, aboutResId)
        args.putBoolean(ARG_VERSION, appendVersion)
        fragment.arguments = args

        return fragment
    }

    @NonNull
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = arguments
        val text = StringBuilder(getString(args!!.getInt(ARG_TEXT)))

        val appendVersion = args.getBoolean(ARG_VERSION)
        if (appendVersion) {
            try {
                val version = activity!!.packageManager.getPackageInfo(activity!!.packageName, 0).versionName
                text.append(getString(R.string.about_version, version))
            } catch (e: PackageManager.NameNotFoundException) {
                // do nothing
            }

        }
        return AlertDialog.Builder(activity!!).setTitle(R.string.about_title).setMessage(text)
            .setPositiveButton(R.string.ok, null).create()
    }
}

