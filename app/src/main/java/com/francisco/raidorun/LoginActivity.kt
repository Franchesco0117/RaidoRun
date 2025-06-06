package com.francisco.raidorun

import android.content.Intent
import android.credentials.CredentialManager
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import androidx.credentials.GetCredentialRequest
import androidx.transition.Visibility
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.coroutineScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        lyTermsConditions = findViewById(R.id.lyTemsConditions)
        lyTermsConditions.visibility = View.INVISIBLE

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        mAuth = FirebaseAuth.getInstance()
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tvTermsConditions = findViewById(R.id.tvTermsConditions)
        btnSignGoogle = findViewById(R.id.btnSignGoogle)

        btnLogin.setOnClickListener {
            loginUser()
        }

        tvForgotPassword.setOnClickListener {
            resetPassword()
        }

        tvTermsConditions.setOnClickListener {
            val intent = Intent(this, TermsConditionsActivity::class.java)
            startActivity(intent)
        }

        btnSignGoogle.setOnClickListener {
            signInGoogle()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val startIntent = Intent(Intent.ACTION_MAIN)
                startIntent.addCategory(Intent.CATEGORY_HOME)
                startIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(startIntent)
            }
        })

        manageLogin()
        etEmail.doOnTextChanged { text, start, before, count -> manageLogin()}
        etPassword.doOnTextChanged { text, start, before, count -> manageLogin()}
    }

    private fun signInGoogle() {
        TODO("Not yet implemented")
    }

    private fun manageLogin() {
        var email = etEmail.text.toString()
        var password = etPassword.text.toString()

        if (TextUtils.isEmpty(password) || !ValidateEmail.isEmail(email)) {
            btnLogin.setBackgroundColor(ContextCompat.getColor(this, R.color.gray_medium))
            btnLogin.isEnabled = false
        } else {
            btnLogin.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
            btnLogin.isEnabled = true
        }
    }

    private fun resetPassword() {
        var e = etEmail.text.toString() //trim()

        if (e.isEmpty()) {
            Toast.makeText(this, "Enter a valid email", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        db.collection("users").whereEqualTo("user", e).get()
            .addOnSuccessListener {documents ->
                if (!documents.isEmpty) {
                    mAuth.sendPasswordResetEmail(e).addOnCompleteListener {task ->
                        if(task.isSuccessful) {
                            Toast.makeText(this, "$e has receive an email to reset your password", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Error to send email", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                }
            } .addOnFailureListener {
                Toast.makeText(this, "Error when searching user", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loginUser() {
        email = etEmail.text.toString()
        password = etPassword.text.toString()

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                goHome(email, "email")
            } else {
                if (lyTermsConditions.visibility == View.INVISIBLE) {
                    lyTermsConditions.visibility = View.VISIBLE
                } else {
                    var cbAccept = findViewById<CheckBox>(R.id.cbAcceptTermsConditions)
                    if (cbAccept.isChecked) {
                        register("email")
                    } else {
                        Toast.makeText(this, "Tienes que aceptar los Terminos y Condiciones", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun goHome(email: String, provider: String) {
        userEmail = email
        providerSession = provider

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private fun register(provider: String) {
        email = etEmail.text.toString()
        password = etPassword.text.toString()

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) {
            if (it.isSuccessful) {
                var dateRegister = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                var dbRegister = FirebaseFirestore.getInstance()
                dbRegister.collection("users").document(email)
                    .set(hashMapOf(
                        "user" to email,
                        "dateRegister" to dateRegister
                    ))

                // For new users, go to OnBoarding first
                userEmail = email
                providerSession = provider
                startActivity(Intent(this, com.francisco.raidorun.onboarding.OnBoardingActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "No se pudo crear el usuario", Toast.LENGTH_SHORT).show()
            }
        }
    }

}