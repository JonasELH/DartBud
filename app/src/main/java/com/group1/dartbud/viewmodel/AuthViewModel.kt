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
import com.group1.dartbud.R
import com.group1.dartbud.data.DartBudDatabase
import com.group1.dartbud.data.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Håndterer Google Sign-In / Firebase Auth for appen: innlogging, utlogging
// og sletting av bruker (inkludert tilhørende data i Firestore).
// UI observerer authState/deleteAccountState for å vise loading/feil/suksess,
// og googleUserId for å vite hvilken bruker sine data som skal lastes.
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestoreRepository = FirestoreRepository()

    // Firebase sin egen "kilde til sannhet" for hvem som er innlogget
    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    // ⬇️ NYTT: Google User ID flow
    // Egen flow for kun uid-en, slik at andre ViewModels (f.eks. GameViewModel)
    // kan hente ut/observere bruker-ID uten å måtte forholde seg til hele
    // FirebaseUser-objektet.
    private val _googleUserId = MutableStateFlow<String?>(auth.currentUser?.uid)
    val googleUserId: StateFlow<String?> = _googleUserId.asStateFlow()

    // Status for selve innloggingsprosessen (Idle/Loading/Success/Error).
    // UI bruker denne til å vise spinner og feilmeldinger på login-skjermen.
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

    // Bygger Google Sign-In-klienten. web client ID-en her må matche OAuth-
    // klienten registrert i Firebase/Google Cloud-prosjektet, ellers feiler
    // innloggingen. Brukes både ved innlogging og for å logge ut av Google-
    // sesjonen (signOut/deleteAccount).
    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        // default_web_client_id genereres automatisk av google-services-plugin ut fra
        // google-services.json. Å hente den derfra i stedet for å hardkode strengen
        // gjør at klient-IDen ikke kan komme ut av synk med Firebase-prosjektet.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        return GoogleSignIn.getClient(context, gso)
    }

    // Steg 2 av Google-innlogging: UI/Activity gjør selve Google-innloggingen
    // (via GoogleSignInClient) og gir oss den ferdige GoogleSignInAccount-en.
    // Her veksler vi Google-token-et inn mot en Firebase-credential og
    // logger inn i Firebase Auth med den.
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

    // Logger ut både fra Firebase Auth og fra Google-kontoen. Begge må
    // logges ut for at brukeren skal kunne velge en annen Google-konto
    // neste gang de logger inn (ellers huskes forrige konto automatisk).
    fun signOut(context: Context) {
        auth.signOut()
        getGoogleSignInClient(context).signOut()
        _currentUser.value = null
        _authState.value = AuthState.Idle
    }

    // Status for kontosletting, separat fra authState siden dette er en egen
    // (potensielt lang) prosess med sitt eget loading/feil-UI (bekreftelses-
    // dialog for sletting av konto).
    private val _deleteAccountState = MutableStateFlow<DeleteAccountState>(DeleteAccountState.Idle)
    val deleteAccountState: StateFlow<DeleteAccountState> = _deleteAccountState.asStateFlow()

    // Sletter brukeren fullstendig: først all brukerdata i Firestore, deretter
    // selve Firebase Auth-kontoen.
    //
    // Rekkefølgen er viktig: sletter vi Auth-brukeren først og Firestore-
    // slettingen feiler etterpå, mister brukeren tilgang til kontoen sin
    // (kan ikke logge inn igjen) mens dataene fortsatt ligger igjen i
    // Firestore som foreldreløst søppel ingen kan rydde opp i. Ved å slette
    // Firestore-dataene først, og bruke getOrThrow() for å kaste videre hvis
    // det feiler, stopper vi hele operasjonen før Auth-brukeren i det hele
    // tatt slettes - da kan brukeren prøve på nytt.
    fun deleteAccount(context: Context) {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            _deleteAccountState.value = DeleteAccountState.Loading
            try {
                firestoreRepository.deleteAllUserData(user.uid).getOrThrow()

                // Slett også de lokale dataene. Uten dette lå brukerens profiler og
                // kamper fortsatt igjen i Room på enheten etter "slett konto", stikk i
                // strid med det personvernerklæringen lover. Kampene må slettes før
                // profilene, siden oppslaget går via players.googleUserId.
                val database = DartBudDatabase.getDatabase(getApplication())
                database.gameDao().deleteGamesByGoogleUserId(user.uid)
                database.playerDao().deletePlayersByGoogleUserId(user.uid)

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

    // Nullstiller deleteAccountState, f.eks. etter at UI har vist en
    // feilmelding og brukeren lukker dialogen (så den ikke dukker opp igjen
    // ved neste recomposition).
    fun resetDeleteAccountState() {
        _deleteAccountState.value = DeleteAccountState.Idle
    }
}

// Tilstander for innloggingsflyten. UI viser typisk spinner ved Loading,
// navigerer videre ved Success, og viser feilmelding (message) ved Error.
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

// Tilstander for kontosletting. Samme mønster som AuthState, men holdt atskilt
// slik at en pågående sletting ikke påvirker/overskriver innloggingsstatusen.
sealed class DeleteAccountState {
    object Idle : DeleteAccountState()
    object Loading : DeleteAccountState()
    object Success : DeleteAccountState()
    data class Error(val message: String) : DeleteAccountState()
}