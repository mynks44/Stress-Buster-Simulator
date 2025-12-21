package com.example.stressbustersimulator

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class MainActivity : AppCompatActivity() {

    private var interstitialAd: InterstitialAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        MobileAds.initialize(this) {}

        loadBannerAd()
        loadInterstitialAd()

        val btnPlayBubble = findViewById<Button>(R.id.btnPlayBubble)
        val btnPlayPaint = findViewById<Button>(R.id.btnPlayPaint)
        val btnSettings = findViewById<Button>(R.id.btnSettings)
        val btnPrivacy = findViewById<Button>(R.id.btnPrivacy)

        btnPlayBubble.setOnClickListener {
            showInterstitialThenOpenGame(GameView.Mode.BUBBLE)
        }

        btnPlayPaint.setOnClickListener {
            showInterstitialThenOpenGame(GameView.Mode.PAINT_SPLASH)
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnPrivacy.setOnClickListener {
            startActivity(Intent(this, PrivacyPolicyActivity::class.java))
        }
    }


    private fun loadBannerAd() {
        val adContainer = findViewById<FrameLayout>(R.id.adContainer)

        val adView = AdView(this)
        adView.setAdSize(AdSize.BANNER)

        // PRODUCTION Banner Ad Unit ID
        adView.adUnitId = "ca-app-pub-2039404506887879/1757902432"

        // for testing
        // adView.adUnitId = "ca-app-pub-3940256099942544/6300978111"

        adContainer.addView(adView)

        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }


    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()

        //  PRODUCTION Interstitial Ad Unit ID
        val adUnitId = "ca-app-pub-2039404506887879/4624449363"

        // for testing
        // val adUnitId = "ca-app-pub-3940256099942544/1033173712"

        InterstitialAd.load(
            this,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {

                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    private fun showInterstitialThenOpenGame(mode: GameView.Mode) {
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {

                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitialAd()
                    openGame(mode)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    openGame(mode)
                }
            }
            ad.show(this)
        } else {
            openGame(mode)
        }
    }

    private fun openGame(mode: GameView.Mode) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra(GameActivity.EXTRA_START_MODE, mode.name)
        startActivity(intent)
    }
}
