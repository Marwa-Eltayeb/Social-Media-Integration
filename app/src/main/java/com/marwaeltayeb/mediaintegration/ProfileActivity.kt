package com.marwaeltayeb.mediaintegration

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

import com.google.android.gms.auth.api.Auth
import com.google.android.gms.common.ConnectionResult
import android.widget.Toast

import com.google.firebase.auth.FirebaseAuth
import android.content.Intent
import com.bumptech.glide.Glide
import com.facebook.login.LoginManager

import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.firebase.auth.FirebaseUser
import com.marwaeltayeb.mediaintegration.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var googleApiClient: GoogleApiClient
    private lateinit var gso: GoogleSignInOptions

    private lateinit var firebaseAuth: FirebaseAuth
    private var firebaseUser: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadUserInfoFromFacebookUser()

        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        googleApiClient = GoogleApiClient.Builder(this)
            .enableAutoManage(this, this)
            .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
            .build()

       setUpListeners()
    }

    private fun setUpListeners() {
        binding.btnLogout.setOnClickListener {
            firebaseAuth.signOut()
            Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback { status ->
                if (status.isSuccess) {
                    gotoMainActivity()
                } else {
                    Toast.makeText(this, "Session not close", Toast.LENGTH_LONG).show()
                }
            }

            if (firebaseUser != null) {
                LoginManager.getInstance().logOut()
                gotoMainActivity()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val optionalPendingResult = Auth.GoogleSignInApi.silentSignIn(googleApiClient)
        // If the result is available, return true
        if (optionalPendingResult.isDone) {
            val result = optionalPendingResult.get()
            handleSignInResult(result)
        } else {
            optionalPendingResult.setResultCallback { googleSignInResult -> handleSignInResult(googleSignInResult) }
        }
    }

    private fun handleSignInResult(result: GoogleSignInResult) {
        if (result.isSuccess) {
            val account = result.signInAccount

            account?.let {
                binding.txtName.text = it.displayName
                binding.txtEmail.text = it.email
                binding.txtUserID.text = it.id

                if(it.photoUrl!= null){
                    Glide.with(this).load(account.photoUrl).into(binding.ImgProfilePhoto)
                }else{
                    Toast.makeText(this, "Image not found", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loadUserInfoFromFacebookUser() {
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseUser = firebaseAuth.currentUser

        firebaseUser?.let {
            binding.txtName.text = it.displayName
            binding.txtEmail.text = it.email
            binding.txtUserID.text = it.uid

            if(it.photoUrl != null){
                Glide.with(this).load(it.photoUrl).into(binding.ImgProfilePhoto)
            }else{
                Toast.makeText(this, "Image not found", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun gotoMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {}
}