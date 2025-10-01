package com.TheBudgeteers.dragonomics
import com.TheBudgeteers.dragonomics.R
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.TheBudgeteers.dragonomics.databinding.ActivityHomeBinding
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout

private const val DRAGON_BIG_DP = 360
private const val DRAGON_SMALL_DP = 250
private const val ROTATE_MS = 180L
private const val KEY_EXPANDED = "expanded"
private const val KEY_ACH_OPEN = "ach_open"
private const val KEY_SHOP_OPEN = "shop_open"

class HomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var expanded = false

    private lateinit var binding: ActivityHomeBinding

    // --- shop state ---
    private lateinit var shopAdapter: ShopAdapter
    private var currency: Int = 0

    // sample 4-per-tab items
    private val hornsItems = listOf(
        ShopItem("horns_twisted",  "Twisted Horns",  90, previewRes = R.drawable.placeholder_item),
        ShopItem("horns_curly",    "Curly Horns",    90, previewRes = R.drawable.placeholder_item),
        ShopItem("horns_chipped",  "Chipped Horns",   0, owned = true, previewRes = R.drawable.placeholder_item),
        ShopItem("horns_straight", "Straight Horns", 90, previewRes = R.drawable.placeholder_item)
    )
    private val wingsItems = listOf(
        ShopItem("wings_bat",     "Bat Wings",     120, previewRes = R.drawable.placeholder_item),
        ShopItem("wings_feather", "Feathered",     150, previewRes = R.drawable.placeholder_item),
        ShopItem("wings_ragged",  "Ragged",         60, owned = true, previewRes = R.drawable.placeholder_item),
        ShopItem("wings_royal",   "Royal Wings",   200, previewRes = R.drawable.placeholder_item)
    )
    private val paletteItems = listOf(
        ShopItem("pal_forest",  "Forest Scheme", 40, previewRes = R.drawable.placeholder_item),
        ShopItem("pal_crimson", "Crimson Scheme",60, previewRes = R.drawable.placeholder_item),
        ShopItem("pal_ember",   "Ember Scheme",   0, owned = true, previewRes = R.drawable.placeholder_item),
        ShopItem("pal_ice",     "Ice Scheme",    50, previewRes = R.drawable.placeholder_item)
    )

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
        val shopRecycler = binding.shopRecycler

        // ---------- ACHIEVEMENTS RecyclerView ----------
        val achRecycler = binding.achRecycler
        achRecycler.setHasFixedSize(true)
        achRecycler.layoutManager = LinearLayoutManager(this)
        val achAdapter = AchievementsAdapter(emptyList())
        achRecycler.adapter = achAdapter


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
        currency = homeCurrText.text.toString().toIntOrNull() ?: 0
        shopCurrAmt.text = currency.toString()

        shopBtn.setOnClickListener {
            shopCurrAmt.text = currency.toString()
            if (achOverlay.visibility == View.VISIBLE) achOverlay.hideFadeOut()
            shopAdapter.submitList(paletteItems)
            shopOverlay.showFadeIn()
        }

        shopCloseX.setOnClickListener { shopOverlay.hideFadeOut() }
        shopOverlay.setOnClickListener { shopOverlay.hideFadeOut() }
        shopCard.setOnClickListener { /* swallow */ }

        // ---------- shop tabs ----------
        val tabIcons = intArrayOf(
            R.drawable.palette_shop,
            R.drawable.horns_shop,
            R.drawable.wings_shop
        )

        if (shopTabs.tabCount == 0)
        {
            repeat(3) { i -> shopTabs.addTab(shopTabs.newTab().setIcon(tabIcons[i])) }
        }
        else
        {
            for (i in 0 until shopTabs.tabCount)
                shopTabs.getTabAt(i)?.icon = ContextCompat.getDrawable(this, tabIcons[i])
        }

        val gold = ContextCompat.getColor(this, R.color.GoldenEmber)
        val dim = Color.parseColor("#546579")
        fun tintTabs() {
            for (i in 0 until shopTabs.tabCount) {
                val t = shopTabs.getTabAt(i)
                t?.icon?.setTint(if (t?.isSelected == true) gold else dim)
            }
        }

        // --- Shop Recycler---
        shopAdapter = ShopAdapter { clicked -> handleShopAction(clicked) }
        binding.shopRecycler.adapter = shopAdapter
        binding.shopRecycler.setHasFixedSize(true)
        binding.shopRecycler.layoutManager = GridLayoutManager(this, 2)

        val space = (resources.displayMetrics.density * 8).toInt()
        shopRecycler.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
            ) { outRect.set(space, space, space, space) }
        })

        shopAdapter.submitList(paletteItems)
        android.util.Log.d("Shop", "count = ${shopAdapter.itemCount}")

        // Tab selection drives which 4 items are shown
        shopTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                tintTabs()
                val which = when (tab.position) {
                    0 -> ShopTab.PALETTE
                    1 -> ShopTab.HORNS
                    else -> ShopTab.WINGS
                }
                showTab(which)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) { tintTabs() }
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        shopTabs.selectTab(shopTabs.getTabAt(0))
        tintTabs()
        showTab(ShopTab.PALETTE)

        resizeShopTabIcons(sizeDp = 80, tabHeightDp = 80, horizPadDp = 10)

        // ---------- Back button ----------
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

    private fun showTab(tab: ShopTab) {
        val items = when (tab) {
            ShopTab.PALETTE -> paletteItems
            ShopTab.HORNS   -> hornsItems
            ShopTab.WINGS   -> wingsItems
        }
        shopAdapter.submitList(items)
        android.util.Log.d("Shop", "count = ${shopAdapter.itemCount}")
    }

    private fun handleShopAction(item: ShopItem) {
        val list = when {
            item.id.startsWith("pal_")   -> paletteItems.toMutableList()
            item.id.startsWith("horns_") -> hornsItems.toMutableList()
            else                         -> wingsItems.toMutableList()
        }
        val idx = list.indexOfFirst { it.id == item.id }
        if (idx == -1) return

        val current = list[idx]
        when {
            current.equipped -> return
            current.owned -> {
                for (i in list.indices) list[i] = list[i].copy(equipped = false)
                list[idx] = current.copy(equipped = true)
            }
            else -> {
                if (currency >= current.price) {
                    currency -= current.price
                    binding.shopCurrencyAmount.text = currency.toString()
                    binding.currencyTxt.text = currency.toString()
                    list[idx] = current.copy(owned = true)
                } else {

                }
            }
        }
        shopAdapter.submitList(list)
    }

    // --- Option 2 ---

    private fun Int.dp(): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), resources.displayMetrics).toInt()

    private fun resizeShopTabIcons(sizeDp: Int = 32, tabHeightDp: Int = 56, horizPadDp: Int = 10) {
        binding.shopTabs.post {
            val strip = binding.shopTabs.getChildAt(0) as? ViewGroup ?: return@post
            val iconSize = sizeDp.dp()
            val tabH = tabHeightDp.dp()
            val padH = horizPadDp.dp()
            for (i in 0 until strip.childCount) {
                val tabView = strip.getChildAt(i) as? ViewGroup ?: continue
                // Tab height + padding (touch target)
                tabView.layoutParams = tabView.layoutParams.apply { height = tabH }
                tabView.setPadding(padH, 0, padH, 0)
                // Built-in icon ImageView
                val iconView = tabView.findViewById<ImageView>(com.google.android.material.R.id.icon)
                iconView?.layoutParams = iconView?.layoutParams?.apply {
                    width = iconSize
                    height = iconSize
                }
                iconView?.scaleType = ImageView.ScaleType.CENTER_INSIDE
                iconView?.requestLayout()
            }
            strip.requestLayout()
        }
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
