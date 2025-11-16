package com.example.stressbustersimulator

import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class GameActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_START_MODE = "start_mode"
    }

    private var rewardedAd: RewardedAd? = null
    private lateinit var gameView: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        val root = findViewById<FrameLayout>(R.id.gameRoot)
        gameView = GameView(this)

        val modeName = intent.getStringExtra(EXTRA_START_MODE)
        if (modeName != null) {
            try {
                val mode = GameView.Mode.valueOf(modeName)
                gameView.setMode(mode)
            } catch (_: Exception) { }
        }

        root.addView(gameView)

        // Reward button
        val btnReward = findViewById<Button>(R.id.btnReward)
        btnReward.setOnClickListener {
            showRewardedAd()
        }

        // Load rewarded ad (TEST)
        loadRewardedAd()
    }

    private fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            this,
            "ca-app-pub-3940256099942544/5224354917", //  TEST Rewarded Ad ID
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    rewardedAd = null
                    Toast.makeText(this@GameActivity, "Rewarded Ad failed to load", Toast.LENGTH_SHORT).show()
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Toast.makeText(this@GameActivity, "Rewarded Ad loaded", Toast.LENGTH_SHORT).show()
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

            // TODO: connect reward to gameView logic
            // gameView.unlockSpecialEffect()

            rewardedAd = null
            loadRewardedAd()   // Preload next rewarded ad
        }
    }
}
