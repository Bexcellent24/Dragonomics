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
import com.TheBudgeteers.dragonomics.data.Repository
import com.TheBudgeteers.dragonomics.data.SessionStore
import com.TheBudgeteers.dragonomics.databinding.ActivityProfileBinding
import com.TheBudgeteers.dragonomics.ui.adapters.QuestsAdapter
import com.TheBudgeteers.dragonomics.ui.profile.AvatarManager
import com.TheBudgeteers.dragonomics.utils.RepositoryProvider
import com.TheBudgeteers.dragonomics.utils.openIntent
import com.TheBudgeteers.dragonomics.viewmodel.AchievementsViewModel
import com.TheBudgeteers.dragonomics.viewmodel.ProfileViewModel
import com.TheBudgeteers.dragonomics.viewmodel.factories.ProfileViewModelFactory
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
    */

    // ViewBinding & adapters
    private lateinit var binding: ActivityProfileBinding
    private lateinit var achievementsViewModel: AchievementsViewModel
    private lateinit var questsAdapter: QuestsAdapter

    // Session + ViewModel
    private lateinit var session: SessionStore
    private lateinit var viewModel: ProfileViewModel

    // Per-user state - Changed from Long to String for Firebase UID
    private var currentUserId: String = ""
    private var avatarLocalUri: Uri? = null

    // Jetpack Photo Picker: pick an image and persist a local copy for this user
    private object PrefKeys {
        const val AVATAR_LOCAL = "avatar_local_uri"
        const val FIRST = "first_name"
        const val LAST = "last_name"
    }

    // begin code attribution
    // Pick an image with Jetpack Photo Picker and handle the result via the Activity Result API.
    // Adapted from:
    // Android Developers, 2023. Photo Picker. [online]
    // Available at: <https://developer.android.com/training/data-storage/shared/photopicker> [Accessed 6 October 2025].
    // Android Developers, 2020. Get results from an activity. [online]
    // Available at: <https://developer.android.com/training/basics/intents/result> [Accessed 6 October 2025].

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
    // end code attribution (Android Developers, 2023; Android Developers, 2020)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionStore(this)
        achievementsViewModel = ViewModelProvider(this)[AchievementsViewModel::class.java]

        setupBottomNav()
        setupQuestsList()  // This can stay here now that we initialize adapter first
        setupHeaderActions()

        // Session check + bootstrap
        lifecycleScope.launch {
            val userId = session.userId.firstOrNull()
            if (userId == null) {
                navigateToLogin()
                return@launch
            }

            currentUserId = userId
            initViewModel(userId)
            initPerUserUi()

            // Load quests AFTER we have userId
            loadQuests(userId)
        }
    }

    // Build ViewModel with repository + userId
    private fun initViewModel(userId: String) {
        val repository = RepositoryProvider.getRepository(this)
        viewModel = ViewModelProvider(
            this,
            ProfileViewModelFactory(repository, userId)
        )[ProfileViewModel::class.java]

        // Observe user data from database
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

    // begin code attribution
    // Set up a RecyclerView list with a LinearLayoutManager and adapter.
    // Adapted from:
    // Android Developers, 2020. Create a list with RecyclerView. [online]
    // Available at: <https://developer.android.com/develop/ui/views/layout/recyclerview#kotlin> [Accessed 6 October 2025].
    // RecyclerView + adapter: demo quests
    private fun setupQuestsList() {
        questsAdapter = QuestsAdapter { quest -> }

        binding.rvQuests.apply {
            adapter = questsAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }
    // end code attribution (Android Developers, 2020)

    // Add this new method to load quests when userId is available:
    private fun loadQuests(userId: String) {
        lifecycleScope.launch {
            try {
                achievementsViewModel.loadAchievements(userId)

                achievementsViewModel.achievements.collect { achievements ->
                    if (achievements.isEmpty()) {
                        binding.rvQuests.visibility = View.GONE
                    } else {
                        binding.rvQuests.visibility = View.VISIBLE
                        questsAdapter.submitList(achievements)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileActivity", "Error loading quests", e)
            }
        }
    }

    // Sign out: clear UI prefs for this profile + SessionStore
    private fun setupHeaderActions() {
        binding.btnLogout.setOnClickListener {
            lifecycleScope.launch {
                getProfilePrefs().edit { clear() }
                session.setUser(null)
                navigateToLogin()
            }
        }
    }

    // Restore avatar/name and wire edit panel actions for this specific user
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

        // Name, surname from UI prefs
        val first = prefs.getString(PrefKeys.FIRST, "") ?: ""
        val last = prefs.getString(PrefKeys.LAST, "") ?: ""
        binding.txtUsername.text = viewModel.getDisplayName(first, last)

        // Setup edit button
        binding.btnEdit.setOnClickListener {
            showEditOverlay()
        }

        // Setup overlay buttons
        binding.btnClosePanel.setOnClickListener { closeOverlay() }
        binding.btnCancel.setOnClickListener { closeOverlay() }
        binding.btnSave.setOnClickListener { saveProfileChanges() }
        binding.btnEditAvatar.setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    // Populate edit overlay fields and show it.
    private fun showEditOverlay() {
        val prefs = getProfilePrefs()
        binding.apply {
            etFirstName.setText(prefs.getString(PrefKeys.FIRST, "") ?: "")
            etLastName.setText(prefs.getString(PrefKeys.LAST, "") ?: "")

            // Load goals from ViewModel/database
            viewModel.user.value?.let { user ->
                etMinAmount.setText(user.minGoal?.toInt()?.toString() ?: "")
                etMaxAmount.setText(user.maxGoal?.toInt()?.toString() ?: "")
            }

            profileEditOverlay.visibility = View.VISIBLE
        }
    }

    // Persist name to prefs, goals via ViewModel, update the display, and close panel
    private fun saveProfileChanges() {
        val prefs = getProfilePrefs()

        binding.apply {
            val first = etFirstName.text.toString().trim()
            val last = etLastName.text.toString().trim()
            val minStr = etMinAmount.text.toString().trim()
            val maxStr = etMaxAmount.text.toString().trim()

            // Save name to SharedPreferences
            prefs.edit {
                putString(PrefKeys.FIRST, first)
                putString(PrefKeys.LAST, last)
            }

            // Save goals to database through ViewModel
            val minGoal = minStr.toDoubleOrNull()
            val maxGoal = maxStr.toDoubleOrNull()
            viewModel.updateGoals(minGoal, maxGoal)

            // Update name display immediately
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

    // Profile preferences are per-user (using Firebase UID)
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
// reference list
// Android Developers, 2023. Photo Picker. [online]
// Available at: <https://developer.android.com/training/data-storage/shared/photopicker> [Accessed 6 October 2025].
// Android Developers, 2020. Get results from an activity. [online]
// Available at: <https://developer.android.com/training/basics/intents/result> [Accessed 6 October 2025].
// Android Developers, 2020. Create a list with RecyclerView. [online]
// Available at: <https://developer.android.com/develop/ui/views/layout/recyclerview#kotlin> [Accessed 6 October 2025].