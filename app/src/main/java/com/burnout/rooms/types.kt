package com.burnout.rooms

import android.app.Activity
import android.content.IntentSender
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.textInputServiceFactory
import androidx.core.app.ActivityCompat.startIntentSenderForResult
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.android.gms.common.api.ApiException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class OldUser(
  var id: String = "",
  var name: String = "",
  var password: String = "",
) {
  companion object {
    // Convert JSON String to Message
    @Suppress("UnnecessaryOptInAnnotation")
    @OptIn(ExperimentalSerializationApi::class)
    fun fromJson(json: String): User {
      return Json.decodeFromString(json)
    }
  }

  // Convert Message to JSON String
  override fun toString(): String {
    return "{\"id\":$id,\"name\":\"$name\"}"
  }
}

// Google Sign In User
class User(private val activity: MainActivity, clientID: String) {
  var nickname: String = ""
  var isSignedIn by mutableStateOf(false)

  var oneTapClient: SignInClient = Identity.getSignInClient(activity)
  var signInRequest: BeginSignInRequest = BeginSignInRequest.builder()
    .setPasswordRequestOptions(
      BeginSignInRequest.PasswordRequestOptions.builder()
        .setSupported(true)
        .build()
    )
    .setGoogleIdTokenRequestOptions(
      BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
        .setSupported(true)
        .setServerClientId(clientID)
        .setFilterByAuthorizedAccounts(false)
        .build()
    )
    .setAutoSelectEnabled(true)
    .build()

  // Sign In Credentials
  var credential: SignInCredential? = null

  val oneTapLauncher: ActivityResultLauncher<IntentSenderRequest> = activity.registerForActivityResult(
    ActivityResultContracts.StartIntentSenderForResult()) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      try {
        credential = oneTapClient.getSignInCredentialFromIntent(result.data)
        isSignedIn = true
        when (credential?.googleIdToken) {
          null -> Log.w("login", "No ID Token")
          else -> Log.d("login", "Signed In Successfully")
        }
      } catch (e: ApiException) {
        e.message?.let { Log.d("login", it) }
        credential = null
        isSignedIn = false
      }
    } else {
      Log.d("login", "login may have failed i guess")
      credential = null
      isSignedIn = false
    }
  }

  fun login() {
    oneTapClient.beginSignIn(signInRequest)
      .addOnSuccessListener(activity) { result ->
        try {
          val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
          oneTapLauncher.launch(intentSenderRequest)
        } catch (e: IntentSender.SendIntentException) {
          Log.e("login", "Couldn't start One Tap UI: ${e.localizedMessage}")
        }
      }
      .addOnFailureListener(activity) { e ->
        // No saved credentials found. Launch the One Tap sign-up flow, or
        // do nothing and continue presenting the signed-out UI.
        e.localizedMessage?.let { Log.d("login", it) }
      }
  }

  fun logout() {
    oneTapClient.signOut()
    credential = null
    isSignedIn = false
  }
}

// Basic Message Data Class
@Serializable
data class Message(
  val data: String = "",
  val author: OldUser = OldUser(),
  val room: String = "",
  val date: Long = 0,
) {
  companion object {
    // Convert JSON String to Message
    @Suppress("UnnecessaryOptInAnnotation")
    @OptIn(ExperimentalSerializationApi::class)
    fun fromJson(json: String): Message {
      return Json.decodeFromString(json)
    }
  }

  // Convert Message to JSON String
  override fun toString(): String {
    return "{\"author\":$author,\"room\":$room,\"date\":$date,\"data\":\"$data\"}"
  }
}

// Basic Room Data Class
data class Room(
  val id: String,
  var name: String,
  val messages: SnapshotStateList<Message> = SnapshotStateList()
)