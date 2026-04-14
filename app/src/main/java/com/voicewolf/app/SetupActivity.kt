package com.voicewolf.app

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class SetupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        setupSetupCards()
    }

    private fun setupSetupCards() {
        val container = findViewById<LinearLayout>(R.id.setupCardsContainer)

        GameSetupPresets.ALL.forEach { setup ->
            val card = createSetupCard(setup)
            container.addView(card)
        }
    }

    private fun createSetupCard(setup: GameSetup): MaterialCardView {
        val density = resources.displayMetrics.density
        val paddingPx = (16 * density).toInt()

        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, (16 * density).toInt())
            }
            radius = 12f * density
            cardElevation = 4f * density
            setCardBackgroundColor(resources.getColor(R.color.info_panel, null))
            strokeWidth = 2
            strokeColor = resources.getColor(R.color.border_default, null)
            isClickable = true
            isFocusable = true

            setOnClickListener {
                startMainActivity(setup)
            }
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Setup name
        val nameText = TextView(this).apply {
            text = setup.name
            setTextColor(resources.getColor(R.color.white, null))
            textSize = 18f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, (8 * density).toInt())
            }
        }
        contentLayout.addView(nameText)

        // Good roles summary
        val goodText = TextView(this).apply {
            text = "好人：${setup.getGoodRolesSummary()}"
            setTextColor(resources.getColor(R.color.border_good, null))
            textSize = 14f
            gravity = Gravity.CENTER
        }
        contentLayout.addView(goodText)

        // Evil roles summary
        val evilText = TextView(this).apply {
            text = "狼人：${setup.getEvilRolesSummary()}"
            setTextColor(resources.getColor(R.color.wolf_red, null))
            textSize = 14f
            gravity = Gravity.CENTER
        }
        contentLayout.addView(evilText)

        card.addView(contentLayout)
        return card
    }

    private fun startMainActivity(setup: GameSetup) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("SETUP_NAME", setup.name)
        }
        startActivity(intent)
        finish()
    }
}