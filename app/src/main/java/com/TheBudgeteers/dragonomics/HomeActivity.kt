package com.TheBudgeteers.dragonomics

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.MenuItem
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.TheBudgeteers.dragonomics.databinding.ActivityHomeBinding
import com.TheBudgeteers.dragonomics.gamify.DragonMoodManager
import com.TheBudgeteers.dragonomics.ui.AchievementsDialogFragment
import com.TheBudgeteers.dragonomics.ui.ShopDialogFragment
import com.TheBudgeteers.dragonomics.viewmodel.AchievementsViewModel
import com.TheBudgeteers.dragonomics.viewmodel.DragonUiState
import com.TheBudgeteers.dragonomics.viewmodel.DragonViewModel
import com.TheBudgeteers.dragonomics.viewmodel.ShopViewModel
import com.TheBudgeteers.dragonomics.viewmodel.AccessoryEquipListener
import com.TheBudgeteers.dragonomics.DragonSockets.ADULT_DRAGON_SOCKETS
import com.TheBudgeteers.dragonomics.DragonSockets.BABY_DRAGON_SOCKETS
import com.TheBudgeteers.dragonomics.DragonSockets.DRAGON_REFERENCE_WIDTH_DP
import com.TheBudgeteers.dragonomics.DragonSockets.DRAGON_SMALL_DP
import com.TheBudgeteers.dragonomics.DragonSockets.TEEN_DRAGON_SOCKETS
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch
import com.TheBudgeteers.dragonomics.gamify.DragonRules
import kotlin.math.roundToInt

class HomeActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener,
    AccessoryEquipListener {

    private lateinit var binding: ActivityHomeBinding

    // ViewModels
    private lateinit var dragonViewModel: DragonViewModel
    private lateinit var shopViewModel: ShopViewModel
    private lateinit var achievementsViewModel: AchievementsViewModel

    // Utility for DP to PX conversion
    private lateinit var displayMetrics: DisplayMetrics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        displayMetrics = resources.displayMetrics

        initializeViewModels()
        initializeDragonDisplay()
        setupButtons()
        setupBottomNavigation()
        setupBackButton()

        shopViewModel.setEquipListener(this)
    }

    private fun initializeViewModels() {
        val dragonFactory = DragonViewModel.Factory(this)
        dragonViewModel = ViewModelProvider(this, dragonFactory)[DragonViewModel::class.java]

        shopViewModel = ViewModelProvider(this)[ShopViewModel::class.java]
        achievementsViewModel = ViewModelProvider(this)[AchievementsViewModel::class.java]
    }

    private fun initializeDragonDisplay() {
        lifecycleScope.launch {
            dragonViewModel.uiState.collect { state ->
                // glide is being used here as this is the import that allows us to use gifs in image views
                Glide.with(this@HomeActivity)
                    .load(state.dragonImageRes)
                    .into(binding.dragon)

                binding.xpTxt.text =
                    "XP L${state.level} ${state.xpIntoLevel}/${DragonRules.XP_PER_LEVEL}"
                binding.xpProgress.setProgress(state.xpProgress, true)

                //  Mood Icon and Text
                binding.MoodImg.setImageResource(state.moodIconRes)
                val label = when (state.mood) {
                    DragonRules.Mood.HAPPY -> "Happy"
                    DragonRules.Mood.NEUTRAL -> "Neutral"
                    DragonRules.Mood.ANGRY -> "Angry"
                }
                binding.moodTxt.text = label
                binding.MoodImg.contentDescription = "Mood: $label"
                binding.MoodImg.tag = state.mood

                updateDragonCustomization(state)
            }
        }

        lifecycleScope.launch {
            shopViewModel.state.collect { state ->
                binding.currencyTxt.text = state.currency.toString()
            }
        }
    }

    // CUSTOMIZATION STUFF

    private fun updateDragonCustomization(state: DragonUiState) {
        // We still use View.post to ensure initial layout is stable.
        binding.dragon.post {

            //  Determining which set of sockets to use based on the current level. This is so the wings and horns fit correctly
            val currentSocketSet = when {
                state.level >= 10 -> ADULT_DRAGON_SOCKETS
                state.level >= 5 -> TEEN_DRAGON_SOCKETS
                else -> BABY_DRAGON_SOCKETS
            }


            val dragonWidthDp = DRAGON_SMALL_DP // fixed size, took scaling out
            val scaleFactor = dragonWidthDp / DRAGON_REFERENCE_WIDTH_DP.toFloat()

            // Helper method to convert dp to px, helps with scaleability
            fun dpToPx(dp: Int): Int {
                val scaledDp = dp * scaleFactor
                return (scaledDp * displayMetrics.density).roundToInt()
            }

            // Helper to update  the accessory view
            fun updateAccessoryView(
                imageView: ImageView,
                socket: DragonSockets.AttachmentPoint,
                itemId: String?,
                currentLevel: Int
            ) {
                val safeItemId = itemId ?: ""
                val drawables = getAccessoryDrawables(safeItemId, currentLevel)

                val accessoryRes = when (imageView) {
                    binding.hornLeft, binding.wingLeft -> drawables.leftResId
                    binding.hornRight, binding.wingRight -> drawables.rightResId
                    else -> 0
                }

                if (accessoryRes != 0) {
                    // Calculate scaled position and size
                    val x = dpToPx(socket.x)
                    val y = dpToPx(socket.y)
                    val w = dpToPx(socket.width)
                    val h = dpToPx(socket.height)

                    // Apply position and size
                    imageView.layoutParams = (imageView.layoutParams as ConstraintLayout.LayoutParams).apply {
                        width = w
                        height = h
                        marginStart = x
                        topMargin = y
                        marginEnd = 0
                        bottomMargin = 0
                    }


                    Glide.with(this@HomeActivity).load(accessoryRes).into(imageView)
                    imageView.visibility = ImageView.VISIBLE
                } else {

                    imageView.setImageDrawable(null)
                    imageView.visibility = ImageView.GONE
                }
            }

            // Apply updates to all accessories
            updateAccessoryView(binding.hornLeft, currentSocketSet.hornLeft, state.equippedHornsId, state.level)
            updateAccessoryView(binding.hornRight, currentSocketSet.hornRight, state.equippedHornsId, state.level)
            updateAccessoryView(binding.wingLeft, currentSocketSet.wingLeft, state.equippedWingsId, state.level)
            updateAccessoryView(binding.wingRight, currentSocketSet.wingRight, state.equippedWingsId, state.level)
        }
    }


    private fun getAccessoryDrawables(itemId: String, level: Int): DragonSockets.AccessoryDrawables {
        // This method will help us determine which form of the accessory is to be used. Eg is teen dragon is active, we want to use teen accessories
        val prefix = when {
            level >= 10 -> "adult_"
            level >= 5 -> "teen_"
            else -> "baby_"
        }

        //  This helper function to dynamically construct the resource name to find, thought this would be cleaner then straight up hard coding it
        // it takes the prefix from the level, then the middle part from the item selectec/equipped , this will then find it in our drawable folder and then apply it
        fun getResId(suffix: String): Int {
            val resourceName = "${prefix}${itemId}_$suffix"

            return resources.getIdentifier(resourceName, "drawable", packageName)
        }

        //  this will return both the left and right parts of the accessory
        return DragonSockets.AccessoryDrawables(
            leftResId = getResId("left"),
            rightResId = getResId("right")
        )
    }


    // Accessory listener ( i think its like events, atleast to my understanding)
    override fun onAccessoryEquipped(accessoryType: String, itemId: String) {
        // Passes the string and item id
        dragonViewModel.setEquippedAccessory(accessoryType, itemId)
    }

    private fun setupButtons() {
        binding.achievementsImg.setOnClickListener {
            AchievementsDialogFragment().show(
                supportFragmentManager,
                AchievementsDialogFragment.TAG
            )
        }

        binding.shopImg.setOnClickListener {
            ShopDialogFragment().show(
                supportFragmentManager,
                ShopDialogFragment.TAG
            )
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationView.apply {
            setOnItemSelectedListener { item -> onNavigationItemSelected(item) }
            itemIconTintList = null
        }
    }

    private fun setupBackButton() {
        onBackPressedDispatcher.addCallback(this) {
            val shopDialog = supportFragmentManager.findFragmentByTag(ShopDialogFragment.TAG)
            val achDialog = supportFragmentManager.findFragmentByTag(AchievementsDialogFragment.TAG)

            when {
                shopDialog?.isVisible == true -> (shopDialog as ShopDialogFragment).dismiss()
                achDialog?.isVisible == true -> (achDialog as AchievementsDialogFragment).dismiss()
                else -> finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        DragonMoodManager.getOverallMood(this)?.let { nestMood ->
            dragonViewModel.setOverallMood(nestMood.toDragonMood())
        }
    }

    private fun com.TheBudgeteers.dragonomics.models.Mood.toDragonMood():
            com.TheBudgeteers.dragonomics.gamify.DragonRules.Mood {
        return when (this) {
            com.TheBudgeteers.dragonomics.models.Mood.POSITIVE ->
                com.TheBudgeteers.dragonomics.gamify.DragonRules.Mood.HAPPY
            com.TheBudgeteers.dragonomics.models.Mood.NEUTRAL ->
                com.TheBudgeteers.dragonomics.gamify.DragonRules.Mood.NEUTRAL
            com.TheBudgeteers.dragonomics.models.Mood.NEGATIVE ->
                com.TheBudgeteers.dragonomics.gamify.DragonRules.Mood.ANGRY
        }
    }

    // ==================== Public API for Dragon Events ====================

    fun onUserLoggedExpense(addedPhoto: Boolean) {
        dragonViewModel.onExpenseLogged(addedPhoto)
    }

    fun onBudgetRecalculated(
        under80: Boolean,
        between80And100: Boolean,
        over: Boolean,
        betweenMinAndMax: Boolean,
        aboveMax: Boolean
    ) {
        dragonViewModel.onBudgetEvaluated(
            under80Percent = under80,
            between80And100 = between80And100,
            overBudget = over,
            betweenMinAndMaxGoal = betweenMinAndMax,
            aboveMaxGoal = aboveMax
        )
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
