
/*************************************************************************************************************************************************
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
 ************************************************************************************************************************************************/

package com.h8xC0d8x.itjhin.dfu.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import android.graphics.drawable.NinePatchDrawable
import android.graphics.drawable.Drawable
import android.graphics.Rect
import android.graphics.Canvas
import android.os.Build
import android.annotation.TargetApi

import com.h8xC0d8x.itjhin.dfu.R



class ForegroundRelativeLayout : RelativeLayout {


    private var mForegroundSelector: Drawable? = null
    private var mRectPadding: Rect? = null
    private var mUseBackgroundPadding : Boolean = false

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs, 0)

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        val a = context?.obtainStyledAttributes(
            attrs, R.styleable.ForegroundRelativeLayout,
            defStyle, 0
        )

        val d = a?.getDrawable(R.styleable.ForegroundRelativeLayout_foreground)
        if (d != null) {
            foreground = d
        }

        a?.recycle()

        if (this.background is NinePatchDrawable) {
            val npd = this.background as NinePatchDrawable
            mRectPadding = Rect()
            if (npd.getPadding(mRectPadding!!)) {
                mUseBackgroundPadding = true
            }
        }

    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()

        if (mForegroundSelector != null && mForegroundSelector!!.isStateful) {
            mForegroundSelector!!.state = drawableState
        }
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (mForegroundSelector != null) {
            if (mUseBackgroundPadding) {
                mForegroundSelector!!.setBounds(
                    mRectPadding?.left!!,
                    mRectPadding?.top!!,
                    w - mRectPadding?.right!!,
                    h - mRectPadding?.bottom!!
                )
            } else {
                mForegroundSelector!!.setBounds(0, 0, w, h)
            }
        }
    }


    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)

        if (mForegroundSelector != null) {
            mForegroundSelector!!.draw(canvas)
        }
    }

    override fun verifyDrawable(who: Drawable): Boolean {
        return super.verifyDrawable(who) || who === mForegroundSelector
    }

    override fun jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState()
        if (mForegroundSelector != null) mForegroundSelector!!.jumpToCurrentState()
    }

    override fun setForeground(drawable: Drawable?) {
        if (mForegroundSelector !== drawable) {
            if (mForegroundSelector != null) {
                mForegroundSelector!!.setCallback(null)
                unscheduleDrawable(mForegroundSelector)
            }

            mForegroundSelector = drawable

            if (drawable != null) {
                setWillNotDraw(false)
                drawable.callback = this
                if (drawable.isStateful) {
                    drawable.state = drawableState
                }
            } else {
                setWillNotDraw(true)
            }
            requestLayout()
            invalidate()
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun drawableHotspotChanged(x: Float, y: Float) {
        super.drawableHotspotChanged(x, y)
        if (mForegroundSelector != null) {
            mForegroundSelector!!.setHotspot(x, y)
        }
    }
}

