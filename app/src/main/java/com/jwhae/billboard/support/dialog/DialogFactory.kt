package com.nota.nota_face_baseline.dialog

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import androidx.annotation.NonNull

class DialogFactory private constructor(@NonNull val act : Activity) {
    private var title : String = ""
    private var layoutId = android.R.style.Theme_DeviceDefault_Light_Dialog
    private var usePostiiveBtn = true
    private var useNegativeBtn = true
    private var positiveBtnListener : DialogInterface.OnClickListener? = null
    private var negativeBtnListener : DialogInterface.OnClickListener? = null

    internal fun setTitle(@NonNull title : String) : DialogFactory {
        this.title = title
        return this
    }

    internal fun setLayout(@NonNull layoutId : Int) : DialogFactory {
        this.layoutId = layoutId
        return this
    }

    internal fun usePositiveButton(@NonNull use : Boolean) : DialogFactory {
        usePostiiveBtn = use
        return this
    }

    internal fun useNegativeButton(@NonNull use : Boolean) : DialogFactory {
        useNegativeBtn = use
        return this
    }

    internal fun setPositiveButtonListener(@NonNull listener :DialogInterface.OnClickListener) : DialogFactory {
        usePostiiveBtn = true
        positiveBtnListener = listener
        return this
    }

    internal fun setNegativeButtonListener(@NonNull listener :DialogInterface.OnClickListener) : DialogFactory {
        useNegativeBtn = true
        negativeBtnListener = listener
        return this
    }

    fun build() : Dialog {
        val posListener = positiveBtnListener ?:
        object : DialogInterface.OnClickListener{
            override fun onClick(dialog: DialogInterface?, which: Int) {
                act.finish()
                System.exit(0)
            }
        }

        val negListener = negativeBtnListener ?:
        object : DialogInterface.OnClickListener{
            override fun onClick(dialog: DialogInterface?, which: Int) {
                dialog?.cancel()
            }
        }

        val builder = AlertDialog.Builder(act, layoutId)
        builder.setTitle(title)
        builder.setCancelable(false)

        if(usePostiiveBtn){
            builder.setPositiveButton("확인", posListener)
        }

        if(useNegativeBtn){
            builder.setNegativeButton("취소", negListener)
        }
        return builder.create()
    }

    companion object{
        fun getInstance(@NonNull act : Activity) : DialogFactory {
            return getInstance(act)
        }
    }
}