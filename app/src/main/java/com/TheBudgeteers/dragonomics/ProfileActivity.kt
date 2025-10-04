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

    private lateinit var binding: ActivityProfileBinding
    private lateinit var questsAdapter: QuestsAdapter
    private lateinit var session: SessionStore
    private lateinit var viewModel: ProfileViewModel

    private var currentUserId: Long = -1L
    private var avatarLocalUri: Uri? = null

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

        lifecycleScope.launch {
            val userId = session.userId.firstOrNull()
            if (userId == null) {
                navigateToLogin()
                return@launch
            }

            // CRITICAL: Set user ID FIRST before any UI operations
            currentUserId = userId

            // Initialize ViewModel - this starts loading user data
            initViewModel(userId)

            // Then setup UI components
            initPerUserUi()
        }
    }

    private fun initViewModel(userId: Long) {
        val repository = RepositoryProvider.getRepository(this)
        viewModel = ViewModelProvider(
            this,
            ProfileViewModelFactory(repository, userId)
        )[ProfileViewModel::class.java]

        // Observe user data from database - this is the single source of truth for goals
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

    private fun setupQuestsList() {
        binding.rvQuests.apply {
            if (layoutManager == null) layoutManager = LinearLayoutManager(this@ProfileActivity)
            setHasFixedSize(true)
        }

        questsAdapter = QuestsAdapter {
            // Handle quest click - for now just show edit overlay as placeholder
            binding.profileEditOverlay.visibility = View.VISIBLE
        }
        binding.rvQuests.adapter = questsAdapter

        // Load demo quests (will be replaced with real data later)
        questsAdapter.submitList(getDemoQuests())
    }

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

    private fun setupHeaderActions() {
        binding.btnLogout.setOnClickListener {
            lifecycleScope.launch {
                getProfilePrefs().edit { clear() }
                session.setUser(null)
                navigateToLogin()
            }
        }
    }

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

        // Apply name from SharedPreferences
        // Goals will be applied by ViewModel Flow observer
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

    private fun saveProfileChanges() {
        val prefs = getProfilePrefs()

        binding.apply {
            val first = etFirstName.text.toString().trim()
            val last = etLastName.text.toString().trim()
            val minStr = etMinAmount.text.toString().trim()
            val maxStr = etMaxAmount.text.toString().trim()

            // Save name to SharedPreferences (UI-only data)
            prefs.edit {
                putString(PrefKeys.FIRST, first)
                putString(PrefKeys.LAST, last)
            }

            // Save goals to database through ViewModel
            val minGoal = minStr.toDoubleOrNull()
            val maxGoal = maxStr.toDoubleOrNull()
            viewModel.updateGoals(minGoal, maxGoal)

            // Update name display immediately
            // Goals will update automatically via Flow observer
            binding.txtUsername.text = viewModel.getDisplayName(first, last)

            closeOverlay()
        }
    }

    private fun updateGoalsDisplay(minGoal: Double?, maxGoal: Double?) {
        binding.txtMinMonthAmount.text = formatAmount(minGoal)
        binding.txtMaxMonthAmount.text = formatAmount(maxGoal)
    }

    private fun formatAmount(amount: Double?): String {
        // FIXED: Removed hardcoded "13,000" fallback
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