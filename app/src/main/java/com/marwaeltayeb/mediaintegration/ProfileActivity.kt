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

import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.marwaeltayeb.mediaintegration.databinding.ActivityProfileBinding
import java.lang.NullPointerException

class ProfileActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {

    private lateinit var binding: ActivityProfileBinding
    private var googleApiClient: GoogleApiClient? = null
    private lateinit var gso: GoogleSignInOptions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        googleApiClient = GoogleApiClient.Builder(this)
            .enableAutoManage(this, this)
            .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
            .build()

        binding.btnLogout.setOnClickListener{
            FirebaseAuth.getInstance().signOut()
            Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback { status ->
                if (status.isSuccess()) {
                    gotoMainActivity()
                } else {
                    Toast.makeText(this, "Session not close", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val opr = Auth.GoogleSignInApi.silentSignIn(googleApiClient)
        if (opr.isDone) {
            val result = opr.get()
            handleSignInResult(result)
        } else {
            opr.setResultCallback { googleSignInResult -> handleSignInResult(googleSignInResult) }
        }
    }

    private fun handleSignInResult(result: GoogleSignInResult) {
        if (result.isSuccess) {
            val account = result.signInAccount
            binding.txtName.text = account!!.displayName
            binding.txtEmail.text = account.email
            binding.txtUserID.text = account.id
            try {
                Glide.with(this).load(account.photoUrl).into(binding.ImgProfilePhoto)
            } catch (e: NullPointerException) {
                Toast.makeText(this, "Image not found", Toast.LENGTH_LONG).show()
            }
        } else {
            gotoMainActivity()
        }
    }

    private fun gotoMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {}
}