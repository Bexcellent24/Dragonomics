package com.TheBudgeteers.dragonomics

import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.MenuItem
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.TheBudgeteers.dragonomics.data.Mood
import com.TheBudgeteers.dragonomics.data.NestType
import com.TheBudgeteers.dragonomics.data.Repository
import com.bumptech.glide.Glide
import com.TheBudgeteers.dragonomics.databinding.ActivityHomeBinding
import com.TheBudgeteers.dragonomics.data.SessionStore
import com.TheBudgeteers.dragonomics.gamify.DragonMoodManager
import com.TheBudgeteers.dragonomics.ui.AchievementsDialogFragment
import com.TheBudgeteers.dragonomics.ui.ShopDialogFragment
import com.TheBudgeteers.dragonomics.viewmodel.AchievementsViewModel
import com.TheBudgeteers.dragonomics.viewmodel.DragonUiState
import com.TheBudgeteers.dragonomics.viewmodel.DragonViewModel
import com.TheBudgeteers.dragonomics.viewmodel.NestViewModel
import com.TheBudgeteers.dragonomics.viewmodel.ShopViewModel
import com.TheBudgeteers.dragonomics.viewmodel.AccessoryEquipListener
import com.TheBudgeteers.dragonomics.viewmodel.factories.NestViewModelFactory
import com.TheBudgeteers.dragonomics.ui.dragon.DragonSockets.ADULT_DRAGON_SOCKETS
import com.TheBudgeteers.dragonomics.ui.dragon.DragonSockets.BABY_DRAGON_SOCKETS
import com.TheBudgeteers.dragonomics.ui.dragon.DragonSockets.DRAGON_REFERENCE_WIDTH_DP
import com.TheBudgeteers.dragonomics.ui.dragon.DragonSockets.DRAGON_VIEW_PADDING_DP
import com.TheBudgeteers.dragonomics.ui.dragon.DragonSockets.TEEN_DRAGON_SOCKETS
import com.TheBudgeteers.dragonomics.utilities.PaletteColors
import com.TheBudgeteers.dragonomics.utilities.PaletteMapper
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import com.TheBudgeteers.dragonomics.gamify.DragonRules
import com.TheBudgeteers.dragonomics.ui.dragon.DragonSockets
import com.TheBudgeteers.dragonomics.utils.openIntent
import kotlin.math.roundToInt

/**
 * HomeActivity - Main screen showing the dragon and its progression.
 * UPDATED FOR FIREBASE: Now initializes ViewModels with userId from SessionStore.
 */
class HomeActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener,
    AccessoryEquipListener {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var dragonViewModel: DragonViewModel
    private lateinit var shopViewModel: ShopViewModel
    private lateinit var achievementsViewModel: AchievementsViewModel
    private lateinit var nestViewModel: NestViewModel
    private lateinit var sessionStore: SessionStore
    private lateinit var displayMetrics: DisplayMetrics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        displayMetrics = resources.displayMetrics
        sessionStore = SessionStore(this)

        // Initialize ViewModels and load user data
        lifecycleScope.launch {
            val userId = sessionStore.userId.firstOrNull()
            android.util.Log.d("HomeActivity", "Got userId: $userId")

            if (userId == null) {
                android.util.Log.e("HomeActivity", "No userId found!")
                return@launch
            }

            initializeViewModels()

            // Initialize ViewModels with userId to load Firebase data
            dragonViewModel.initialize(userId)
            shopViewModel.initialize(userId)

            initializeDragonDisplay()
            setupButtons()
            setupBottomNavigation()
            setupBackButton()

            shopViewModel.setEquipListener(this@HomeActivity)

            // Calculate mood on activity creation
            updateOverallMoodFromNests(userId)
        }
    }

    private fun initializeViewModels() {
        val dragonFactory = DragonViewModel.Factory(this)
        dragonViewModel = ViewModelProvider(this, dragonFactory)[DragonViewModel::class.java]

        shopViewModel = ViewModelProvider(this)[ShopViewModel::class.java]
        achievementsViewModel = ViewModelProvider(this)[AchievementsViewModel::class.java]

        val repository = Repository()
        nestViewModel = ViewModelProvider(this, NestViewModelFactory(repository))[NestViewModel::class.java]
    }

    private fun initializeDragonDisplay() {
        lifecycleScope.launch {
            dragonViewModel.uiState.collect { state ->
                Glide.with(this@HomeActivity)
                    .load(state.dragonImageRes)
                    .into(binding.dragon)

                binding.xpTxt.text =
                    "XP L${state.level} ${state.xpIntoLevel}/${DragonRules.XP_PER_LEVEL}"
                binding.xpProgress.setProgress(state.xpProgress, true)

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

    private fun updateDragonCustomization(state: DragonUiState) {
        binding.dragon.post {
            val currentSocketSet = when {
                state.level >= 10 -> ADULT_DRAGON_SOCKETS
                state.level >= 5 -> TEEN_DRAGON_SOCKETS
                else -> BABY_DRAGON_SOCKETS
            }

            // Get the dual color IDs from the mapper
            val paletteColors: PaletteColors? =
                PaletteMapper.mapPaletteIdToColors(this, state.equippedPaletteId)

            // Color Filter creation logic
            val bodyColorFilter: ColorFilter? = paletteColors?.bodyColorRes?.let { resId ->
                val colorInt = ContextCompat.getColor(this, resId)
                PorterDuffColorFilter(colorInt, PorterDuff.Mode.MULTIPLY)
            }
            val accessoryColorFilter: ColorFilter? =
                paletteColors?.accessoryColorRes?.let { resId ->
                    val colorInt = ContextCompat.getColor(this, resId)
                    PorterDuffColorFilter(colorInt, PorterDuff.Mode.MULTIPLY)
                }
            binding.dragon.colorFilter = bodyColorFilter

            // Scaling calculation to ensure it's the same across the board
            val dragonPxWidth = binding.dragon.width.toFloat()
            val finalScaleRatio = dragonPxWidth / DRAGON_REFERENCE_WIDTH_DP.toFloat()

            fun dpToPx(dp: Int): Int {
                return (dp * finalScaleRatio).roundToInt()
            }

            val DRAGON_PADDING_DP = DRAGON_VIEW_PADDING_DP
            val paddingOffsetPx = dpToPx(DRAGON_PADDING_DP)

            fun updateAccessoryView(
                imageView: ImageView,
                socket: DragonSockets.AttachmentPoint,
                itemId: String?,
                currentLevel: Int,
                colorFilter: ColorFilter?
            ) {
                val safeItemId = itemId ?: ""
                val drawables = getAccessoryDrawables(safeItemId, currentLevel)

                val accessoryRes = when (imageView) {
                    binding.hornLeft, binding.wingLeft -> drawables.leftResId
                    binding.hornRight, binding.wingRight -> drawables.rightResId
                    else -> 0
                }

                if (accessoryRes != 0) {
                    val x = dpToPx(socket.x) + paddingOffsetPx
                    val y = dpToPx(socket.y) + paddingOffsetPx
                    val w = dpToPx(socket.width)
                    val h = dpToPx(socket.height)

                    imageView.layoutParams =
                        (imageView.layoutParams as ConstraintLayout.LayoutParams).apply {
                            width = w
                            height = h
                            marginStart = x
                            topMargin = y
                            marginEnd = 0
                            bottomMargin = 0
                        }

                    Glide.with(this@HomeActivity).load(accessoryRes).into(imageView)
                    imageView.colorFilter = colorFilter
                    imageView.visibility = ImageView.VISIBLE
                } else {
                    imageView.setImageDrawable(null)
                    imageView.visibility = ImageView.GONE
                }
            }

            // Apply equipped accessories
            updateAccessoryView(
                binding.hornLeft,
                currentSocketSet.hornLeft,
                state.equippedHornsId,
                state.level,
                accessoryColorFilter
            )
            updateAccessoryView(
                binding.hornRight,
                currentSocketSet.hornRight,
                state.equippedHornsId,
                state.level,
                accessoryColorFilter
            )
            updateAccessoryView(
                binding.wingLeft,
                currentSocketSet.wingLeft,
                state.equippedWingsId,
                state.level,
                accessoryColorFilter
            )
            updateAccessoryView(
                binding.wingRight,
                currentSocketSet.wingRight,
                state.equippedWingsId,
                state.level,
                accessoryColorFilter
            )
        }
    }

    private fun getAccessoryDrawables(
        itemId: String,
        level: Int
    ): DragonSockets.AccessoryDrawables {
        val prefix = when {
            level >= 10 -> "adult_"
            level >= 5 -> "teen_"
            else -> "baby_"
        }

        fun getResId(suffix: String): Int {
            val resourceName = "${prefix}${itemId}_$suffix"
            return resources.getIdentifier(resourceName, "drawable", packageName)
        }

        return DragonSockets.AccessoryDrawables(
            leftResId = getResId("left"),
            rightResId = getResId("right")
        )
    }

    override fun onAccessoryEquipped(accessoryType: String, itemId: String) {
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
        lifecycleScope.launch {
            val userId = sessionStore.userId.firstOrNull() ?: return@launch
            updateOverallMoodFromNests(userId)
        }
    }

    private fun updateOverallMoodFromNests(userId: String) {
        lifecycleScope.launch {
            val (mood, _) = nestViewModel.getOverallMood(
                userId = userId,
                type = NestType.EXPENSE,
                weighting = NestViewModel.Weighting.BUDGET
            )

            DragonMoodManager.setOverallMood(this@HomeActivity, mood)
            dragonViewModel.setOverallMood(mood.toDragonMood())
        }
    }

    private fun Mood.toDragonMood(): DragonRules.Mood {
        return when (this) {
            Mood.POSITIVE -> DragonRules.Mood.HAPPY
            Mood.NEUTRAL -> DragonRules.Mood.NEUTRAL
            Mood.NEGATIVE -> DragonRules.Mood.ANGRY
        }
    }

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