package com.TheBudgeteers.dragonomics

import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import android.widget.ImageButton
import android.widget.ImageView

private const val DRAGON_BIG_DP = 500
private const val DRAGON_SMALL_DP = 360
private const val ROTATE_MS = 180L
private const val KEY_EXPANDED = "expanded"

class HomeActivity : AppCompatActivity() {

    private var expanded = false   // false = collapsed on cold start

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        val root   = findViewById<ConstraintLayout>(R.id.dashboard)
        val arrow  = findViewById<ImageButton>(R.id.toggleArrow)
        val goal   = findViewById<View>(R.id.goalBar)
        val dragon = findViewById<ImageView>(R.id.dragon)

        // Cold start => collapsed. If rotating, restore last state.
        expanded = if (savedInstanceState == null) false
        else savedInstanceState.getBoolean(KEY_EXPANDED, false)

        // Apply initial state before first draw (no flash)
        if (expanded) applyExpanded(root, goal, arrow, dragon, animate = false)
        else          applyCollapsed(root, goal, arrow, dragon, animate = false)

        arrow.setOnClickListener {
            expanded = !expanded
            if (expanded) applyExpanded(root, goal, arrow, dragon, animate = true)
            else          applyCollapsed(root, goal, arrow, dragon, animate = true)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_EXPANDED, expanded)
        super.onSaveInstanceState(outState)
    }

    private fun applyCollapsed(
        root: ConstraintLayout,
        goal: View,
        arrow: ImageButton,
        dragon: ImageView,
        animate: Boolean
    ) {
        if (animate) TransitionManager.beginDelayedTransition(root, AutoTransition())
        goal.visibility = View.GONE                 // HIDDEN when collapsed
        arrow.animate().rotation(180f).setDuration(ROTATE_MS).start()   // arrow points up
        dragon.updateHeightDp(DRAGON_BIG_DP)       // bigger dragon when collapsed
    }

    private fun applyExpanded(
        root: ConstraintLayout,
        goal: View,
        arrow: ImageButton,
        dragon: ImageView,
        animate: Boolean
    ) {
        if (animate) TransitionManager.beginDelayedTransition(root, AutoTransition())
        goal.visibility = View.VISIBLE             // SHOWN when expanded
        arrow.animate().rotation(0f).setDuration(ROTATE_MS).start()     // arrow points down
        dragon.updateHeightDp(DRAGON_SMALL_DP)     // smaller dragon when expanded
    }

    private fun ImageView.updateHeightDp(h: Int) {
        val lp = layoutParams
        lp.height = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, h.toFloat(), resources.displayMetrics
        ).toInt()
        layoutParams = lp
        requestLayout()
    }
}
