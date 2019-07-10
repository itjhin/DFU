
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
import android.app.Activity
import android.util.Log
import android.app.Dialog
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.os.Bundle
import androidx.annotation.NonNull
import androidx.appcompat.app.AlertDialog
import android.content.DialogInterface



/**
 * When cancel button is pressed during uploading this fragment shows uploading cancel dialog
 */

class UploadCancelFragment : DialogFragment() {
    private val TAG = "UploadCancelFragment"

    private var mListener: CancelFragmentListener? = null

    interface CancelFragmentListener {
        fun onCancelUpload()
    }

    fun getInstance(): UploadCancelFragment {
        return UploadCancelFragment()
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity!!)

        try {
            mListener = activity as CancelFragmentListener?
        } catch (e: ClassCastException) {
            Log.d(TAG, "The parent Activity must implement CancelFragmentListener interface")
        }

    }

    override fun onCancel(dialog: DialogInterface?) {
        val manager = LocalBroadcastManager.getInstance(activity!!)
        val pauseAction = Intent(DfuService.BROADCAST_ACTION)
        pauseAction.putExtra(DfuService.EXTRA_ACTION, DfuService.ACTION_RESUME)
        manager.sendBroadcast(pauseAction)
    }

    @NonNull
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity!!).setTitle(R.string.dfu_confirmation_dialog_title)
            .setMessage(R.string.dfu_upload_dialog_cancel_message).setCancelable(false)
            .setPositiveButton(R.string.yes, { dialog, whichButton ->
                val manager = LocalBroadcastManager.getInstance(activity!!)
                val pauseAction = Intent(DfuService.BROADCAST_ACTION)
                pauseAction.putExtra(DfuService.EXTRA_ACTION, DfuService.ACTION_ABORT)
                manager.sendBroadcast(pauseAction)

                mListener?.onCancelUpload()
            }).setNegativeButton(R.string.no, { dialog, which -> dialog.cancel() }).create()
    }


}