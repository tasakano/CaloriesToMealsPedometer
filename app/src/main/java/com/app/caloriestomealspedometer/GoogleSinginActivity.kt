package com.app.caloriestomealspedometer

import android.content.ContentValues
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.tasks.Task
import kotlinx.android.synthetic.main.activity_google_singin.*

class GoogleSinginActivity : AppCompatActivity() {

    private val RC_SIGN_IN = 1
    private val REQUEST_OAUTH_REQUEST_CODE = 2

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

        if (requestCode == REQUEST_OAUTH_REQUEST_CODE){
            if (resultCode == -1) {
                //0.5秒後にMainActivityに遷移する
                Handler(Looper.getMainLooper()).postDelayed({
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }, 500)
            } else {
                Toast.makeText(this,"Google アカウントのデータへのアクセス権が取得できませんでした。", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            account = completedTask.getResult(ApiException::class.java)

            // Signed in successfully, show authenticated UI.
            val pref = PreferenceManager.getDefaultSharedPreferences(this)
            val editor = pref.edit()

            editor.putInt("isSingIn",1).apply()

            Toast.makeText(this,"Googleアカウントとの連携に成功しました", Toast.LENGTH_SHORT).show()

            requestPermission()

        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(ContentValues.TAG, "signInResult:failed code=" + e.statusCode)
            Toast.makeText(this,"Googleアカウントとの連携に失敗しました", Toast.LENGTH_SHORT).show()
            //updateUI(null)
        }
    }

    private fun requestPermission(){
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
        } else {
            //0.5秒後にMainActivityに遷移する
            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_google_singin)

        //「Sing in with google」ボタンを押したとき
        gsa_googleSigninButton.setOnClickListener {
            // Build a GoogleSignInClient with the options specified by gso.
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
            signIn()
        }

        gsa_descriptionImageButton.setOnClickListener {
            val intent = Intent(this, AppDescriptionActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onBackPressed() {
        //アプリケーション全体が中断状態になります。
        //ホームボタンを押した時の動作と同じ動作になります。
        moveTaskToBack(true)
    }
}