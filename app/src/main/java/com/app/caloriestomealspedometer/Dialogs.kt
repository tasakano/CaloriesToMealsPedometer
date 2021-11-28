package com.app.caloriestomealspedometer

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment


class SingOutConfirmationDialog(emailAddress:String) : DialogFragment(){
    interface Listener{
        fun Yes()
        fun No()
    }
    private var listener: Listener? = null
    private var mail = emailAddress

    override fun onAttach(context: Context) {
        super.onAttach(context)
        when (context){
            is Listener -> listener = context
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setMessage("$mail\n\n現在ログインしてるアカウントからサインアウトしますか？")
        builder.setNegativeButton("はい") { _, _ ->
            listener?.Yes()
        }
        builder.setPositiveButton("いいえ") { _, _ ->
            listener?.No()
        }
        return builder.create()
    }
}