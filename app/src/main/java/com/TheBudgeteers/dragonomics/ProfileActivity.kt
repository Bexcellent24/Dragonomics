package com.TheBudgeteers.dragonomics

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.TheBudgeteers.dragonomics.data.SessionStore
import com.TheBudgeteers.dragonomics.databinding.ActivityProfileBinding
import com.TheBudgeteers.dragonomics.ui.adapters.QuestsAdapter
import com.TheBudgeteers.dragonomics.utils.RepositoryProvider
import com.TheBudgeteers.dragonomics.utils.openIntent
import com.TheBudgeteers.dragonomics.viewmodel.AchievementsViewModel
import com.TheBudgeteers.dragonomics.viewmodel.ProfileViewModel
import com.TheBudgeteers.dragonomics.viewmodel.factories.ProfileViewModelFactory
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class ProfileActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    /*
    Purpose:
      - Displays and edits user profile information from Firebase
      - Orchestrates profile UI wiring and session checks
      - Handles profile picture storage as Base64 in Firestore
    */

    // ViewBinding & adapters
    private lateinit var binding: ActivityProfileBinding
    private lateinit var achievementsViewModel: AchievementsViewModel
    private lateinit var questsAdapter: QuestsAdapter

    // Session + ViewModel
    private lateinit var session: SessionStore
    private lateinit var viewModel: ProfileViewModel

    // Per-user state - Firebase UID
    private var currentUserId: String = ""

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
            // Show the image immediately for instant feedback
            applyAvatar(pickerUri)

            // Save to Firestore as Base64 in background
            lifecycleScope.launch {
                uploadProfilePicture(pickerUri)
            }
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
        setupQuestsList()
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

        // Observe user data from Firebase
        lifecycleScope.launch {
            viewModel.user.collect { user ->
                user?.let {
                    // Update display name from Firebase data
                    binding.txtUsername.text = viewModel.getDisplayName()

                    // Update goals display
                    updateGoalsDisplay(it.minGoal, it.maxGoal)

                    // Load profile picture if available
                    if (it.profilePictureUrl.isNotEmpty()) {
                        loadProfilePicture(it.profilePictureUrl)
                    }
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

    private fun setupQuestsList() {
        questsAdapter = QuestsAdapter { quest -> }

        binding.rvQuests.apply {
            adapter = questsAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }
    // end code attribution (Android Developers, 2020)

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
                Log.e("ProfileActivity", "Error loading quests", e)
            }
        }
    }

    // Sign out: clear session
    private fun setupHeaderActions() {
        binding.btnLogout.setOnClickListener {
            lifecycleScope.launch {
                session.setUser(null)
                navigateToLogin()
            }
        }
    }

    // Wire up edit panel actions
    private fun initPerUserUi() {
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

    // Populate edit overlay fields from Firebase and show it
    private fun showEditOverlay() {
        binding.apply {
            // Load data from ViewModel (Firebase)
            viewModel.user.value?.let { user ->
                etFirstName.setText(user.firstName)
                etLastName.setText(user.lastName)
                etMinAmount.setText(user.minGoal?.toInt()?.toString() ?: "")
                etMaxAmount.setText(user.maxGoal?.toInt()?.toString() ?: "")
            }

            profileEditOverlay.visibility = View.VISIBLE
        }
    }

    // Save profile changes to Firebase through ViewModel
    private fun saveProfileChanges() {
        binding.apply {
            val first = etFirstName.text.toString().trim()
            val last = etLastName.text.toString().trim()
            val minStr = etMinAmount.text.toString().trim()
            val maxStr = etMaxAmount.text.toString().trim()

            // Save name to Firebase through ViewModel
            viewModel.updateProfile(first, last)

            // Save goals to Firebase through ViewModel
            val minGoal = minStr.toDoubleOrNull()
            val maxGoal = maxStr.toDoubleOrNull()
            viewModel.updateGoals(minGoal, maxGoal)

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
        Glide.with(this)
            .load(uri)
            .placeholder(R.drawable.default_avatar)
            .error(R.drawable.default_avatar)
            .circleCrop()
            .into(binding.ivAvatar)

        Glide.with(this)
            .load(uri)
            .placeholder(R.drawable.default_avatar)
            .error(R.drawable.default_avatar)
            .circleCrop()
            .into(binding.imgProfile)
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

    // Convert and save profile picture as Base64 in Firestore
    private suspend fun uploadProfilePicture(uri: Uri) {
        try {
            Log.d("ProfileActivity", "Converting image to Base64 for user: $currentUserId")

            // Read the image and convert to Base64
            val inputStream = contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()

            if (bytes == null) {
                Toast.makeText(this, "Failed to read image", Toast.LENGTH_SHORT).show()
                return
            }

            // Convert to Base64 string
            val base64Image = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
            val base64WithPrefix = "data:image/jpeg;base64,$base64Image"

            Log.d("ProfileActivity", "Image converted, size: ${bytes.size} bytes")

            // Save Base64 string to Firestore via ViewModel
            viewModel.updateProfilePicture(base64WithPrefix)

            Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("ProfileActivity", "Error saving profile picture", e)
            Toast.makeText(this, "Failed to save profile picture: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Load profile picture from Base64 string using Glide
    private fun loadProfilePicture(base64String: String) {
        Glide.with(this)
            .load(base64String)
            .placeholder(R.drawable.default_avatar)
            .error(R.drawable.default_avatar)
            .circleCrop()
            .into(binding.ivAvatar)

        Glide.with(this)
            .load(base64String)
            .placeholder(R.drawable.default_avatar)
            .error(R.drawable.default_avatar)
            .circleCrop()
            .into(binding.imgProfile)
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
// Android Developers, 2023. Photo Picker. [online]
// Available at: <https://developer.android.com/training/data-storage/shared/photopicker> [Accessed 6 October 2025].
// Android Developers, 2020. Get results from an activity. [online]
// Available at: <https://developer.android.com/training/basics/intents/result> [Accessed 6 October 2025].
// Android Developers, 2020. Create a list with RecyclerView. [online]
// Available at: <https://developer.android.com/develop/ui/views/layout/recyclerview#kotlin> [Accessed 6 October 2025].