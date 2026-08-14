package com.group1.dartbud.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.group1.dartbud.data.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestoreRepository = FirestoreRepository()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    // ⬇️ NYTT: Google User ID flow
    private val _googleUserId = MutableStateFlow<String?>(auth.currentUser?.uid)
    val googleUserId: StateFlow<String?> = _googleUserId.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        _currentUser.value = auth.currentUser

        // Observer currentUser og oppdater googleUserId
        viewModelScope.launch {
            currentUser.collect { user ->
                _googleUserId.value = user?.uid
            }
        }
    }

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("1047061559331-kfn5bcu39rg20dpjphgnk996bm9t335p.apps.googleusercontent.com")
            .requestEmail()
            .build()

        return GoogleSignIn.getClient(context, gso)
    }

    fun signInWithGoogle(account: GoogleSignInAccount) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading

                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                val result = auth.signInWithCredential(credential).await()

                _currentUser.value = result.user
                _authState.value = AuthState.Success

            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun signOut(context: Context) {
        auth.signOut()
        getGoogleSignInClient(context).signOut()
        _currentUser.value = null
        _authState.value = AuthState.Idle
    }

    private val _deleteAccountState = MutableStateFlow<DeleteAccountState>(DeleteAccountState.Idle)
    val deleteAccountState: StateFlow<DeleteAccountState> = _deleteAccountState.asStateFlow()

    fun deleteAccount(context: Context) {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            _deleteAccountState.value = DeleteAccountState.Loading
            try {
                firestoreRepository.deleteAllUserData(user.uid).getOrThrow()
                user.delete().await()
                getGoogleSignInClient(context).signOut()
                _currentUser.value = null
                _authState.value = AuthState.Idle
                _deleteAccountState.value = DeleteAccountState.Success
            } catch (e: Exception) {
                _deleteAccountState.value = DeleteAccountState.Error(e.message ?: "Ukjent feil")
            }
        }
    }

    fun resetDeleteAccountState() {
        _deleteAccountState.value = DeleteAccountState.Idle
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

sealed class DeleteAccountState {
    object Idle : DeleteAccountState()
    object Loading : DeleteAccountState()
    object Success : DeleteAccountState()
    data class Error(val message: String) : DeleteAccountState()
}