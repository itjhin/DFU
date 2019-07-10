package com.h8xC0d8x.itjhin.dfu

import android.content.Context
import androidx.fragment.app.DialogFragment
import android.os.Bundle
import android.app.Dialog
import androidx.annotation.NonNull
import androidx.appcompat.app.AlertDialog
import com.h8xC0d8x.itjhin.dfu.R






class PermissionRationaleFragment : DialogFragment() {

    private val ARG_PERMISSION = "ARG_PERMISSION"
    private val ARG_TEXT = "ARG_TEXT"

    private var mListener : PermissionDialogListener? = null

    interface PermissionDialogListener {
        fun onRequestPermission(permission: String)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is PermissionDialogListener) {
            mListener = context
        } else {
            throw IllegalArgumentException("The parent activity must impelement PermissionDialogListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    fun getInstance(aboutResId: Int, permission: String): PermissionRationaleFragment {
        val fragment = PermissionRationaleFragment()

        val args = Bundle()
        args.putInt(ARG_TEXT, aboutResId)
        args.putString(ARG_PERMISSION, permission)
        fragment.arguments = args

        return fragment
    }

    @NonNull
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = arguments
        val text = StringBuilder(getString(args!!.getInt(ARG_TEXT)))
        return AlertDialog.Builder(this.activity!!).setTitle(R.string.permission_title).setMessage(text)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(
                R.string.ok,
                { dialog, which -> mListener?.onRequestPermission(args.getString(ARG_PERMISSION)!!) }).create()
    }

}