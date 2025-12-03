package com.example.stressbustersimulator

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.time.LocalDate

class GameActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_START_MODE = "start_mode"
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var gameView: GameView
    private lateinit var meter: ProgressBar

    private var rewardedAd: RewardedAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        prefs = getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)

        meter = findViewById(R.id.satisfactionMeter)
        gameView = findViewById(R.id.gameView)

        val modeName = intent.getStringExtra(EXTRA_START_MODE)
        try {
            val mode = GameView.Mode.valueOf(modeName ?: "BUBBLE")
            gameView.setMode(mode)
        } catch (_: Exception) {}

        gameView.onSatisfactionChange = { value ->
            runOnUiThread { meter.progress = value }
        }

        initDailyChallenge()

        val btnReward = findViewById<Button>(R.id.btnReward)
        btnReward.setOnClickListener {
            showRewardedAd()
        }

        val btnAuto = findViewById<Button>(R.id.btnAutoVanish)
        val btnPermanent = findViewById<Button>(R.id.btnPermanent)
        val btnClear = findViewById<Button>(R.id.btnClear)

        btnAuto.setOnClickListener {
            gameView.setSplashMode(GameView.SplashMode.VANISH)
            Toast.makeText(this, "Auto Vanish Enabled", Toast.LENGTH_SHORT).show()
        }

        btnPermanent.setOnClickListener {
            gameView.setSplashMode(GameView.SplashMode.PERMANENT)
            Toast.makeText(this, "Permanent Mode Enabled", Toast.LENGTH_SHORT).show()
        }

        btnClear.setOnClickListener {
            gameView.clearSplashes()
            Toast.makeText(this, "Canvas Cleared", Toast.LENGTH_SHORT).show()
        }

        loadRewardedAd()
    }


    private fun initDailyChallenge() {
        val today = LocalDate.now().toString()
        val lastDay = prefs.getString(Prefs.KEY_LAST_PLAY_DATE, "")

        if (today != lastDay) {
            prefs.edit()
                .putString(Prefs.KEY_LAST_PLAY_DATE, today)
                .putInt(Prefs.KEY_DAILY_PROGRESS, 0)
                .putInt(Prefs.KEY_DAILY_GOAL, 50)
                .apply()
        }
    }

    private fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            this,
            "ca-app-pub-3940256099942544/5224354917",
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    rewardedAd = null
                    Toast.makeText(
                        this@GameActivity,
                        "Rewarded Ad failed to load",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Toast.makeText(
                        this@GameActivity,
                        "Rewarded Ad loaded",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun showRewardedAd() {
        val ad = rewardedAd
        if (ad == null) {
            Toast.makeText(this, "Reward loading... try again soon", Toast.LENGTH_SHORT).show()
            loadRewardedAd()
            return
        }

        ad.show(this) { rewardItem: RewardItem ->
            Toast.makeText(this, "Reward earned! +1 Super Splash", Toast.LENGTH_SHORT).show()
            rewardedAd = null
            loadRewardedAd()
        }
    }
}
