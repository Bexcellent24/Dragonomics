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

/*
Purpose:
  - Presents the login UI and coordinates authentication.
  - Validates inputs locally and delegates to [AuthViewModel].
  - Observes [AuthState] to show loading, surface errors, and navigate.
 */

class LoginActivity : AppCompatActivity() {

    //ViewBinding to access layout views.
    private lateinit var binding: ActivityLoginBinding
    private val vm: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Local helper to update button state
        val updateStateFromInputs = {

            //Clear top-level error on any edit
            binding.tvError?.text = ""
            binding.btnLogin.isEnabled = looksValid()
        }

        // begin code attribution
        // Live field validation using AndroidX Core KTX text-change callbacks.
        // Adapted from:
        // Android Developers, 2020. androidx.core.widget.doAfterTextChanged. [online]
        // Available at: <https://developer.android.com/reference/kotlin/androidx/core/widget/package-summary#doaftertextchanged>
        // [Accessed 6 October 2025].

        //Live UX validation
        binding.etUsername.doAfterTextChanged {
            //If field previously showed an error, re-validate on edit
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

        // end code attribution (Android Developers, 2020)

        //Initial button state
        binding.btnLogin.isEnabled = looksValid()

        //Navigate to sign up page.
        binding.btnGoToSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        //Call to action when user attempts to login.
        binding.btnLogin.setOnClickListener {

            // Perform validation, sets field errors if needed
            // Similar logic to the SignUpActivity btnCreateAccount
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

                        //Persist session for user info
                        val userId = s.userId
                        SessionStore(this@LoginActivity).setUser(userId)

                        // begin code attribution
                        // Navigate to Home and clear the back stack using Intent flags NEW_TASK, CLEAR_TASK, and CLEAR_TOP.
                        // Adapted from:
                        // Android Developers, 2020. Tasks and the back stack. [online]
                        // Available at: <https://developer.android.com/guide/components/activities/tasks-and-back-stack>
                        // [Accessed 6 October 2025].
                        //Go to HomeActivity on successful login and clears the back stack
                        val i = Intent(this@LoginActivity, HomeActivity::class.java).apply {
                            addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                            )
                        }
                        startActivity(i)
                        finish()
                        // end code attribution (Android Developers, 2020)
                    }

                    //On failed validation
                    is AuthState.Error -> {
                        setLoading(false)

                        //Error message handling depending on the error case
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

    //Returns true when both fields are not empty.
    private fun looksValid(): Boolean {
        val u = binding.etUsername.text?.toString()?.trim().orEmpty()
        val p = binding.etPassword.text?.toString()?.trim().orEmpty()
        return Validators.username(u) == null && Validators.password(p) == null
    }

    //Actual validation of EditText fields.
    private fun validateHard(): Boolean {
        val u = binding.etUsername.text?.toString()?.trim().orEmpty()
        val p = binding.etPassword.text?.toString()?.trim().orEmpty()

        var ok = true
        Validators.username(u)?.let { binding.etUsername.error = it; ok = false } ?: run { binding.etUsername.error = null }
        Validators.password(p)?.let { binding.etPassword.error = it; ok = false } ?: run { binding.etPassword.error = null }
        return ok
    }

    //Toggles loading UI and prevents duplicate user submissions.
    private fun setLoading(loading: Boolean) {
        binding.progressBar.isVisible = loading
        binding.btnLogin.isEnabled = !loading && looksValid()
        binding.btnGoToSignUp.isEnabled = !loading
        binding.etUsername.isEnabled = !loading
        binding.etPassword.isEnabled = !loading
    }
}
// reference list
// Android Developers, 2020. androidx.core.widget.doAfterTextChanged. [online]
// Available at: <https://developer.android.com/reference/kotlin/androidx/core/widget/package-summary#doaftertextchanged> [Accessed 6 October 2025].
// Android Developers, 2020. Tasks and the back stack. [online]
// Available at: <https://developer.android.com/guide/components/activities/tasks-and-back-stack> [Accessed 6 October 2025].
