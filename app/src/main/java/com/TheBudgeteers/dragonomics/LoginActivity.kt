package com.TheBudgeteers.dragonomics

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.TheBudgeteers.dragonomics.data.SessionStore
import com.TheBudgeteers.dragonomics.utils.Validators
import com.TheBudgeteers.dragonomics.databinding.ActivityLoginBinding
import com.TheBudgeteers.dragonomics.viewmodel.AuthState
import com.TheBudgeteers.dragonomics.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val vm: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val updateStateFromInputs = {
            binding.tvError?.text = ""

            binding.btnLogin.isEnabled = looksValid()
        }

        binding.etUsername.doAfterTextChanged {
            if (binding.etUsername.error != null) {
                binding.etUsername.error = Validators.username(binding.etUsername.text?.toString().orEmpty())
            }
            updateStateFromInputs()
        }
        binding.etPassword.doAfterTextChanged {
            if (binding.etPassword.error != null) {
                binding.etPassword.error = Validators.password(binding.etPassword.text?.toString().orEmpty())
            }
            updateStateFromInputs()
        }

        binding.btnLogin.isEnabled = looksValid()

        binding.btnGoToSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        binding.btnLogin.setOnClickListener {
            if (!validateHard()) return@setOnClickListener

            val u = binding.etUsername.text?.toString()?.trim().orEmpty()
            val p = binding.etPassword.text?.toString()?.trim().orEmpty()
            vm.logIn(u, p)
        }

        lifecycleScope.launch {
            vm.state.collect { s ->
                when (s) {
                    is AuthState.Loading -> {
                        setLoading(true)
                        binding.tvError?.text = ""
                    }
                    is AuthState.Success -> {
                        setLoading(false)

                        // Persist session
                        val userId = s.userId
                        SessionStore(this@LoginActivity).setUser(userId)

                        // Go to Home
                        val i = Intent(this@LoginActivity, HomeActivity::class.java).apply {
                            addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                            )
                        }
                        startActivity(i)
                        finish()
                    }
                    is AuthState.Error -> {
                        setLoading(false)

                        // Map server error to fields when possible
                        val msg = (s.message ?: "").trim()
                        when {
                            msg.contains("username", ignoreCase = true) ->
                                binding.etUsername.error = msg.ifEmpty { "Invalid username" }
                            msg.contains("password", ignoreCase = true) ->
                                binding.etPassword.error = msg.ifEmpty { "Invalid password" }
                            else ->
                                binding.tvError?.text = msg.ifEmpty { "Login failed. Please try again." }
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun looksValid(): Boolean {
        val u = binding.etUsername.text?.toString()?.trim().orEmpty()
        val p = binding.etPassword.text?.toString()?.trim().orEmpty()
        return Validators.username(u) == null && Validators.password(p) == null
    }

    private fun validateHard(): Boolean {
        val u = binding.etUsername.text?.toString()?.trim().orEmpty()
        val p = binding.etPassword.text?.toString()?.trim().orEmpty()

        var ok = true
        Validators.username(u)?.let { binding.etUsername.error = it; ok = false } ?: run { binding.etUsername.error = null }
        Validators.password(p)?.let { binding.etPassword.error = it; ok = false } ?: run { binding.etPassword.error = null }
        return ok
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.isVisible = loading
        binding.btnLogin.isEnabled = !loading && looksValid()
        binding.btnGoToSignUp.isEnabled = !loading
        binding.etUsername.isEnabled = !loading
        binding.etPassword.isEnabled = !loading
    }
}
