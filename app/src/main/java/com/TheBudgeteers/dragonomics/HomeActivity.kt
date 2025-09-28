package com.TheBudgeteers.dragonomics

import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager

private const val DRAGON_BIG_DP = 450
private const val DRAGON_SMALL_DP = 360
private const val ROTATE_MS = 180L
private const val KEY_EXPANDED = "expanded"
private const val KEY_ACH_OPEN = "ach_open"

class HomeActivity : AppCompatActivity() {

    // ---------- false = starts collapsed ----------
    private var expanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        // ---------- base dashboard refs ----------
        val root   = findViewById<ConstraintLayout>(R.id.dashboard)
        val arrow  = findViewById<ImageButton>(R.id.toggleArrow)
        val goal   = findViewById<View>(R.id.goalBar)
        val dragon = findViewById<ImageView>(R.id.dragon)

        // ---------- overlay / achievements refs ----------
        val achBtn   = findViewById<ImageButton>(R.id.achievementsImg)
        val overlay  = findViewById<View>(R.id.achievementsOverlay)
        val card     = findViewById<View>(R.id.achievementsCard)
        val closeX   = findViewById<ImageButton>(R.id.closeX)

        // ---------- RecyclerView ----------
        val achRecycler = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.achRecycler)
        achRecycler.setHasFixedSize(true)
        achRecycler.layoutManager = LinearLayoutManager(this)
        val achAdapter = AchievementsAdapter(emptyList())
        achRecycler.adapter = achAdapter

        // Achievement Data, like a JSON file essentially
        achAdapter.submit(
            listOf(
                Achievement(
                    id = "master",
                    title = "Dragon Master",
                    description = "Unlock all customizations for your dragon.",
                    medalRes = R.drawable.gold_badge,
                    achieved = false
                ),
                Achievement(
                    id = "hoard",
                    title = "Dragon’s Hoard",
                    description = "Have 30,000 or more in a savings nest.",
                    medalRes = R.drawable.silver_badge,
                    achieved = false
                ),
                Achievement(
                    id = "streak",
                    title = "Flames of authority",
                    description = "Log for 30 days in a row.",
                    medalRes = R.drawable.bronze_badge,
                    achieved = true
                )
            )
        )

        // ---------- restore state ----------
        expanded = savedInstanceState?.getBoolean(KEY_EXPANDED, false) ?: false
        val achOpen = savedInstanceState?.getBoolean(KEY_ACH_OPEN, false) ?: false

        if (expanded) applyExpanded(root, goal, arrow, dragon, animate = false)
        else          applyCollapsed(root, goal, arrow, dragon, animate = false)

        if (achOpen) overlay.showFadeIn(immediate = true)

        // ---------- arrow toggle ----------
        arrow.setOnClickListener {
            expanded = !expanded
            if (expanded) applyExpanded(root, goal, arrow, dragon, animate = true)
            else          applyCollapsed(root, goal, arrow, dragon, animate = true)
        }

        // ---------- achievements open/close ----------
        achBtn.setOnClickListener { overlay.showFadeIn() }
        closeX.setOnClickListener { overlay.hideFadeOut() }

        // tap scrim to close; tap card to swallow (so scrim click doesn't trigger)
        overlay.setOnClickListener { overlay.hideFadeOut() }
        card.setOnClickListener {  }

        // Back button: if overlay visible, close it first
        onBackPressedDispatcher.addCallback(this) {
            if (overlay.visibility == View.VISIBLE) overlay.hideFadeOut() else finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_EXPANDED, expanded)
        val overlay = findViewById<View>(R.id.achievementsOverlay)
        outState.putBoolean(KEY_ACH_OPEN, overlay.visibility == View.VISIBLE)
        super.onSaveInstanceState(outState)
    }

    // ---------------- helpers ----------------

    private fun applyCollapsed(
        root: ConstraintLayout,
        goal: View,
        arrow: ImageButton,
        dragon: ImageView,
        animate: Boolean
    ) {
        if (animate) TransitionManager.beginDelayedTransition(root, AutoTransition())
        goal.visibility = View.GONE
        arrow.animate().rotation(180f).setDuration(ROTATE_MS).start() // points up
        dragon.updateHeightDp(DRAGON_BIG_DP)
    }

    private fun applyExpanded(
        root: ConstraintLayout,
        goal: View,
        arrow: ImageButton,
        dragon: ImageView,
        animate: Boolean
    ) {
        if (animate) TransitionManager.beginDelayedTransition(root, AutoTransition())
        goal.visibility = View.VISIBLE
        arrow.animate().rotation(0f).setDuration(ROTATE_MS).start() // points down
        dragon.updateHeightDp(DRAGON_SMALL_DP)
    }

    private fun ImageView.updateHeightDp(h: Int) {
        val lp = layoutParams
        lp.height = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, h.toFloat(), resources.displayMetrics
        ).toInt()
        layoutParams = lp
        requestLayout()
    }

    private fun View.showFadeIn(immediate: Boolean = false) {
        if (visibility == View.VISIBLE) return
        if (immediate) {
            alpha = 1f
            visibility = View.VISIBLE
        } else {
            alpha = 0f
            visibility = View.VISIBLE
            animate().alpha(1f).setDuration(150).start()
        }
    }

    private fun View.hideFadeOut() {
        if (visibility != View.VISIBLE) return
        animate().alpha(0f).setDuration(150).withEndAction {
            visibility = View.GONE
            alpha = 1f
        }.start()
    }
}
