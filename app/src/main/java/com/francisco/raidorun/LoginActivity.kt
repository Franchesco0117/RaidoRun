package com.francisco.raidorun

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*  
import kotlin.properties.Delegates

/**
 * LoginActivity
 *
 * Activity responsible for handling user authentication through email/password
 * and Google Sign-In using Firebase. Includes validation, UI initialization,
 * Google sign-in setup, and navigation to the main screen after successful login.
 *
 * Requirements:
 * - Firebase Authentication configured for email/password and Google.
 * - Google Sign-In API client ID set in `strings.xml`.
 *
 * Author: [Francisco Castro]
 * Created: [9/MAR/2025]
 */
class LoginActivity : AppCompatActivity() {

    companion object {
        lateinit var userEmail: String
        lateinit var providerSession: String
    }

    private var email by Delegates.notNull<String>()
    private var password by Delegates.notNull<String>()
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var lyTermsConditions: LinearLayout
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvTermsConditions: TextView
    private lateinit var btnSignGoogle: Button
    private lateinit var cbAccept: CheckBox

    private lateinit var mAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initializeViews()
        setupGoogleSignIn()
        setupClickListeners()
        setupTextChangedListeners()
        setupActivityResultLauncher()
    }

    /**
     * Initializes UI components from the layout and sets up the FirebaseAuth instance.
     * Also hides the terms and conditions section initially.
     */
    private fun initializeViews() {
        lyTermsConditions = findViewById(R.id.lyTemsConditions)
        lyTermsConditions.visibility = View.INVISIBLE
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tvTermsConditions = findViewById(R.id.tvTermsConditions)
        btnSignGoogle = findViewById(R.id.btnSignGoogle)
        cbAccept = findViewById(R.id.cbAcceptTermsConditions)
        mAuth = FirebaseAuth.getInstance()
    }


    /**
     * Configures Google Sign-In options and attempts a silent sign-in if available.
     * On success, proceeds to authenticate with Firebase using the Google account's ID token.
     */
    private fun setupGoogleSignIn() {
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build()

            googleSignInClient = GoogleSignIn.getClient(this, gso)

            googleSignInClient.silentSignIn().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("GoogleSignIn", "Silent sign-in successful")
                    val account = task.result
                    account?.idToken?.let { token ->
                        firebaseAuthWithGoogle(token)
                    }
                } else {
                    Log.d("GoogleSignIn", "No silent sign-in available")
                }
            }
        } catch (e: Exception) {
            Log.e("GoogleSignIn", "Error setting up Google Sign-In", e)
            Toast.makeText(this, "Error setting Google Sign-In", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Registers an activity result launcher to handle the result of the Google Sign-In intent.
     * Handles different result scenarios such as success, cancelation, or unexpected errors.
     */
    private fun setupActivityResultLauncher() {
        signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d("GoogleSignIn", "Activity result received: ${result.resultCode}")
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    try {
                        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                        Log.d("GoogleSignIn", "Processing sign in result")
                        try {
                            val account = task.getResult(ApiException::class.java)
                            Log.d("GoogleSignIn", "Google Sign In successful")
                            account?.let {
                                Log.d("GoogleSignIn", "Email: ${it.email}, ID Token present: ${it.idToken != null}")
                                it.idToken?.let { token ->
                                    firebaseAuthWithGoogle(token)
                                } ?: run {
                                    Log.e("GoogleSignIn", "ID Token is null")
                                    Toast.makeText(this, "Error: could not get ID Token", Toast.LENGTH_SHORT).show()
                                }
                            } ?: run {
                                Log.e("GoogleSignIn", "Account object is null")
                                Toast.makeText(this, "Error: could not get account", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: ApiException) {
                            Log.e("GoogleSignIn", "Google sign in failed. Code: ${e.statusCode}", e)
                            Toast.makeText(this, "Error ${e.statusCode}: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Log.e("GoogleSignIn", "Unexpected error", e)
                        Toast.makeText(this, "Unexpected error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                Activity.RESULT_CANCELED -> {
                    Log.e("GoogleSignIn", "Sign in cancelled by user")
                    Toast.makeText(this, "Login cancel by user", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Log.e("GoogleSignIn", "Unknown result code: ${result.resultCode}")
                    Toast.makeText(this, "Unexpected result code: ${result.resultCode}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Sets up the listeners for login button, password recovery, terms and conditions,
     * and Google Sign-In button. Ensures that terms are accepted before proceeding with Google Sign-In.
     */
    private fun setupClickListeners() {
        btnLogin.setOnClickListener { loginUser() }
        tvForgotPassword.setOnClickListener { resetPassword() }
        tvTermsConditions.setOnClickListener {
            startActivity(Intent(this, TermsConditionsActivity::class.java))
        }
        btnSignGoogle.setOnClickListener {
            when {
                lyTermsConditions.visibility == View.INVISIBLE -> {
                    lyTermsConditions.visibility = View.VISIBLE
                    Toast.makeText(this, R.string.accept_terms_and_retry, Toast.LENGTH_LONG).show()
                }
                cbAccept.isChecked -> signInWithGoogle()
                else -> Toast.makeText(this, R.string.must_accept_terms, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Adds listeners to email and password input fields to manage the login button state
     * based on user input.
     */
    private fun setupTextChangedListeners() {
        etEmail.doOnTextChanged { _, _, _, _ -> manageLoginButton() }
        etPassword.doOnTextChanged { _, _, _, _ -> manageLoginButton() }
    }
    /**
     * Launches the Google Sign-In intent after signing out any previously signed-in account.
     * Ensures that the sign-in client is properly initialized before launching.
     */
    private fun signInWithGoogle() {
        if (!::googleSignInClient.isInitialized) {
            Log.e("GoogleSignIn", "Google Sign-In client not initialized")
            setupGoogleSignIn()
            return
        }

        try {
            Log.d("GoogleSignIn", "Starting Google Sign In process")
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                signInLauncher.launch(signInIntent)
            }
        } catch (e: Exception) {
            Log.e("GoogleSignIn", "Error launching sign in", e)
            Toast.makeText(this, R.string.google_sign_in_error, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Authenticates the user with Firebase using the Google ID token.
     * On successful authentication, saves the user to Firestore and redirects to the appropriate home screen.
     *
     * @param idToken The ID token obtained from the Google Sign-In process.
     */
    private fun firebaseAuthWithGoogle(idToken: String) {
        Log.d("GoogleSignIn", "Starting Firebase auth with Google")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        mAuth.signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                Log.d("GoogleSignIn", "Firebase auth successful")
                val user = authResult.user
                user?.let { firebaseUser ->
                    val email = firebaseUser.email ?: return@let
                    Log.d("GoogleSignIn", "User email: $email")
                    saveUserToFirestore(email, "Google")
                    goHome(email, "Google")
                }
            }
            .addOnFailureListener { e ->
                Log.e("GoogleSignIn", "Firebase auth failed", e)
                Toast.makeText(this, "${R.string.auth_error} ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Saves a new user's data to the Firestore "users" collection, including email,
     * registration date, and provider used for login.
     *
     * @param email The user's email address.
     * @param provider The authentication provider (e.g., "Google", "email").
     */
    private fun saveUserToFirestore(email: String, provider: String) {
        val dateRegister = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        FirebaseFirestore.getInstance().collection("users").document(email)
            .set(hashMapOf(
                "user" to email,
                "dateRegister" to dateRegister,
                "provider" to provider
            ))
    }

    /**
     * Enables or disables the login button depending on the validity of the email and password inputs.
     * Also changes the button's background color to reflect its enabled state.
     */
    private fun manageLoginButton() {
        val emailText = etEmail.text.toString()
        val passwordText = etPassword.text.toString()

        btnLogin.isEnabled = !TextUtils.isEmpty(passwordText) && ValidateEmail.isEmail(emailText)
        btnLogin.setBackgroundColor(
            ContextCompat.getColor(this,
                if (btnLogin.isEnabled) R.color.green else R.color.gray_medium
            )
        )
    }

    /**
     * Attempts to sign in the user using email and password credentials.
     * If sign-in fails and the terms are accepted, it triggers user registration.
     * Otherwise, prompts the user to accept the terms.
     */
    private fun loginUser() {
        email = etEmail.text.toString()
        password = etPassword.text.toString()

        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    goHome(email, "email")
                } else {
                    when {
                        lyTermsConditions.visibility == View.INVISIBLE -> {
                            lyTermsConditions.visibility = View.VISIBLE
                        }
                        cbAccept.isChecked -> register("email")
                        else -> {
                            Toast.makeText(this, R.string.must_accept_terms_and_conditions, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
    }

    /**
     * Sends a password reset email to the user if the email exists in the Firestore "users" collection.
     * Validates that the email field is not empty before proceeding.
     */
    private fun resetPassword() {
        val emailText = etEmail.text.toString()

        if (emailText.isEmpty()) {
            Toast.makeText(this, R.string.enter_valid_email, Toast.LENGTH_SHORT).show()
            return
        }

        FirebaseFirestore.getInstance().collection("users")
            .whereEqualTo("user", emailText)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, R.string.user_not_found, Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                mAuth.sendPasswordResetEmail(emailText)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "${R.string.password_reset_email_sent} $emailText ${R.string.password_reset_email_reset}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, R.string.error_sending_email, Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, R.string.error_finding_user, Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Registers a new user in Firebase Authentication using email and password.
     * On success, the user is saved to Firestore and redirected to the onboarding activity.
     *
     * @param provider The authentication provider (should be "email" in this context).
     */
    private fun register(provider: String) {
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    saveUserToFirestore(email, provider)
                    startActivity(Intent(this, com.francisco.raidorun.onboarding.OnBoardingActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, R.string.user_creation_failed, Toast.LENGTH_SHORT).show()
                }
            }
    }

    /**
     * Navigates the user to the appropriate home screen depending on their role.
     * If the email corresponds to the admin account, the admin dashboard is shown.
     * Otherwise, redirects to the main user activity.
     *
     * @param email The authenticated user's email.
     * @param provider The authentication provider.
     */
    private fun goHome(email: String, provider: String) {
        userEmail = email
        providerSession = provider
        
        // Check if user is admin
        if (email == "admin@raidhorun.com") {
            val intent = Intent(this, DashboardAdminActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        } else {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        finish()
    }

    /**
     * Overrides the default back button behavior to return to the device home screen
     * instead of navigating back to previous activities.
     */
    override fun onBackPressed() {
        super.onBackPressed()
        val startIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(startIntent)
    }
}