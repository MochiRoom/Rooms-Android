package com.burnout.rooms

import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.IOException

data class ConnectionData(
  var serverURL: String = "", var websocketPort: Int = 0,

  var isConnected: Boolean = false
)

class RoomsAPI(private val url: String = "chat.toaster.hu", private val wsPort: Int = 443) {
  var isConnected: Boolean = false

  // Networking
  private val client = OkHttpClient()
  private lateinit var socket: WebSocket

  // Connect to Server
  fun connect() {
    try {
      val request = Request.Builder().url("ws://$url:${wsPort}").build()
      socket = client.newWebSocket(request, Listener(this@RoomsAPI))
    } finally {
      Log.e("RoomsAPI", "Connection Failed")
    }
  }

  // Get User Nickname
  fun getNickname(): String {
    // TODO

    return ""
  }

  // Set User Nickname
  fun setNickname(newNickname: String) {
    // TODO
  }

  // Disconnect from the Server
  fun disconnect() {
    isConnected = false
    socket.close(69, "f u")
  }

  // Validate Connection (with Sign In Data)
  fun validate(id: String, token: String) {
    val okHttpClient = OkHttpClient()
    val request = Request.Builder()
      .post("{\"id\":\"$id\", \"token\":\"$token\"}".toRequestBody())
      .url(url)
      .build()

    okHttpClient.newCall(request).enqueue(object : Callback {
      override fun onFailure(call: Call, e: IOException) {
        Log.e("RoomsAPI", "Post Failure: ${e.message}")
        // TODO
      }

      override fun onResponse(call: Call, response: Response) {
        Log.d("RoomsAPI", "Post Response: ${response.message}")
        socket.send("{\"id\":\"${response.message}\"}")
        // TODO
      }
    })
  }

  private class Listener(api: RoomsAPI) : WebSocketListener() {
    private val server = api

    override fun onOpen(webSocket: WebSocket, response: Response) {
//      println("RoomsAPI: Connection opened")
      server.isConnected = true
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
//      println("RoomsAPI: Received Message: $text")
//      val msg = Message.fromJson(text)

//      server.rooms[msg.room]?.let { it1 -> it1.messages += msg }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
      server.isConnected = false
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
      server.isConnected = false
    }
  }
}

//  private fun getRoomData(id: String) {
//    val request = Request.Builder()
//      .url("http://$serverURL/room/$id")
//      .build()
//
//    client.newCall(request).enqueue(object : Callback {
//      override fun onFailure(call: Call, e: IOException) {
//        // Handle this
//        e.cause?.message?.let { it1 -> Log.d("get", it1) }
//      }
//
//      override fun onResponse(call: Call, response: Response) {
//        // here is the response from the server
//        Log.d("get", response.message)
//      }
//    })
//  }