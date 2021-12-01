package com.app.caloriestomealspedometer

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.yodo1.mas.Yodo1Mas
import com.yodo1.mas.error.Yodo1MasError
import com.yodo1.mas.event.Yodo1MasAdEvent
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_food_edit.*
import java.io.FileDescriptor
import java.io.IOException


class FoodEditActivity : AppCompatActivity() {
    private lateinit var realm: Realm
    private val RESULT_PICK_IMAGEFILE = 1000

    var app: CaloriesToMealsPedometerApplication? = null
    var layAd: LinearLayout? = null

    private var mInterstitialAd: InterstitialAd? = null

    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
        val fileDescriptor: FileDescriptor = parcelFileDescriptor!!.fileDescriptor
        val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
        parcelFileDescriptor.close()
        return image
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode === RESULT_PICK_IMAGEFILE && resultCode === RESULT_OK) {

            val uri: Uri?
            if (data != null) {
                uri = data.data
                //URLをTextViewに保存しておく　このTextViewは非表示にしてあり画面上では見えない
                fea_imagePathText.text = uri.toString()
                try {
                    val bmp: Bitmap? = uri?.let { getBitmapFromUri(it) }
                    fea_foodImage.setImageBitmap(bmp)
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(this,"画像の読み込みに失敗しました", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_food_edit)
        realm = Realm.getDefaultInstance()

        /*yodo1の広告
        val align = Yodo1Mas.BannerBottom or Yodo1Mas.BannerHorizontalCenter
        Yodo1Mas.getInstance().showBannerAd(this, align)

        val interstitialListener: Yodo1Mas.InterstitialListener = object : Yodo1Mas.InterstitialListener() {
            override fun onAdOpened(event: Yodo1MasAdEvent) {

            }
            override fun onAdError(event: Yodo1MasAdEvent, error: Yodo1MasError) {
                //Toast.makeText(this, error.message, Toast.LENGTH_SHORT).show()
            }
            override fun onAdClosed(event: Yodo1MasAdEvent) {
                toFoodListActivity()
            }
        }
        Yodo1Mas.getInstance().setInterstitialListener(interstitialListener)

        val isAdLoaded = Yodo1Mas.getInstance().isInterstitialAdLoaded
        */


        layAd = findViewById<View>(R.id.layad) as LinearLayout

        app = applicationContext  as CaloriesToMealsPedometerApplication
        app!!.loadAd(layAd!!)

        //全画面広告の設定
        val extras = Bundle()
        extras.putString("max_ad_content_rating", "G")
        val adRequest = AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter::class.java, extras).build()
        InterstitialAd.load(this,getString(R.string.interstitial_ad_id), adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                mInterstitialAd = interstitialAd
                mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        toFoodListActivity()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                    }

                    override fun onAdShowedFullScreenContent() {
                        mInterstitialAd = null
                    }
                }
            }
        })

        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = pref.edit()

        //共有プリファレンスから練習を実行した回数を取得する
        var saveNum = pref.getInt("saveNum",0)

        val checkFoodId = pref.getLong("CHECK_FOOD_ID",-1)

        val foodId = intent?.getLongExtra("food_id", -1L)
        if (foodId != -1L) {
            val food = realm.where<Food>()
                .equalTo("id", foodId).findFirst()
            fea_editName.setText(food?.name)
            fea_editUnit.setText(food?.unit)
            fea_editCalorie.setText(food?.calorie.toString())
            fea_imagePathText.text = food?.imagePath
            val uri = Uri.parse(food?.imagePath)
            try {
                val bmp: Bitmap? = uri?.let { getBitmapFromUri(it) }
                fea_foodImage.setImageBitmap(bmp)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            fea_deleteButton.visibility = View.VISIBLE
        } else {
            fea_deleteButton.visibility = View.INVISIBLE
        }

        //保存ボタンを押したとき
        fea_saveButton.setOnClickListener {
            if (fea_editName.text.toString() == ""){
                Toast.makeText(this,"名前を入力してください", Toast.LENGTH_SHORT).show()
            } else if (fea_editUnit.text.toString() == ""){
                Toast.makeText(this,"単位を入力してください", Toast.LENGTH_SHORT).show()
            } else if (fea_editCalorie.text.toString() == "" || fea_editCalorie.text.toString().toLong() == 0L){
                Toast.makeText(this,"カロリーに1以上の値を入力してください", Toast.LENGTH_SHORT).show()
            } else {
                when (foodId) {
                    -1L -> {
                        /*食事データを新規追加するとき*/
                        realm.beginTransaction()
                        val maxId = realm.where<Food>().max("id")
                        val nextId = (maxId?.toLong() ?: 0L) + 1
                        val food = realm.createObject<Food>(nextId)
                        food.name = fea_editName.text.toString()
                        food.unit = fea_editUnit.text.toString()
                        food.calorie = fea_editCalorie.text.toString().toLong()
                        food.imagePath = fea_imagePathText.text.toString()
                        realm.commitTransaction()

                        Toast.makeText(this,"追加しました", Toast.LENGTH_SHORT).show()
                        fea_saveButton.visibility = View.INVISIBLE
                        fea_deleteButton.visibility = View.INVISIBLE
                        //1.5秒後にFoodListActivityに遷移する
                        Handler(Looper.getMainLooper()).postDelayed({
                            if(saveNum >= 4){
                                //saveNumが4以上の場合に（保存ボタンが5回押されるたび）に全画面広告を表示する
                                if (mInterstitialAd != null) {
                                    editor.putInt("saveNum",0).apply()
                                    mInterstitialAd?.show(this)
                                } else {
                                    toFoodListActivity()
                                }
//                                if (isAdLoaded) {
//                                    editor.putInt("saveNum",0).apply()
//                                    Yodo1Mas.getInstance().showInterstitialAd(this)
//                                } else {
//                                    toFoodListActivity()
//                                }
                            } else {
                                saveNum++
                                editor.putInt("saveNum",saveNum).apply()

                                toFoodListActivity()
                            }
                        }, 1500)

                    }
                    else -> {
                        /*すでに追加してある食事データを編集するとき*/

                       //もしその食事がチェック付きの場合、共有プリファレンスのデータを更新する
                        if (foodId == checkFoodId){
                            editor.putString("CHECK_FOOD_IMAGE_PATH",fea_imagePathText.text.toString()).apply()
                            editor.putLong("CHECK_CALORIE", fea_editCalorie.text.toString().toLong()).apply()
                            editor.putString("CHECK_NAME", fea_editName.text.toString()).apply()
                            editor.putString("CHECK_UNIT", fea_editUnit.text.toString()).apply()
                        }
                        realm.beginTransaction()
                        val food = realm.where<Food>()
                            .equalTo("id", foodId).findFirst()
                        if (food != null){
                            food.name = fea_editName.text.toString()
                            food.unit = fea_editUnit.text.toString()
                            food.calorie = fea_editCalorie.text.toString().toLong()
                            food.imagePath = fea_imagePathText.text.toString()
                        }
                        realm.commitTransaction()

                        Toast.makeText(this,"保存しました", Toast.LENGTH_SHORT).show()
                        fea_saveButton.visibility = View.INVISIBLE
                        fea_deleteButton.visibility = View.INVISIBLE
                        //1.5秒後にFoodListActivityに遷移する
                        Handler(Looper.getMainLooper()).postDelayed({
                            if(saveNum >= 4){
                                //saveNumが4以上の場合に（保存ボタンが5回押されるたび）に全画面広告を表示する
                                if (mInterstitialAd != null) {
                                    editor.putInt("saveNum",0).apply()
                                    mInterstitialAd?.show(this)
                                } else {
                                    toFoodListActivity()
                                }
//                                if (isAdLoaded) {
//                                    editor.putInt("saveNum",0).apply()
//                                    Yodo1Mas.getInstance().showInterstitialAd(this)
//                                } else {
//                                    toFoodListActivity()
//                                }
                            } else {
                                saveNum++
                                editor.putInt("saveNum",saveNum).apply()

                                toFoodListActivity()
                            }
                        }, 1500)
                    }
                }
            }
        }

        //削除ボタンを押したとき
        fea_deleteButton.setOnClickListener {
            if (foodId == checkFoodId){
                editor.putLong("CHECK_FOOD_ID",-1).apply()
                editor.putString("CHECK_FOOD_IMAGE_PATH","default").apply()
                editor.putLong("CHECK_CALORIE", 0L).apply()
                editor.putString("CHECK_NAME", "").apply()
                editor.putString("CHECK_UNIT", "").apply()
            }

            realm.beginTransaction()
            realm.where<Food>().equalTo("id", foodId)?.findFirst()?.deleteFromRealm()
            realm.commitTransaction()

            Toast.makeText(this,"削除しました", Toast.LENGTH_SHORT).show()
            fea_saveButton.visibility = View.INVISIBLE
            fea_deleteButton.visibility = View.INVISIBLE
            //1.5秒後にFoodListActivityに遷移する
            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(this, FoodListActivity::class.java)
                startActivity(intent)
            }, 1500)

        }

        //画像ファイルを選択するボタンを押したとき
        fea_imageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            startActivityForResult(intent, RESULT_PICK_IMAGEFILE)
        }

        //←ボタンを押したとき
        fea_backImageButton.setOnClickListener {
            val intent = Intent(this, FoodListActivity::class.java)
            startActivity(intent)
        }

    }

    fun toFoodListActivity() {
        val intent = Intent(this, FoodListActivity::class.java)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }

    //戻るボタンが押された場合の処理
    override fun onBackPressed() {
        val intent = Intent(this, FoodListActivity::class.java)
        startActivity(intent)
    }
}