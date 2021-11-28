package com.app.caloriestomealspedometer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.yodo1.mas.Yodo1Mas
import com.yodo1.mas.error.Yodo1MasError
import com.yodo1.mas.event.Yodo1MasAdEvent
import io.realm.Realm
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_food_list.*
import kotlinx.android.synthetic.main.content_food_list.*

class FoodListActivity : AppCompatActivity() {
    private lateinit var realm: Realm

    var app: CaloriesToMealsPedometerApplication? = null
    var layAd: LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_food_list)

//        val align = Yodo1Mas.BannerBottom or Yodo1Mas.BannerHorizontalCenter
//        Yodo1Mas.getInstance().showBannerAd(this, align)

        layAd = findViewById<View>(R.id.layad) as LinearLayout

        app = applicationContext  as CaloriesToMealsPedometerApplication
        app!!.loadAd(layAd!!)

        realm = Realm.getDefaultInstance()
        list.layoutManager = LinearLayoutManager(this)
        val foodList = realm.where<Food>().findAll()
        val adapter = FoodAdapter(foodList,this)
        list.adapter = adapter

        //食事データが一つも登録されていない場合は、登録を促すテキスト表示する
        if (foodList.size == 0){
            fla_addPromptText.visibility = View.VISIBLE
        }

        //追加ボタンを押したとき
        fab.setOnClickListener {
            val intent = Intent(this, FoodEditActivity::class.java)
            startActivity(intent)
        }

        //←ボタンを押したとき
        fla_backImageButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        adapter.setOnItemClickListener { id ->
            val intent = Intent(this, FoodEditActivity::class.java)
                .putExtra("food_id", id)
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }

    //戻るボタンが押された場合の処理
    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

}