package com.TheBudgeteers.dragonomics

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import androidx.activity.addCallback
import androidx.activity.OnBackPressedCallback


class ProfileActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var questsAdapter: QuestsAdapter
    private lateinit var rvQuests: RecyclerView

    // overlay refs
    private lateinit var overlay: View
    private lateinit var editCard: View
    private lateinit var scrim: View
    private lateinit var btnEdit: Button
    private lateinit var btnCancel: Button
    private lateinit var btnSave: Button

    // fields in overlay
    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etMinGoal: EditText
    private lateinit var etMaxGoal: EditText

    // header views to update
    private lateinit var tvUserName: TextView
    private lateinit var tvMinAmount: TextView
    private lateinit var tvMaxAmount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        // ---------- Bottom nav ----------
        bottomNav = findViewById(R.id.bottomNavigationView)
        bottomNav.itemIconTintList = null
        bottomNav.setOnItemSelectedListener { onNavigationItemSelected(it); true }
        bottomNav.menu.findItem(R.id.nav_profile)?.apply {
            isCheckable = false
            isChecked = false
        }

        // ---------- Quests list ----------
        rvQuests = findViewById(R.id.rvQuests)
        rvQuests.layoutManager = LinearLayoutManager(this)
        questsAdapter = QuestsAdapter {  }
        rvQuests.adapter = questsAdapter

        questsAdapter.submitList(
            listOf(
                Quest("streak_1", "1 Day Streak", R.drawable.streak, null, true),
                Quest("streak_7", "7 Day Streak", R.drawable.streak, "20", false),
                Quest("nest_10k", "Put 10,000 a savings nest", R.drawable.saving_achievement, "400XP", false)
            )
        )

        // ---------- Header refs ----------
        tvUserName  = findViewById(R.id.txt_username)
        tvMinAmount = findViewById(R.id.txt_min_month_amount)
        tvMaxAmount = findViewById(R.id.txt_max_month_amount)

        // ---------- Overlay wiring ----------
        btnEdit   = findViewById(R.id.btn_edit)
        overlay   = findViewById(R.id.profileEditOverlay)
        editCard  = findViewById(R.id.editCard)
        scrim     = findViewById(R.id.scrim)
        btnCancel = findViewById(R.id.btnCancel)
        btnSave   = findViewById(R.id.btnSave)

        etFirstName = findViewById(R.id.etFirstName)
        etLastName  = findViewById(R.id.etLastName)
        etMinGoal   = findViewById(R.id.etMinGoal)
        etMaxGoal   = findViewById(R.id.etMaxGoal)

        btnEdit.setOnClickListener {
            // prefill from current labels
            val name = tvUserName.text?.toString()?.trim().orEmpty()
            val parts = name.split(" ").filter { it.isNotBlank() }
            etFirstName.setText(parts.getOrNull(0) ?: "")
            etLastName.setText(parts.drop(1).joinToString(" "))

            etMinGoal.setText(tvMinAmount.text?.toString()?.trim().orEmpty())
            etMaxGoal.setText(tvMaxAmount.text?.toString()?.trim().orEmpty())

            showEditOverlay(true)
        }

        scrim.setOnClickListener { showEditOverlay(false) }
        btnCancel.setOnClickListener { showEditOverlay(false) }

        btnSave.setOnClickListener {
            val first = etFirstName.text.toString().trim()
            val last  = etLastName.text.toString().trim()
            val minG  = etMinGoal.text.toString().trim()
            val maxG  = etMaxGoal.text.toString().trim()

            tvUserName.text = listOf(first, last).filter { it.isNotEmpty() }.joinToString(" ").ifEmpty { "User Name" }
            if (minG.isNotEmpty()) tvMinAmount.text = minG
            if (maxG.isNotEmpty()) tvMaxAmount.text = maxG

            showEditOverlay(false)
        }

        // Back button closes overlay first
        onBackPressedDispatcher.addCallback(this) {
            if (overlay.visibility == View.VISIBLE) showEditOverlay(false) else finish()
        }
    }

    private fun showEditOverlay(show: Boolean) {
        if (show) {
            overlay.alpha = 0f
            overlay.visibility = View.VISIBLE
            editCard.scaleX = 0.96f
            editCard.scaleY = 0.96f
            overlay.animate().alpha(1f).setDuration(150).start()
            editCard.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
        } else {
            overlay.animate().alpha(0f).setDuration(150).withEndAction {
                overlay.visibility = View.GONE
                overlay.alpha = 1f
            }.start()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home     -> openIntent(this, "", HomeActivity::class.java)
            R.id.nav_expenses -> openIntent(this, "", ExpensesActivity::class.java)
            R.id.nav_history  -> openIntent(this, "", HistoryActivity::class.java)
            R.id.nav_profile  -> { /* already here */ }
        }
        return true
    }
}
