
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

package com.h8xC0d8x.itjhin.dfu.adapter

import android.content.Context
import android.content.res.Resources
import android.widget.BaseAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.h8xC0d8x.itjhin.dfu.R



class FileBrowserAppsAdapter() : BaseAdapter() {

    private var mInflater: LayoutInflater? = null
    private var mResources: Resources? = null

    constructor(context: Context) : this() {
        this.mInflater = LayoutInflater.from(context)
        this.mResources = context.resources
    }


    /**
     *
     */
    override fun getView(position : Int, convertView: View?, parent : ViewGroup?): View {
        var view = convertView
        if (view == null) {
            view = mInflater?.inflate(R.layout.app_file_browser_item, parent, false)
        }

        val item = view as TextView?
        item!!.text = mResources?.getStringArray(R.array.dfu_app_file_browser)!![position]
        item.compoundDrawablesRelative[0].level = position
        return view!!
    }

    override fun getItem(position : Int): Any {
        return mResources?.getStringArray(R.array.dfu_app_file_browser_action)!![position]

    }

    override fun getItemId(position : Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return mResources?.getStringArray(R.array.dfu_app_file_browser)!!.size

    }

}