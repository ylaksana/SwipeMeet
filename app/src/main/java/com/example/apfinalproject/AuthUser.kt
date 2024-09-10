package com.example.apfinalproject

import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.apfinalproject.model.invalidUserUid
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class AuthUser(private val registry: ActivityResultRegistry) :
    DefaultLifecycleObserver,
    FirebaseAuth.AuthStateListener {
    companion object {
        private const val TAG = "AuthUser"
    }

    private lateinit var signInLauncher: ActivityResultLauncher<Intent>
    private var activeUserUid = MutableLiveData<String>()
    private var pendingLogin = false

    init {
        Log.d(TAG, ">>init")
        // Listen to FirebaseAuth state
        // That way, if the server logs us out, we know it and change the view
        FirebaseAuth.getInstance().addAuthStateListener(this)
    }

    fun observeAuthId(): LiveData<String> {
        return activeUserUid
    }

    fun getEmail(): String {
        return Firebase.auth.currentUser?.email ?: ""
    }

    fun getName(): String {
        return Firebase.auth.currentUser?.displayName ?: ""
    }

    // Update active user LiveData upon a change of state for our FirebaseUser
    private fun activeUserUpdate(firebaseUser: FirebaseUser?) {
        Log.d(TAG, ">>activeUserUpdate")
        if (firebaseUser == null) {
            Log.d(TAG, ">>FirebaseUser is null")
            activeUserUid.postValue(invalidUserUid)
            login()
        } else {
            Log.d(TAG, ">>authUserUpdate ${firebaseUser.uid}")
            activeUserUid.postValue(firebaseUser.uid)
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        Log.d(TAG, ">>onCreate")
        signInLauncher =
            registry.register(
                "key",
                owner,
                FirebaseAuthUIActivityResultContract(),
            ) { result ->
                Log.d(TAG, "sign in result ${result.resultCode}")
                pendingLogin = false
            }
    }

    override fun onAuthStateChanged(p0: FirebaseAuth) {
        Log.d(TAG, ">>onAuthStateChanged null? ${p0.currentUser == null}")
        activeUserUpdate(p0.currentUser)
    }

    private fun user(): FirebaseUser? {
        return Firebase.auth.currentUser
    }

    private fun login() {
        Log.d(TAG, ">>login started")
        if (user() == null && !pendingLogin) {
            Log.d(TAG, ">>Logging in")
            pendingLogin = true
            val providers =
                arrayListOf(
                    AuthUI.IdpConfig.EmailBuilder().build(),
                )
            val signInIntent =
                AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(providers)
                    .setIsSmartLockEnabled(false)
                    .build()
            signInLauncher.launch(signInIntent)
        }
    }

    fun logout() {
        Log.d(TAG, "logout started")
        if (user() == null) return
        Log.d(TAG, "Logging out")
        Firebase.auth.signOut()
    }
}
