package com.TheBudgeteers.dragonomics

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.TheBudgeteers.dragonomics.databinding.ActivityHomeBinding
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout

private const val DRAGON_BIG_DP = 450
private const val DRAGON_SMALL_DP = 360
private const val ROTATE_MS = 180L
private const val KEY_EXPANDED = "expanded"
private const val KEY_ACH_OPEN = "ach_open"
private const val KEY_SHOP_OPEN = "shop_open"

class HomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var expanded = false

    private lateinit var binding: ActivityHomeBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            onNavigationItemSelected(item)
        }

        binding.bottomNavigationView.itemIconTintList = null


        val root = binding.dashboard
        val arrow = binding.toggleArrow
        val goal = binding.goalBar
        val dragon = binding.dragon


        // ---------- achievements overlay refs ----------
        val achBtn = binding.achievementsImg
        val achOverlay = binding.achievementsOverlay
        val achCard = binding.achievementsCard
        val achClose = binding.closeX

        // ---------- shop overlay refs ----------
        val shopBtn = binding.shopImg
        val shopOverlay = binding.shopOverlay
        val shopCard = binding.shopCard
        val shopCloseX = binding.shopCloseX
        val shopTabs = binding.shopTabs
        val shopCurrAmt = binding.shopCurrencyAmount
        val homeCurrText = binding.currencyTxt

        // ---------- ACHIEVEMENTS RecyclerView (RESTORED) ----------
        val achRecycler = binding.achRecycler
        achRecycler.setHasFixedSize(true)
        achRecycler.layoutManager = LinearLayoutManager(this)
        val achAdapter = AchievementsAdapter(emptyList())
        achRecycler.adapter = achAdapter


        // Demo data — swap for your real data source
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
        val shopOpen = savedInstanceState?.getBoolean(KEY_SHOP_OPEN, false) ?: false

        if (expanded) applyExpanded(root, goal, arrow, dragon, animate = false)
        else applyCollapsed(root, goal, arrow, dragon, animate = false)

        if (achOpen) achOverlay.showFadeIn(immediate = true)
        if (shopOpen) shopOverlay.showFadeIn(immediate = true)

        // ---------- arrow toggle ----------
        arrow.setOnClickListener {
            expanded = !expanded
            if (expanded) applyExpanded(root, goal, arrow, dragon, animate = true)
            else applyCollapsed(root, goal, arrow, dragon, animate = true)
        }

        // ---------- achievements open/close ----------
        achBtn.setOnClickListener {
            if (shopOverlay.visibility == View.VISIBLE) shopOverlay.hideFadeOut()
            achOverlay.showFadeIn()
        }
        achClose.setOnClickListener { achOverlay.hideFadeOut() }
        achOverlay.setOnClickListener { achOverlay.hideFadeOut() }
        achCard.setOnClickListener { /* swallow */ }

        // ---------- shop open/close ----------
        shopBtn.setOnClickListener {
            shopCurrAmt.text = homeCurrText.text  // mirror current currency
            if (achOverlay.visibility == View.VISIBLE) achOverlay.hideFadeOut()
            shopOverlay.showFadeIn()
        }
        shopCloseX.setOnClickListener { shopOverlay.hideFadeOut() }
        shopOverlay.setOnClickListener { shopOverlay.hideFadeOut() }
        shopCard.setOnClickListener { /* swallow */ }

        // ---------- shop tabs (3 categories) ----------
        val tabIcons = intArrayOf(
            R.drawable.palette_shop,
            R.drawable.horns_shop,
            R.drawable.wings_shop
        )
        repeat(3) { i -> shopTabs.addTab(shopTabs.newTab().setIcon(tabIcons[i])) }

        val gold = ContextCompat.getColor(this, R.color.GoldenEmber)
        val dim = Color.parseColor("#546579")
        fun tintTabs() {
            for (i in 0 until shopTabs.tabCount) {
                val t = shopTabs.getTabAt(i)
                t?.icon?.setTint(if (t?.isSelected == true) gold else dim)
            }
        }
        shopTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = tintTabs()
            override fun onTabUnselected(tab: TabLayout.Tab) = tintTabs()
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        shopTabs.selectTab(shopTabs.getTabAt(0))
        tintTabs()

        // ---------- Back button behavior ----------
        onBackPressedDispatcher.addCallback(this) {
            when {
                shopOverlay.visibility == View.VISIBLE -> shopOverlay.hideFadeOut()
                achOverlay.visibility == View.VISIBLE -> achOverlay.hideFadeOut()
                else -> finish()
            }
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_EXPANDED, expanded)
        outState.putBoolean(KEY_ACH_OPEN, binding.achievementsOverlay.visibility == View.VISIBLE)
        outState.putBoolean(KEY_SHOP_OPEN, binding.shopOverlay.visibility == View.VISIBLE)
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
        arrow.animate().rotation(180f).setDuration(ROTATE_MS).start()
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
        arrow.animate().rotation(0f).setDuration(ROTATE_MS).start()
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> openIntent(this, "", HomeActivity::class.java)
            R.id.nav_expenses -> openIntent(this, "", ExpensesActivity::class.java)
            R.id.nav_history -> openIntent(this, "", HistoryActivity::class.java)
            R.id.nav_profile -> openIntent(this, "", ProfileActivity::class.java)
        }
        return true

    }
}
