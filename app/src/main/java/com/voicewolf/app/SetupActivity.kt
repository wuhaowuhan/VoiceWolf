package com.voicewolf.app

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class SetupActivity : AppCompatActivity() {

    private var selectedPlayerCount = 12  // Default player count
    private val playerCountButtons = mutableListOf<MaterialButton>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        setupPlayerCountButtons()
        setupSetupCards()
    }

    private fun setupPlayerCountButtons() {
        val btn12 = findViewById<MaterialButton>(R.id.btnPlayer12)
        val btn13 = findViewById<MaterialButton>(R.id.btnPlayer13)
        val btn14 = findViewById<MaterialButton>(R.id.btnPlayer14)
        val btn15 = findViewById<MaterialButton>(R.id.btnPlayer15)

        playerCountButtons.addAll(listOf(btn12, btn13, btn14, btn15))

        // Set click listeners
        btn12.setOnClickListener { selectPlayerCount(12) }
        btn13.setOnClickListener { selectPlayerCount(13) }
        btn14.setOnClickListener { selectPlayerCount(14) }
        btn15.setOnClickListener { selectPlayerCount(15) }

        // Initial selection
        selectPlayerCount(12)
    }

    private fun selectPlayerCount(count: Int) {
        selectedPlayerCount = count

        // Update button states - more visual contrast
        playerCountButtons.forEach { btn ->
            val buttonCount = btn.text.toString().replace("人", "").toIntOrNull()
            if (buttonCount == count) {
                // Selected button - filled style with bright color
                btn.setBackgroundColor(resources.getColor(R.color.teal_200, null))
                btn.setTextColor(resources.getColor(R.color.black, null))
                btn.setStrokeWidth(0)
            } else {
                // Unselected button - outlined style
                btn.setBackgroundColor(resources.getColor(R.color.info_panel, null))
                btn.setTextColor(resources.getColor(R.color.gray_light, null))
                btn.setStrokeWidth(2)
                btn.setStrokeColorResource(R.color.border_default)
            }
        }
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
                setMargins(0, 0, 0, (12 * density).toInt())
            }
            radius = 12f * density
            cardElevation = 4f * density
            setCardBackgroundColor(resources.getColor(R.color.info_panel, null))
            strokeWidth = 2
            strokeColor = resources.getColor(R.color.border_default, null)
            isClickable = true
            isFocusable = true

            setOnClickListener {
                startMainActivity(setup, selectedPlayerCount)
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
            textSize = 16f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, (6 * density).toInt())
            }
        }
        contentLayout.addView(nameText)

        // Good roles summary
        val goodText = TextView(this).apply {
            text = "好人：${setup.getGoodRolesSummary()}"
            setTextColor(resources.getColor(R.color.border_good, null))
            textSize = 12f
            gravity = Gravity.CENTER
        }
        contentLayout.addView(goodText)

        // Evil roles summary
        val evilText = TextView(this).apply {
            text = "狼人：${setup.getEvilRolesSummary()}"
            setTextColor(resources.getColor(R.color.wolf_red, null))
            textSize = 12f
            gravity = Gravity.CENTER
        }
        contentLayout.addView(evilText)

        card.addView(contentLayout)
        return card
    }

    private fun startMainActivity(setup: GameSetup, playerCount: Int) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("SETUP_NAME", setup.name)
            putExtra("PLAYER_COUNT", playerCount)
        }
        startActivity(intent)
        finish()
    }
}