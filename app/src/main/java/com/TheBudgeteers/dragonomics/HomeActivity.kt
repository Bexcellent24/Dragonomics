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
import com.bumptech.glide.Glide
import com.TheBudgeteers.dragonomics.databinding.ActivityHomeBinding
import com.TheBudgeteers.dragonomics.data.SessionStore
import com.TheBudgeteers.dragonomics.gamify.DragonMoodManager
import com.TheBudgeteers.dragonomics.models.NestType
import com.TheBudgeteers.dragonomics.ui.AchievementsDialogFragment
import com.TheBudgeteers.dragonomics.ui.ShopDialogFragment
import com.TheBudgeteers.dragonomics.utils.RepositoryProvider
import com.TheBudgeteers.dragonomics.viewmodel.AchievementsViewModel
import com.TheBudgeteers.dragonomics.viewmodel.DragonUiState
import com.TheBudgeteers.dragonomics.viewmodel.DragonViewModel
import com.TheBudgeteers.dragonomics.viewmodel.NestViewModel
import com.TheBudgeteers.dragonomics.viewmodel.ShopViewModel
import com.TheBudgeteers.dragonomics.viewmodel.AccessoryEquipListener
import com.TheBudgeteers.dragonomics.DragonSockets.ADULT_DRAGON_SOCKETS
import com.TheBudgeteers.dragonomics.DragonSockets.BABY_DRAGON_SOCKETS
import com.TheBudgeteers.dragonomics.DragonSockets.DRAGON_REFERENCE_WIDTH_DP
import com.TheBudgeteers.dragonomics.DragonSockets.DRAGON_SMALL_DP
import com.TheBudgeteers.dragonomics.DragonSockets.TEEN_DRAGON_SOCKETS
import com.TheBudgeteers.dragonomics.utilities.PaletteColors
import com.TheBudgeteers.dragonomics.utilities.PaletteMapper
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import com.TheBudgeteers.dragonomics.gamify.DragonRules
import kotlin.math.roundToInt

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

        initializeViewModels()
        initializeDragonDisplay()
        setupButtons()
        setupBottomNavigation()
        setupBackButton()

        shopViewModel.setEquipListener(this)

        // Calculate mood on activity creation
        updateOverallMoodFromNests()
    }

    private fun initializeViewModels() {
        val dragonFactory = DragonViewModel.Factory(this)
        dragonViewModel = ViewModelProvider(this, dragonFactory)[DragonViewModel::class.java]

        shopViewModel = ViewModelProvider(this)[ShopViewModel::class.java]
        achievementsViewModel = ViewModelProvider(this)[AchievementsViewModel::class.java]

        val repository = RepositoryProvider.getRepository(this)
        nestViewModel = NestViewModel(repository)
        sessionStore = SessionStore(this)
    }

    private fun initializeDragonDisplay() {
        lifecycleScope.launch { //------------CODE ATTRIBUTION------------
//Title: Getting started with glider
//Author: Glider
//Date: 05/10/2025
//Code Version: v4
//Availability: https://bumptech.github.io/glide/doc/getting-started.html
            dragonViewModel.uiState.collect { state ->
                Glide.with(this@HomeActivity)
                    .load(state.dragonImageRes)
                    .into(binding.dragon)
//------------ END OF CODE ATTRIBUTION------------ ( Glider, 2025)
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
                state.level >= 10 -> DragonSockets.ADULT_DRAGON_SOCKETS
                state.level >= 5 -> DragonSockets.TEEN_DRAGON_SOCKETS
                else -> DragonSockets.BABY_DRAGON_SOCKETS
            }

            // gets the dual color IDs from the mapper
            val paletteColors: PaletteColors? = PaletteMapper.mapPaletteIdToColors(this, state.equippedPaletteId)

            // color Filter creation logic
            val bodyColorFilter: ColorFilter? = paletteColors?.bodyColorRes?.let { resId ->
                val colorInt = ContextCompat.getColor(this, resId)
                PorterDuffColorFilter(colorInt, PorterDuff.Mode.MULTIPLY)
            }
            val accessoryColorFilter: ColorFilter? = paletteColors?.accessoryColorRes?.let { resId ->
                val colorInt = ContextCompat.getColor(this, resId)
                PorterDuffColorFilter(colorInt, PorterDuff.Mode.MULTIPLY)
            }
            binding.dragon.colorFilter = bodyColorFilter

            // scaling cal. To ensure its the same across the board, no matter the screen size
            val dragonPxWidth = binding.dragon.width.toFloat()

            // Use the ratio of the physical width (PX) to the design reference DP width.
            val finalScaleRatio = dragonPxWidth / DragonSockets.DRAGON_REFERENCE_WIDTH_DP.toFloat()

            fun dpToPx(dp: Int): Int {
                return (dp * finalScaleRatio).roundToInt()
            }


            val DRAGON_PADDING_DP = 70
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

                    imageView.layoutParams = (imageView.layoutParams as ConstraintLayout.LayoutParams).apply {
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

            // passes the specific accessoryColorFilter to all accessory updates
            updateAccessoryView(binding.hornLeft, currentSocketSet.hornLeft, state.equippedHornsId, state.level, accessoryColorFilter)
            updateAccessoryView(binding.hornRight, currentSocketSet.hornRight, state.equippedHornsId, state.level, accessoryColorFilter)
            updateAccessoryView(binding.wingLeft, currentSocketSet.wingLeft, state.equippedWingsId, state.level, accessoryColorFilter)
            updateAccessoryView(binding.wingRight, currentSocketSet.wingRight, state.equippedWingsId, state.level, accessoryColorFilter)
        }
    }

    private fun getAccessoryDrawables(itemId: String, level: Int): DragonSockets.AccessoryDrawables {
        val prefix = when {
            level >= 10 -> "adult_"
            level >= 5 -> "teen_"
            else -> "baby_"
        }
// helper method that helps us dynamically finds the names of our accessories. It checks what level the user is on
        // then based on that level, we will get the first prefix. Whatever the user selects in the  store will be the second prefix

        fun getResId(suffix: String): Int {
            val resourceName = "${prefix}${itemId}_$suffix"
            return resources.getIdentifier(resourceName, "drawable", packageName)
        }
// suffix will alwayss return both left and right so we can get both the components
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
        updateOverallMoodFromNests()
    }

    private fun updateOverallMoodFromNests() {
        lifecycleScope.launch {
            val userId = sessionStore.userId.firstOrNull()

            if (userId == null) {
                return@launch
            }

            val (mood, _) = nestViewModel.getOverallMood(
                userId = userId,
                type = NestType.EXPENSE,
                weighting = NestViewModel.Weighting.BUDGET
            )

            DragonMoodManager.setOverallMood(this@HomeActivity, mood)
            dragonViewModel.setOverallMood(mood.toDragonMood())
        }
    }

    private fun com.TheBudgeteers.dragonomics.models.Mood.toDragonMood(): DragonRules.Mood {
        return when (this) {
            com.TheBudgeteers.dragonomics.models.Mood.POSITIVE -> DragonRules.Mood.HAPPY
            com.TheBudgeteers.dragonomics.models.Mood.NEUTRAL -> DragonRules.Mood.NEUTRAL
            com.TheBudgeteers.dragonomics.models.Mood.NEGATIVE -> DragonRules.Mood.ANGRY
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
// reference list
//The Independent Institute of Education. 2025. Open Source Coding Module Manuel  [OPSC 7311]. nt. [online via internal VLE] The Independent Institute of Education. Available at: <https://advtechonline.sharepoint.com/:w:/r/sites/TertiaryStudents/_layouts/15/Doc.aspx?sourcedoc=%7BD5C243B5-895D-4B63-B083-140930EF9734%7D&file=OPSC7311MM.docx&action=default&mobileredirect=true> [Accessed Date 03 October 2025].