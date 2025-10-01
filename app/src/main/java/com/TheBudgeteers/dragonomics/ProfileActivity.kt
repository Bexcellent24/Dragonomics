package com.TheBudgeteers.dragonomics

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.TheBudgeteers.dragonomics.data.SessionStore
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale

class ProfileActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var rvQuests: RecyclerView
    private lateinit var questsAdapter: QuestsAdapter

    // Header views
    private lateinit var tvUserName: TextView
    private lateinit var tvMinAmt: TextView
    private lateinit var tvMaxAmt: TextView

    // Overlay views
    private lateinit var overlay: View
    private lateinit var etFirst: EditText
    private lateinit var etLast: EditText
    private lateinit var etMin: EditText
    private lateinit var etMax: EditText

    // Avatar
    private var avatarLocalUri: Uri? = null

    // Session / per-user prefs
    private lateinit var session: SessionStore
    private lateinit var prefs: SharedPreferences
    private var currentUserId: Long = -1L

    private object Keys {
        const val AVATAR_LOCAL = "avatar_local_uri"
        const val FIRST = "first_name"
        const val LAST  = "last_name"
        const val MIN   = "min_amount"
        const val MAX   = "max_amount"
    }

    private fun profilePrefsFor(userId: Long): SharedPreferences =
        getSharedPreferences("profile_prefs_u_$userId", Context.MODE_PRIVATE)

    /** Photo Picker → copy to per-user app files */
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { pickerUri ->
        if (pickerUri != null) {
            val local = copyToAppStorage(pickerUri) ?: return@registerForActivityResult
            avatarLocalUri = local
            applyAvatar(local)
            if (::prefs.isInitialized) {
                prefs.edit { putString(Keys.AVATAR_LOCAL, local.toString()) }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        // ---- bind header views
        tvUserName = findViewById(R.id.txt_username)
        tvMinAmt   = findViewById(R.id.txt_min_month_amount)
        tvMaxAmt   = findViewById(R.id.txt_max_month_amount)

        // ---- bind overlay views
        overlay = findViewById(R.id.profileEditOverlay)
        etFirst  = findViewById(R.id.etFirstName)
        etLast   = findViewById(R.id.etLastName)
        etMin    = findViewById(R.id.etMinAmount)
        etMax    = findViewById(R.id.etMaxAmount)

        // Bottom nav
        bottomNav = findViewById(R.id.bottomNavigationView)
        bottomNav.itemIconTintList = null
        bottomNav.setOnItemSelectedListener { onNavigationItemSelected(it); true }
        bottomNav.menu.findItem(R.id.nav_profile)?.apply { isCheckable = false; isChecked = false }

        // Header actions (Edit / Logout)
        wireHeaderActions()

        // Quests list
        rvQuests = findViewById(R.id.rvQuests)
        if (rvQuests.layoutManager == null) rvQuests.layoutManager = LinearLayoutManager(this)
        rvQuests.setHasFixedSize(true)

        questsAdapter = QuestsAdapter { overlay.visibility = View.VISIBLE }
        rvQuests.adapter = questsAdapter

        val demoQuests = listOf(
            Quest(id="q1", title="1 day streak",                iconRes=R.drawable.streak,        rewardText="+10 XP", completed=false),
            Quest(id="q2", title="Log 3 expenses",              iconRes=R.drawable.nest,          rewardText="+15 XP", completed=false),
            Quest(id="q3", title="Hit your min goal this week", iconRes=R.drawable.incoming_nest, rewardText=null,     completed=true)
        )
        questsAdapter.submitList(demoQuests)

        // Resolve current session user and init per-user UI
        session = SessionStore(this)

        lifecycleScope.launch {
            val id = session.userId.firstOrNull()
            if (id == null) {
                // if (!BuildConfig.DEBUG) {
                //     startActivity(Intent(this@ProfileActivity, LoginActivity::class.java))
                //     finish()
                // }
                return@launch
            }
            currentUserId = id
            prefs = profilePrefsFor(id)
            initPerUserUi()
        }
    }

    private fun initPerUserUi() {
        // Restore avatar from THIS user's saved uri
        prefs.getString(Keys.AVATAR_LOCAL, null)?.let { saved ->
            runCatching { Uri.parse(saved) }.getOrNull()?.let { local ->
                runCatching { applyAvatar(local) }.onFailure {
                    prefs.edit { remove(Keys.AVATAR_LOCAL) }
                }
                avatarLocalUri = local
            }
        }

        // Populate header from THIS user's prefs
        applyDisplayFromPrefs()

        // Overlay wiring
        findViewById<View>(R.id.btn_edit).setOnClickListener {
            etFirst.setText(prefs.getString(Keys.FIRST, "") ?: "")
            etLast.setText(prefs.getString(Keys.LAST, "") ?: "")
            etMin.setText(prefs.getString(Keys.MIN, "") ?: "")
            etMax.setText(prefs.getString(Keys.MAX, "") ?: "")
            overlay.visibility = View.VISIBLE
        }

        findViewById<View>(R.id.btnClosePanel).setOnClickListener { closeOverlay() }
        findViewById<View>(R.id.btnCancel).setOnClickListener { closeOverlay() }
        findViewById<View>(R.id.btnSave).setOnClickListener {
            val first = etFirst.text.toString().trim()
            val last  = etLast.text.toString().trim()
            val min   = etMin.text.toString().trim()
            val max   = etMax.text.toString().trim()

            prefs.edit {
                putString(Keys.FIRST, first)
                putString(Keys.LAST,  last)
                putString(Keys.MIN,   min)
                putString(Keys.MAX,   max)
            }
            applyDisplay(first, last, min, max)
            closeOverlay()
        }

        findViewById<View>(R.id.btnEditAvatar).setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    /** Replace old toolbar menu with explicit Logout button wiring */
    private fun wireHeaderActions() {
        findViewById<View>(R.id.btn_logout)?.setOnClickListener {
            lifecycleScope.launch {
                // Clear session and go to Login, wiping back stack
                SessionStore(this@ProfileActivity).setUser(null)
                val i = Intent(this@ProfileActivity, LoginActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP
                    )
                }
                startActivity(i)
                finish()
            }
        }
    }

    private fun applyAvatar(uri: Uri) {
        findViewById<ImageView>(R.id.ivAvatar)?.setImageURI(uri)    // small avatar (overlay)
        findViewById<ImageView>(R.id.img_profile)?.setImageURI(uri) // big header (top)
    }

    private fun copyToAppStorage(source: Uri): Uri? {
        return try {
            if (currentUserId <= 0) return null
            val dir = File(filesDir, "users/u_$currentUserId/avatars").apply { if (!exists()) mkdirs() }
            val name = queryDisplayName(source).takeIf { !it.isNullOrBlank() }
                ?: "avatar_${System.currentTimeMillis()}.jpg"
            val dest = File(dir, name)

            contentResolver.openInputStream(source)?.use { input ->
                FileOutputStream(dest).use { out -> input.copyTo(out) }
            }
            Uri.fromFile(dest)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        return runCatching {
            contentResolver.query(uri, projection, null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
    }

    // ---- display helpers ----
    private fun applyDisplayFromPrefs() {
        val first = prefs.getString(Keys.FIRST, "") ?: ""
        val last  = prefs.getString(Keys.LAST,  "") ?: ""
        val min   = prefs.getString(Keys.MIN,   "") ?: ""
        val max   = prefs.getString(Keys.MAX,   "") ?: ""
        applyDisplay(first, last, min, max)
    }

    private fun applyDisplay(first: String, last: String, minRaw: String, maxRaw: String) {
        val displayName = when {
            first.isNotEmpty() && last.isNotEmpty() -> "$first $last"
            first.isNotEmpty() -> first
            last.isNotEmpty()  -> last
            else -> "User Name"
        }
        tvUserName.text = displayName

        // amounts
        tvMinAmt.text = formatAmountOrFallback(minRaw, tvMinAmt.hint?.toString() ?: "13,000")
        tvMaxAmt.text = formatAmountOrFallback(maxRaw, tvMaxAmt.hint?.toString() ?: "13,000")
    }

    private fun formatAmountOrFallback(value: String, fallback: String): String {
        if (value.isBlank()) return fallback
        return runCatching {
            val n = value.replace(",", "").toLong()
            NumberFormat.getNumberInstance(Locale.getDefault()).format(n)
        }.getOrElse { value }
    }

    private fun closeOverlay() {
        overlay.visibility = View.GONE
        hideKeyboard()
    }

    private fun hideKeyboard() {
        currentFocus?.let { v ->
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(v.windowToken, 0)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home     -> openIntent(this, "", HomeActivity::class.java)
            R.id.nav_expenses -> openIntent(this, "", ExpensesActivity::class.java)
            R.id.nav_history  -> openIntent(this, "", HistoryActivity::class.java)
            R.id.nav_profile  -> openIntent(this, "", ProfileActivity::class.java)
        }
        return true
    }
}
