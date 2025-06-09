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

    private fun setupGoogleSignIn() {
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build()

            googleSignInClient = GoogleSignIn.getClient(this, gso)

            // Verificar la configuración actual
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

    private fun setupTextChangedListeners() {
        etEmail.doOnTextChanged { _, _, _, _ -> manageLoginButton() }
        etPassword.doOnTextChanged { _, _, _, _ -> manageLoginButton() }
    }

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

    private fun saveUserToFirestore(email: String, provider: String) {
        Log.d("GoogleSignIn", "Saving user to Firestore")
        val dateRegister = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        FirebaseFirestore.getInstance().collection("users").document(email)
            .set(hashMapOf(
                "user" to email,
                "dateRegister" to dateRegister,
                "provider" to provider
            ))
            .addOnSuccessListener {
                Log.d("GoogleSignIn", "User saved to Firestore successfully")
            }
            .addOnFailureListener { e ->
                Log.e("GoogleSignIn", "Error saving user to Firestore", e)
            }
    }

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

    override fun onBackPressed() {
        super.onBackPressed()
        val startIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(startIntent)
    }
}