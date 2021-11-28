package com.app.caloriestomealspedometer

import android.app.Application
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import io.realm.Realm

class CaloriesToMealsPedometerApplication : Application() {
    private var adView: AdView? = null
    override fun onCreate() {
        super.onCreate()
        Realm.init(this)

        //admob広告の読み込み
        adView = AdView(this)
        adView!!.adSize = AdSize.BANNER
        adView!!.adUnitId = getString(R.string.banner_ad_id)

        val extras = Bundle()
        extras.putString("max_ad_content_rating", "G")
        // Request for Ads
        val adRequest: AdRequest = AdRequest.Builder()
            .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
            .build()

        // Load ads into Banner Ads
        adView!!.loadAd(adRequest)
    }

    fun loadAd(layAd: LinearLayout) {
        // Locate the Banner Ad in activity xml
        if (adView!!.parent != null) {
            val tempVg = adView!!.parent as ViewGroup
            tempVg.removeView(adView)
        }
        layAd.addView(adView)
    }
}