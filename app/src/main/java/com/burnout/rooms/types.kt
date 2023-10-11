package com.burnout.rooms

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.textInputServiceFactory
import androidx.core.app.ActivityCompat.startIntentSenderForResult
import androidx.core.net.toUri
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

// Google Sign In User
class User(private val activity: MainActivity, clientID: String) {
  // Data Storage
  private val sharedPreference = activity.getSharedPreferences("UserData", Context.MODE_PRIVATE)
  private val editor = sharedPreference.edit()

  var state by mutableStateOf<Boolean>(sharedPreference.getBoolean("state", false))

  var id by mutableStateOf<String>(sharedPreference.getString("id", "").toString())
  var token by mutableStateOf<String>(sharedPreference.getString("token", "").toString())
  var url by mutableStateOf<String>(sharedPreference.getString("url", "").toString())
  var name by mutableStateOf<String>(sharedPreference.getString("name", "").toString())
  var nickname by mutableStateOf<String>(sharedPreference.getString("nickname", "").toString())

  @Composable
  fun StartAutoSave() {
    LaunchedEffect(state) {
      editor.putBoolean("state", state)
      editor.commit()
    }

    LaunchedEffect(id) {
      editor.putString("id", id)
      editor.commit()
    }

    LaunchedEffect(token) {
      editor.putString("token", token)
      editor.commit()
    }

    LaunchedEffect(url) {
      editor.putString("url", url)
      editor.commit()
    }

    LaunchedEffect(name) {
      editor.putString("name", name)
      editor.commit()
    }

    LaunchedEffect(nickname) {
      editor.putString("nickname", nickname)
      editor.commit()
    }
  }

  private var oneTapClient: SignInClient = Identity.getSignInClient(activity)
  private var signInRequest: BeginSignInRequest = BeginSignInRequest.builder()
    .setGoogleIdTokenRequestOptions(
      BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
        .setSupported(true)
        .setServerClientId(clientID)
        .setFilterByAuthorizedAccounts(false)
        .build()
    )
    .setAutoSelectEnabled(true)
    .build()

  private val oneTapLauncher: ActivityResultLauncher<IntentSenderRequest> = activity.registerForActivityResult(
    ActivityResultContracts.StartIntentSenderForResult()) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      try {
        val credential = oneTapClient.getSignInCredentialFromIntent(result.data)

        state = true

        id = credential.id
        token = credential.googleIdToken?.toString() ?: ""
        url = credential.profilePictureUri?.toString() ?: ""
        name = credential.displayName?.toString() ?: ""
      } catch (e: ApiException) {
        e.message?.let { Log.d("login", "login failed: $it") }
        state = false
      }
    } else {
      Log.d("login", "login may have failed i guess")
      state = false
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
    state = false
  }
}

// Basic Message Data Class
@Serializable
data class Message(
  val data: String = "",
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
    return "{\"author\":error,\"room\":$room,\"date\":$date,\"data\":\"$data\"}"
  }
}

// Basic Room Data Class
data class Room(
  val id: String,
  var name: String,
  val messages: SnapshotStateList<Message> = SnapshotStateList()
)