package com.example.stressbustersimulator   // 👈 change if needed

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
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
    private lateinit var splashControls: LinearLayout

    private var rewardedAd: RewardedAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )


        prefs = getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)

        gameView = findViewById(R.id.gameView)
        meter = findViewById(R.id.satisfactionMeter)
        splashControls = findViewById(R.id.splashControls)

        val btnReward = findViewById<Button>(R.id.btnReward)
        val btnAuto = findViewById<Button>(R.id.btnAutoVanish)
        val btnPermanent = findViewById<Button>(R.id.btnPermanent)
        val btnClear = findViewById<Button>(R.id.btnClear)


        val modeName = intent.getStringExtra(EXTRA_START_MODE)
        val mode = try {
            GameView.Mode.valueOf(modeName ?: GameView.Mode.BUBBLE.name)
        } catch (_: Exception) {
            GameView.Mode.BUBBLE
        }

        gameView.setMode(mode)

        splashControls.visibility =
            if (mode == GameView.Mode.PAINT_SPLASH) View.VISIBLE else View.GONE


        gameView.onSatisfactionChange = { value ->
            runOnUiThread { meter.progress = value }
        }


        initDailyChallenge()


        btnReward.setOnClickListener {
            showRewardedAd()
        }


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
            "ca-app-pub-2039404506887879/8120841243",
//            for testing
//            "ca-app-pub-3940256099942544/5224354917",
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    rewardedAd = null
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
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

        ad.show(this) { _: RewardItem ->
            Toast.makeText(this, "Reward earned! +1 Super Splash", Toast.LENGTH_SHORT).show()
            rewardedAd = null
            loadRewardedAd()
        }
    }
}
