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

/*
Purpose:
    - Presents the registration UI and coordinates user sign-up.
    - Validates user input locally before delegating to AuthViewModel.
    - Reacts to authentication state.
 */

class SignUpActivity : AppCompatActivity() {

    //ViewBinding to access layout views.
    private lateinit var binding: ActivitySignupBinding
    private val vm: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Navigate to login page.
        binding.btnGoToLogIn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        //Call to action when user attempts to create an account.
        binding.btnCreateAccount.setOnClickListener {

            //Reads user editText fields, defaults them to empty strings if null.
            val u = binding.etUsername.text?.toString().orEmpty()
            val e = binding.etEmail.text?.toString().orEmpty()
            val p = binding.etPassword.text?.toString().orEmpty()
            val c = binding.etConfirmPassword.text?.toString().orEmpty()

            //Local validation
            var ok = true

            //Each validator returns an error message or null if valid.
            //If message != null, show it on the corresponding EditText and correction.
            Validators.username(u)?.let { binding.etUsername.error = it; ok = false }
                ?: run { binding.etUsername.error = null }

            Validators.email(e)?.let    { binding.etEmail.error = it;    ok = false }
                ?: run { binding.etEmail.error = null }

            Validators.password(p)?.let { binding.etPassword.error = it; ok = false }
                ?: run { binding.etPassword.error = null }

            Validators.confirmPassword(p, c)?.let { binding.etConfirmPassword.error = it; ok = false }
                ?: run { binding.etConfirmPassword.error = null }

            //If local validation ok == false, stop here and return.
            if (!ok) return@setOnClickListener

            //Store the sign up values to the view model.
            vm.signUp(u, e, p)
        }

        // begin code attribution
        // Collect AuthState from ViewModel using lifecycle-aware coroutines.
        // Adapted from:
        // Android Developers, 2020. Kotlin coroutines on Android. [online]
        // Available at: <https://developer.android.com/topic/libraries/architecture/coroutines> [Accessed 6 October 2025].

        //Observe authentication state using lifecycle coroutine
        lifecycleScope.launch {

            //Whilst validation is occurring toggle the visibility of the progress bar on/off.
            vm.state.collect { s ->
                when (s) {
                    is AuthState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    is AuthState.Success -> {
                        binding.progressBar.visibility = View.GONE

                        //Once finished authentication - take user to the login page.
                        startActivity(Intent(this@SignUpActivity, LoginActivity::class.java))
                        finish()
                    }

                    //If authentication has failed, toggle progress bar off and display error message.
                    is AuthState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        if ((s.message ?: "").contains("Username", ignoreCase = true)) {
                            binding.etUsername.error = s.message
                        } else {

                        }
                    }
                    else -> Unit
                }

            // end code attribution (Android Developers, 2020)

            }
        }
    }
}
// reference list
// Android Developers, 2020. Kotlin coroutines on Android. [online]
// Available at: <https://developer.android.com/topic/libraries/architecture/coroutines>
// [Accessed 6 October 2025].

