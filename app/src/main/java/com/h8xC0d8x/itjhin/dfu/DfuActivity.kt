package com.h8xC0d8x.itjhin.dfu

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.app.ActivityManager
import android.database.Cursor
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.fragment.app.DialogFragment


import no.nordicsemi.android.nrftoolbox.scanner.ScannerFragment;

class DfuActivity : AppCompatActivity(), LoaderManager.LoaderCallbacks<Cursor>  {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onRestart() {
        super.onRestart()
    }


    /********************************************************************
     *
     *  Loader Interface
     *
     ********************************************************************/
    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

//public class DfuActivity extends AppCompatActivity implements LoaderCallbacks<Cursor>, ScannerFragment.OnDeviceSelectedListener,
//UploadCancelFragment.CancelFragmentListener, PermissionRationaleFragment.PermissionDialogListener
