package com.TheBudgeteers.dragonomics

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.TheBudgeteers.dragonomics.utils.Validators
import com.TheBudgeteers.dragonomics.databinding.ActivitySignupBinding
import com.TheBudgeteers.dragonomics.viewmodel.AuthState
import com.TheBudgeteers.dragonomics.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding
    private val vm: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        binding = ActivitySignupBinding.inflate(layoutInflater)


        setContentView(binding.root)


        binding.btnGoToLogIn.setOnClickListener {

            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.btnCreateAccount.setOnClickListener {
            val u = binding.etUsername.text?.toString().orEmpty()
            val e = binding.etEmail.text?.toString().orEmpty()
            val p = binding.etPassword.text?.toString().orEmpty()
            val c = binding.etConfirmPassword.text?.toString().orEmpty()

            var ok = true
            Validators.username(u)?.let { binding.etUsername.error = it; ok = false } ?: run { binding.etUsername.error = null }
            Validators.email(e)?.let    { binding.etEmail.error = it;    ok = false } ?: run { binding.etEmail.error = null }
            Validators.password(p)?.let { binding.etPassword.error = it; ok = false } ?: run { binding.etPassword.error = null }
            Validators.confirmPassword(p, c)?.let { binding.etConfirmPassword.error = it; ok = false } ?: run { binding.etConfirmPassword.error = null }

            if (!ok) return@setOnClickListener
            vm.signUp(u, e, p)
        }

        lifecycleScope.launch {
            vm.state.collect { s ->
                when (s) {
                    is AuthState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    is AuthState.Success -> {
                        binding.progressBar.visibility = View.GONE

                        startActivity(Intent(this@SignUpActivity, LoginActivity::class.java))
                        finish()
                    }
                    is AuthState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        if ((s.message ?: "").contains("Username", ignoreCase = true)) {
                            binding.etUsername.error = s.message
                        } else {

                        }
                    }
                    else -> Unit
                }
            }
        }
    }
}
