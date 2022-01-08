package com.app.caloriestomealspedometer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.BulletSpan
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_app_description.*

class AppDescriptionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_description)

        val list= mutableListOf(ada_functionText1,ada_functionText2,ada_functionText3)
        for (functionText in list){
            val text = functionText.text
            val spannableStringBuilder = SpannableStringBuilder("")
            spannableStringBuilder.append(
                text,
                this.let { ContextCompat.getColor(it, R.color.black) }.let {
                    BulletSpan(20,it)
                },
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            functionText.text = spannableStringBuilder
        }


    }
}