package com.TheBudgeteers.dragonomics

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.TheBudgeteers.dragonomics.utils.RepositoryProvider
import com.TheBudgeteers.dragonomics.data.SessionStore
import com.TheBudgeteers.dragonomics.databinding.ActivityProfileBinding
import com.TheBudgeteers.dragonomics.models.Quest
import com.TheBudgeteers.dragonomics.ui.QuestsAdapter
import com.TheBudgeteers.dragonomics.viewmodel.ProfileViewModel
import com.TheBudgeteers.dragonomics.viewmodel.ProfileViewModelFactory
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class ProfileActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

/*
Purpose:
  - Displays and edits user profile information
  - Orchestrates profile UI wiring and session checks.
  - Bridges ViewModel with lightweight UI prefs

References:
 - Android official docs: Activities & lifecycle, ViewModel & factories, coroutines with Lifecycle, and ViewBinding.
     * Activities & lifecycle: https://developer.android.com/guide/components/activities/intro-activities
     * Lifecycle-aware coroutines (lifecycleScope/Flow): https://developer.android.com/topic/libraries/architecture/coroutines
 - Android official docs: RecyclerView.
     * Create a list with RecyclerView: https://developer.android.com/develop/ui/views/layout/recyclerview#kotlin
 - Android official docs: Photo Picker & Activity Result APIs.
     * Jetpack Photo Picker (PickVisualMedia): https://developer.android.com/training/data-storage/shared/photopicker
     * Register for activity results: https://developer.android.com/training/basics/intents/result
 - Android official docs: UI & navigation bits.
     * Tasks & back stack (Intent flags): https://developer.android.com/guide/components/activities/tasks-and-back-stack
     * InputMethodManager (hide keyboard): https://developer.android.com/reference/android/view/inputmethod/InputMethodManager

Author: Android | Date: 2025-10-05
*/

    // ViewBinding & adapters
    private lateinit var binding: ActivityProfileBinding
    private lateinit var questsAdapter: QuestsAdapter

    // Session + ViewModel
    private lateinit var session: SessionStore
    private lateinit var viewModel: ProfileViewModel

    // Per-user state
    private var currentUserId: Long = -1L
    private var avatarLocalUri: Uri? = null

    // Jetpack Photo Picker: pick an image and persist a local copy for this user
    private object PrefKeys {
        const val AVATAR_LOCAL = "avatar_local_uri"
        const val FIRST = "first_name"
        const val LAST = "last_name"
    }

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { pickerUri ->
        if (pickerUri != null) {
            val local = AvatarManager.copyToAppStorage(this, pickerUri, currentUserId)
                ?: return@registerForActivityResult
            avatarLocalUri = local
            applyAvatar(local)
            getProfilePrefs().edit { putString(PrefKeys.AVATAR_LOCAL, local.toString()) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionStore(this)

        setupBottomNav()
        setupQuestsList()
        setupHeaderActions()

        // Session check + bootstrap: ensure we have a userId before wiring user-specific UI
        lifecycleScope.launch {
            val userId = session.userId.firstOrNull()
            if (userId == null) {
                navigateToLogin()
                return@launch
            }

            //Set user ID FIRST before any UI operations
            currentUserId = userId

            // Initialise ViewModel
            initViewModel(userId)

            // Then setup UI components
            initPerUserUi()
        }
    }

    //Build ViewModel with repository + userId
    private fun initViewModel(userId: Long) {
        val repository = RepositoryProvider.getRepository(this)
        viewModel = ViewModelProvider(
            this,
            ProfileViewModelFactory(repository, userId)
        )[ProfileViewModel::class.java]

        //Observe user data from database
        lifecycleScope.launch {
            viewModel.user.collect { user ->
                user?.let {
                    updateGoalsDisplay(it.minGoal, it.maxGoal)

                    // Update display name when user data loads
                    val prefs = getProfilePrefs()
                    val first = prefs.getString(PrefKeys.FIRST, "") ?: ""
                    val last = prefs.getString(PrefKeys.LAST, "") ?: ""
                    binding.txtUsername.text = viewModel.getDisplayName(first, last)
                }
            }
        }
    }


    private fun setupBottomNav() {
        binding.bottomNavigationView.itemIconTintList = null
        binding.bottomNavigationView.setOnItemSelectedListener {
            onNavigationItemSelected(it)
            true
        }
        binding.bottomNavigationView.menu.findItem(R.id.nav_profile)?.apply {
            isCheckable = false
            isChecked = false
        }
    }

    //RecyclerView + adapter: demo quests
    private fun setupQuestsList() {
        binding.rvQuests.apply {
            if (layoutManager == null) layoutManager = LinearLayoutManager(this@ProfileActivity)
            setHasFixedSize(true)
        }

        questsAdapter = QuestsAdapter {
            binding.profileEditOverlay.visibility = View.VISIBLE
        }
        binding.rvQuests.adapter = questsAdapter

        // Load demo quests
        questsAdapter.submitList(getDemoQuests())
    }

    //Demo Quests
    private fun getDemoQuests(): List<Quest> {
        return listOf(
            Quest(
                id = "q1",
                title = "1 day streak",
                iconRes = R.drawable.streak,
                rewardText = "+10 XP",
                completed = false
            ),
            Quest(
                id = "q2",
                title = "Log 3 expenses",
                iconRes = R.drawable.nest,
                rewardText = "+15 XP",
                completed = false
            ),
            Quest(
                id = "q3",
                title = "Hit your min goal this week",
                iconRes = R.drawable.incoming_nest,
                rewardText = null,
                completed = true
            )
        )
    }

    //Sign out: clear UI prefs for this profile + SessionStore
    private fun setupHeaderActions() {
        binding.btnLogout.setOnClickListener {
            lifecycleScope.launch {
                getProfilePrefs().edit { clear() }
                session.setUser(null)
                navigateToLogin()
            }
        }
    }

    //Restore avatar/name and wire edit panel actions for this specific user
    private fun initPerUserUi() {
        val prefs = getProfilePrefs()

        // Restore avatar
        prefs.getString(PrefKeys.AVATAR_LOCAL, null)?.let { saved ->
            runCatching {
                Uri.parse(saved)
            }.getOrNull()?.let { local ->
                runCatching {
                    applyAvatar(local)
                }.onFailure {
                    prefs.edit { remove(PrefKeys.AVATAR_LOCAL) }
                }
                avatarLocalUri = local
            }
        }

        //Name, surname from UI prefs
        val first = prefs.getString(PrefKeys.FIRST, "") ?: ""
        val last = prefs.getString(PrefKeys.LAST, "") ?: ""
        binding.txtUsername.text = viewModel.getDisplayName(first, last)

        //Setup edit button
        binding.btnEdit.setOnClickListener {
            showEditOverlay()
        }

        //Setup overlay buttons
        binding.btnClosePanel.setOnClickListener { closeOverlay() }
        binding.btnCancel.setOnClickListener { closeOverlay() }
        binding.btnSave.setOnClickListener { saveProfileChanges() }
        binding.btnEditAvatar.setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    //Populate edit overlay fields and show it.
    private fun showEditOverlay() {
        val prefs = getProfilePrefs()
        binding.apply {
            etFirstName.setText(prefs.getString(PrefKeys.FIRST, "") ?: "")
            etLastName.setText(prefs.getString(PrefKeys.LAST, "") ?: "")

            //Load goals from ViewModel/database
            viewModel.user.value?.let { user ->
                etMinAmount.setText(user.minGoal?.toInt()?.toString() ?: "")
                etMaxAmount.setText(user.maxGoal?.toInt()?.toString() ?: "")
            }

            profileEditOverlay.visibility = View.VISIBLE
        }
    }

    //Persist name to prefs, goals via ViewModel, update the display, and close panel
    private fun saveProfileChanges() {
        val prefs = getProfilePrefs()

        binding.apply {
            val first = etFirstName.text.toString().trim()
            val last = etLastName.text.toString().trim()
            val minStr = etMinAmount.text.toString().trim()
            val maxStr = etMaxAmount.text.toString().trim()

            //Save name to SharedPreferences
            prefs.edit {
                putString(PrefKeys.FIRST, first)
                putString(PrefKeys.LAST, last)
            }

            //Save goals to database through ViewModel
            val minGoal = minStr.toDoubleOrNull()
            val maxGoal = maxStr.toDoubleOrNull()
            viewModel.updateGoals(minGoal, maxGoal)

            //Update name display immediately
            binding.txtUsername.text = viewModel.getDisplayName(first, last)

            closeOverlay()
        }
    }

    private fun updateGoalsDisplay(minGoal: Double?, maxGoal: Double?) {
        binding.txtMinMonthAmount.text = formatAmount(minGoal)
        binding.txtMaxMonthAmount.text = formatAmount(maxGoal)
    }

    private fun formatAmount(amount: Double?): String {
        if (amount == null) return "Not Set"
        return NumberFormat.getNumberInstance(Locale.getDefault()).format(amount.toInt())
    }

    private fun applyAvatar(uri: Uri) {
        binding.ivAvatar.setImageURI(uri)
        binding.imgProfile.setImageURI(uri)
    }

    private fun closeOverlay() {
        binding.profileEditOverlay.visibility = View.GONE
        hideKeyboard()
    }

    private fun hideKeyboard() {
        currentFocus?.let { v ->
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(v.windowToken, 0)
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
        }
        startActivity(intent)
        finish()
    }

    //Profile preferences are per-user
    private fun getProfilePrefs() =
        getSharedPreferences("profile_prefs_u_$currentUserId", Context.MODE_PRIVATE)

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