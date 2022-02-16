package com.marwaeltayeb.mediaintegration

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import android.content.Intent
import android.widget.Toast
import com.facebook.*
import com.facebook.login.LoginBehavior
import com.facebook.login.LoginManager

import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.marwaeltayeb.mediaintegration.databinding.ActivityMainBinding
import timber.log.Timber

import com.facebook.login.LoginResult
import com.google.firebase.auth.*

class MainActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {

    private lateinit var binding: ActivityMainBinding
    private val TAG = "MainActivity"

    private var googleApiClient: GoogleApiClient? = null
    private val RC_SIGN_IN = 1
    var name: String? = null
    var email: String? = null
    var idToken: String? = null
    private lateinit var firebaseAuth: FirebaseAuth
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    private var callbackManager: CallbackManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        FacebookSdk.sdkInitialize(getApplicationContext())

        firebaseAuth = FirebaseAuth.getInstance()
        // Auth state Listener to listen for whether the user is signed in or not
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            // Get signedIn user
            val user = firebaseAuth.currentUser

            if (user != null) {
                // User signed in
                Timber.tag(TAG).d("onAuthStateChanged:signed_in:%s", user.uid)
            } else {
                // User signed out
                Timber.tag(TAG).d("onAuthStateChanged:signed_out")
            }
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()

        googleApiClient = GoogleApiClient.Builder(this)
            .enableAutoManage(this, this)
            .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
            .build()

        binding.btnSignIn.setOnClickListener {
            val intent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient)
            startActivityForResult(intent, RC_SIGN_IN)
        }

        // Initialize Facebook Login button
        callbackManager = CallbackManager.Factory.create()

        binding.btnFaceBookSignIn.setReadPermissions("public_profile", "email")
        binding.btnFaceBookSignIn.setLoginBehavior(LoginBehavior.WEB_ONLY);
        binding.btnFaceBookSignIn.registerCallback(callbackManager, object :
            FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                Timber.tag(TAG).d("facebook:onSuccess: $result")
                handleFacebookAccessToken(result.accessToken)
                FirebaseAuth.getInstance().signOut()  // Log out from Firebase
                if(isFacebookLogin()){ LoginManager.getInstance().logOut()}
            }

            override fun onCancel() {
                Timber.tag(TAG).d("facebook:onCancel")
            }

            override fun onError(error: FacebookException) {
                Timber.tag(TAG).d("facebook:onError $error")
            }
        })
    }

    private fun isFacebookLogin(): Boolean { return AccessToken.getCurrentAccessToken() !=null }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            handleSignInResult(result!!)
        } else {
            // Pass the activity result back to the Facebook SDK
            callbackManager?.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun handleSignInResult(result: GoogleSignInResult) {
        if (result.isSuccess) {
            val account = result.signInAccount
            idToken = account!!.idToken
            name = account.displayName
            email = account.email

            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuthWithGoogle(credential)
        } else {
            // Google Sign In failed, update UI appropriately
            Timber.tag(TAG).d("Login Unsuccessful. $result")

            Toast.makeText(this, "Login Unsuccessful", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(credential: AuthCredential) {
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                Timber.tag(TAG).d("signInWithCredential:onComplete: ${task.isSuccessful}")

                if (task.isSuccessful) {
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                    gotoProfile()
                } else {
                    Timber.tag(TAG).d("signInWithCredential ${task.exception.toString()}")

                    task.exception?.printStackTrace()
                    Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun gotoProfile() {
        val intent = Intent(this, ProfileActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    override fun onStart() {
        super.onStart()
        if (authStateListener != null) {
            FirebaseAuth.getInstance().signOut()
        }
        firebaseAuth.addAuthStateListener(authStateListener!!)


        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) { Toast.makeText(this, "Currently Logged in: " + currentUser.getEmail(), Toast.LENGTH_LONG).show()
            updateUI(currentUser)
        }
    }

    override fun onStop() {
        super.onStop()
        if (authStateListener != null) {
            firebaseAuth.removeAuthStateListener(authStateListener!!)
        }
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {}

    private fun handleFacebookAccessToken(token: AccessToken) {
        Timber.tag(TAG).d("handleFacebookAccessToken:$token")

        val credential = FacebookAuthProvider.getCredential(token.token)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                // Sign in success, update UI with the signed-in user's information
                Timber.tag(TAG).d("signInWithCredential:success")

                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                val user = firebaseAuth.currentUser
                updateUI(user)
            } else {
                // If sign in fails, display a message to the user.
                Timber.tag(TAG).d("signInWithCredential:failure ${task.exception}")

                Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                updateUI(null)
            }
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            gotoProfile()
        } else {
            Toast.makeText(this, "Please Sign in to continue", Toast.LENGTH_LONG).show()
        }
    }
}