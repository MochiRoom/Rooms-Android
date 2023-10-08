package com.burnout.rooms

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.identity.SignInCredential
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class User(
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

// Basic Message Data Class
@Serializable
data class Message(
  val data: String = "",
  val author: User = User(),
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