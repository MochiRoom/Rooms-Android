package com.burnout.rooms

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

data class ConnectionData(
  var serverURL: String,
  var websocketPort: Int,

  var isConnected: Boolean = false,
)

class RoomsAPI {
  var connection: ConnectionData? = null

  // Networking
  private val client = OkHttpClient()
  private var socket: WebSocket? = null

  // Connect to Server
  fun connect(serverURL: String = "chat.toaster.hu", websocketPort: Int = 443) {
    connection = ConnectionData(serverURL, websocketPort)

    val request = Request.Builder().url("ws://$serverURL:${connection?.websocketPort}").build()
    socket = client.newWebSocket(request, Listener(this@RoomsAPI))
  }

  // Disconnect from the Server
  fun disconnect() {
    connection = null
  }

  private class Listener(api: RoomsAPI) : WebSocketListener() {
    private val server = api

    override fun onOpen(webSocket: WebSocket, response: Response) {
      Log.d("WEBSOCKET", "Connection opened")
      server.connection?.isConnected = true
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
      Log.d("WEBSOCKET", "Received Message: $text")
      val msg = Message.fromJson(text)

      //server.rooms[msg.room]?.let { it1 -> it1.messages += msg }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
      Log.d("WEBSOCKET", "Connection closed")
      server.socket = null
      server.connection?.isConnected = false
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
      Log.w("WEBSOCKET", "Connection failure: ${t.message}")
      server.socket = null
      server.connection?.isConnected = false
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