package com.TheBudgeteers.dragonomics

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.TheBudgeteers.dragonomics.data.NestLayoutType
import com.TheBudgeteers.dragonomics.databinding.ActivityExpensesBinding
import com.TheBudgeteers.dragonomics.databinding.ActivityNestsBinding
import com.TheBudgeteers.dragonomics.models.NestType
import com.TheBudgeteers.dragonomics.ui.NestFragment
import com.TheBudgeteers.dragonomics.ui.NewNestDialogFragment
import com.google.android.material.navigation.NavigationView

class NestsActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityNestsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityNestsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavigationView.itemIconTintList = null
        binding.bottomNavigationView.selectedItemId = R.id.nav_expenses

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            onNavigationItemSelected(item)
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentOutgoingNests, NestFragment.newInstance(NestType.EXPENSE, NestLayoutType.LIST))
            .commit()

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentIncomingNests, NestFragment.newInstance(NestType.INCOME, NestLayoutType.LIST))
            .commit()

        binding.fabAddNest.setOnClickListener {
            val dlg = NewNestDialogFragment()
            dlg.show(supportFragmentManager, "new_nest")
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