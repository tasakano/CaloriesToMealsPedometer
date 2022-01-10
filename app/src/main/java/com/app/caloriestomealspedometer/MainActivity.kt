package com.app.caloriestomealspedometer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataSource
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.tasks.Task
import com.yodo1.mas.Yodo1Mas
import com.yodo1.mas.error.Yodo1MasError
import com.yodo1.mas.event.Yodo1MasAdEvent
import kotlinx.android.synthetic.main.activity_main.*
import java.io.FileDescriptor
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(),SingOutConfirmationDialog.Listener {

    override fun Yes() {
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = pref.edit()

        mGoogleSignInClient.signOut()
        editor.putInt("isSingIn",0).apply()

        //通知がセットされていたらキャンセルする
        if (pref.getInt("isAlarmSet",0) == 1){
            val intent = Intent(applicationContext, AlarmNotification::class.java)
            intent.putExtra("RequestCode", reqCode)
            pending = PendingIntent.getBroadcast(applicationContext, reqCode, intent, PendingIntent.FLAG_MUTABLE)
            am = getSystemService(ALARM_SERVICE) as AlarmManager
            am!!.cancel(pending)
            editor.putInt("isAlarmSet",0).apply()
        }

        val intent = Intent(this, GoogleSinginActivity::class.java)
        startActivity(intent)
    }

    override fun No() {
        //何もしない
    }

    private val RC_SIGN_IN = 1
    private val REQUEST_OAUTH_REQUEST_CODE = 2
    private val RESULT_PICK_IMAGEFILE = 1000

    private lateinit var mGoogleSignInClient : GoogleSignInClient

    // Configure sign-in to request the user's ID, email address, and basic
    // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
    var gso: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build()

    private lateinit var account: GoogleSignInAccount

    private fun signIn() {
        val signInIntent: Intent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)

        }

        if (requestCode === RESULT_PICK_IMAGEFILE && resultCode === RESULT_OK) {
            val pref = PreferenceManager.getDefaultSharedPreferences(this)
            val editor = pref.edit()
            val uri: Uri?
            if (data != null) {
                uri = data.data
                //URLを共有プリファレンスに保存
                editor.putString("URL",uri.toString()).apply()
                try {
                    val bmp: Bitmap? = uri?.let { getBitmapFromUri(it) }
                    ma_foodImageView.setImageBitmap(bmp)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
        val fileDescriptor: FileDescriptor = parcelFileDescriptor!!.fileDescriptor
        val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
        parcelFileDescriptor.close()
        return image
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            account = completedTask.getResult(ApiException::class.java)

            // Signed in successfully, show authenticated UI.

            //右上のアカウントボタンを表示する
            ma_googleSingOutButton.visibility = View.VISIBLE

            //歩数と消費カロリーのデータをとってきて、画面に表示する
            displayHealthData()

        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(ContentValues.TAG, "signInResult:failed code=" + e.statusCode)
            Toast.makeText(this,"Googleアカウントとの連携に失敗しました", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayHealthData(){

        val fitnessOptions = FitnessOptions.builder()
            .addDataType(DataType.TYPE_CALORIES_EXPENDED)
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA)
            .addDataType(DataType.AGGREGATE_CALORIES_EXPENDED)
            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA)
            .addDataType(DataType.TYPE_DISTANCE_DELTA)
            .addDataType(DataType.AGGREGATE_DISTANCE_DELTA)
            .addDataType(DataType.TYPE_MOVE_MINUTES)
            .addDataType(DataType.AGGREGATE_MOVE_MINUTES)
            .build()

        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)) {
            GoogleSignIn.requestPermissions(this, REQUEST_OAUTH_REQUEST_CODE, GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)
        }

        val startTime = LocalDate.now().atStartOfDay(ZoneId.systemDefault())
        val endTime = LocalDateTime.now().atZone(ZoneId.systemDefault())

        val datasource = DataSource.Builder()
            .setAppPackageName("com.google.android.gms")
            .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
            .setType(DataSource.TYPE_DERIVED)
            .setStreamName("estimated_steps")
            .build()

        val request = DataReadRequest.Builder()
            .aggregate(datasource)
            .bucketByTime(1, TimeUnit.DAYS)
            .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
            .build()

        Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
            .readData(request)
            .addOnSuccessListener { response ->
                try {
                    val totalSteps = response.buckets.flatMap { it.dataSets }.flatMap { it.dataPoints }.sumBy { it.getValue(Field.FIELD_STEPS).asInt() }
                    Log.i(ContentValues.TAG, "Total steps: $totalSteps")
                    ma_stepsText.text = totalSteps.toString()
                } catch (ex: Exception){
                    Toast.makeText(this,"[FIELD_STEPS]:Exception ${ex.message}", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener {  e ->
                Toast.makeText(this,"[FIELD_STEPS]:FailureListener ${e.message}", Toast.LENGTH_LONG).show()
            }


        Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
            .readDailyTotal(DataType.TYPE_DISTANCE_DELTA)
            .addOnSuccessListener { result ->
                try {
                    var totalDistances =
                        result.dataPoints.firstOrNull()?.getValue(Field.FIELD_DISTANCE)?.asFloat() ?: 0
                    // totalDistancesをDouble型に変換して、小数点第三位を四捨五入して、メートルからキロメートルに変換する
                    totalDistances = totalDistances.toDouble()
                    totalDistances /= 1000.0
                    totalDistances = (Math.round(totalDistances * 100.0) / 100.0)
                    ma_distanceText.text = "%.2f".format(totalDistances)
                } catch (ex: Exception) {
                    Log.d(ContentValues.TAG, "Exception ${ex.message}")
                    Toast.makeText(this,"[TYPE_DISTANCE_DELTA]:Exception ${ex.message}", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Log.i(ContentValues.TAG, "There was a problem getting steps.", e)
                Toast.makeText(this,"[TYPE_DISTANCE_DELTA]:FailureListener ${e.message}", Toast.LENGTH_LONG).show()
            }

        Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
            .readDailyTotal(DataType.TYPE_MOVE_MINUTES)
            .addOnSuccessListener { result ->
                try {
                    val totalMoveMinutes =
                        result.dataPoints.firstOrNull()?.getValue(Field.FIELD_DURATION)?.asInt() ?: 0
                    ma_moveMinutesText.text = totalMoveMinutes.toString()
                } catch (ex: Exception) {
                    Log.d(ContentValues.TAG, "Exception ${ex.message}")
                    Toast.makeText(this,"[TYPE_MOVE_MINUTES]:Exception ${ex.message}", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Log.i(ContentValues.TAG, "There was a problem getting steps.", e)
                Toast.makeText(this,"[TYPE_MOVE_MINUTES]:FailureListener ${e.message}", Toast.LENGTH_LONG).show()
            }

        Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
            .readDailyTotal(DataType.TYPE_CALORIES_EXPENDED)
            .addOnSuccessListener { result ->
                try {
                    var totalCalories =
                        result.dataPoints.firstOrNull()?.getValue(Field.FIELD_CALORIES)?.asFloat() ?: 0
                    // totalCaloriesをDouble型に変換して、小数点第二位を四捨五入する
                    totalCalories = totalCalories.toDouble()
                    totalCalories = (Math.round(totalCalories * 10.0) / 10.0)
                    ma_caloriesText.text = totalCalories.toString()

                    //食べ物画像を表示して、プログレスバーを非表示にする
                    ma_foodImageView.visibility = View.VISIBLE
                    progressBar.visibility = View.INVISIBLE

                    val pref = PreferenceManager.getDefaultSharedPreferences(this)
                    //共有プリファレンスからリストでチェックしている食事の情報を取得する
                    ma_foodText.text = pref.getString("CHECK_NAME","")
                    val checkCalories = pref.getLong("CHECK_CALORIE",0L)
                    val checkUnit = pref.getString("CHECK_UNIT","")
                    if (checkCalories != 0L) {
                        ma_caloriesConsumedTodayText.visibility = View.VISIBLE
                        ma_foodUnitText.visibility = View.VISIBLE
                        //消費カロリーが選んだ食事何杯分かを計算して、小数点第二位を四捨五入する
                        val mealEquivalents =
                            (Math.round((totalCalories / checkCalories) * 10.0) / 10.0)
                        ma_numberText.text = mealEquivalents.toString()
                        ma_foodUnitText.text = checkUnit + "分です"
                    } else {
                        //食べ物が登録or選択されていない場合は、追加を促すテキストを表示する
                        ma_additionText.visibility = View.VISIBLE
                    }
                } catch (ex: Exception) {
                    Log.d(ContentValues.TAG, "Exception ${ex.message}")
                    Toast.makeText(this,"[TYPE_CALORIES_EXPENDED]:Exception ${ex.message}", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Log.i(ContentValues.TAG, "There was a problem getting steps.", e)
                Toast.makeText(this,"[TYPE_CALORIES_EXPENDED]:FailureListener ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    var app: CaloriesToMealsPedometerApplication? = null
    var layAd: LinearLayout? = null

    private var am: AlarmManager? = null
    private var pending: PendingIntent? = null
    private val reqCode = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setTheme(R.style.Theme_CaloriesToMealsPedometer)
        setContentView(R.layout.activity_main)

//        //MAS SDKの初期化
//        Yodo1Mas.getInstance().init(this, "NYukMZDqxN", object : Yodo1Mas.InitListener {
//            override fun onMasInitSuccessful() {
//                //Toast.makeText(this@MainActivity, "[Yodo1 Mas] Successful initialization", Toast.LENGTH_SHORT).show()
//            }
//            override fun onMasInitFailed(error: Yodo1MasError) {
//                //Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_SHORT).show()
//            }
//        })
//
//        //Banner Ad Delegate Methodのセットアップ
//        val bannerListener: Yodo1Mas.BannerListener = object : Yodo1Mas.BannerListener() {
//            override fun onAdOpened(event: Yodo1MasAdEvent) {
//
//            }
//            override fun onAdError(event: Yodo1MasAdEvent, error: Yodo1MasError) {
//                //Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_SHORT).show()
//            }
//            override fun onAdClosed(event: Yodo1MasAdEvent) {
//
//            }
//        }
//        Yodo1Mas.getInstance().setBannerListener(bannerListener)
//
//        val align = Yodo1Mas.BannerBottom or Yodo1Mas.BannerHorizontalCenter
//        Yodo1Mas.getInstance().showBannerAd(this, align)

        //広告のセットアップ
        MobileAds.initialize(
            this
        ) { }
        layAd = findViewById<View>(R.id.layad) as LinearLayout
        app = applicationContext  as CaloriesToMealsPedometerApplication
        app!!.loadAd(layAd!!)


        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = pref.edit()

        val isSingIn = pref.getInt("isSingIn",0)

        if (isSingIn == 1){
            // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        signIn()
        } else {
            progressBar.visibility = View.INVISIBLE
            val intent = Intent(this, GoogleSinginActivity::class.java)
            startActivity(intent)
        }

        // Set the alarm to start at approximately 11:00 p.m.
        val calendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE,0)
            set(Calendar.SECOND, 0)
        }
        //通知のセット
        val intent = Intent(applicationContext, AlarmNotification::class.java)
        intent.putExtra("RequestCode", reqCode)
        pending = PendingIntent.getBroadcast(applicationContext, reqCode, intent, PendingIntent.FLAG_MUTABLE)

        //通知がセットされていないかつサインインした状態であれば、通知をセットする
        if (pref.getInt("isAlarmSet",0) == 0 && isSingIn == 1){
            // アラームをセットする
            am = getSystemService(ALARM_SERVICE) as AlarmManager
            if (am != null) {
                am!!.setInexactRepeating(AlarmManager.RTC_WAKEUP,calendar.timeInMillis, AlarmManager.INTERVAL_DAY, pending)
                editor.putInt("isAlarmSet",1).apply()
            }else {
                //Toast.makeText(applicationContext, "アラームの起動に失敗しました", Toast.LENGTH_SHORT).show()
            }
        }

        //画像のパスを読み込む
        val strUri = pref.getString("CHECK_FOOD_IMAGE_PATH","default")
        //もし取得できた場合、画像を読み込んでimageViewにセットする
        if (strUri != "default"){
            val uri = Uri.parse(strUri)
            try {
                val bmp: Bitmap? = uri?.let { getBitmapFromUri(it) }
                ma_foodImageView.setImageBitmap(bmp)
            } catch (e: Exception) {
                e.printStackTrace()
                //画像が読み込めなかった場合、デフォルト画像を表示する
                ma_foodImageView.setImageResource(R.drawable.rice)
            }
        }

        //右下のボタンを押した場合
        ma_foodFloatingActionButton.setOnClickListener {
            val intent = Intent(this, FoodListActivity::class.java)
            startActivity(intent)
        }

        //右上のアカウントボタンを押した場合、サインアウト確認ダイアログを表示する
        ma_googleSingOutButton.setOnClickListener {
            val dialog = SingOutConfirmationDialog(account.email)
            dialog.show(supportFragmentManager, "sing_out_confirmation_dialog")
        }
    }

    override fun onBackPressed() {
        //アプリケーション全体が中断状態になります。
        //ホームボタンを押した時の動作と同じ動作になります。
        moveTaskToBack(true)
    }
}