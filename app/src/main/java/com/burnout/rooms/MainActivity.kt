@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class
)

package com.burnout.rooms

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.isDigitsOnly
import com.burnout.rooms.ui.theme.RoomsTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.IOException

// Get Current UNIX Timestamp
fun time(): Long {
    return System.currentTimeMillis() / 1000
}

// Main Activity
class MainActivity : ComponentActivity() {
  var me: User = User((0..8191).random(), "User")
  var rooms = SnapshotStateMap<String, Room>()

  // Keyboard Controller
  private var keyboardController: SoftwareKeyboardController? = null

  // Networking
  private var serverURL: String = "chat.toaster.hu"
  private var wsPort: Int = 443

  private val client = OkHttpClient()
  var socket: WebSocket? = null

  // onCreate Function
  @SuppressLint("CoroutineCreationDuringComposition")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {

      RoomsTheme(dynamicColor = false) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          keyboardController = LocalSoftwareKeyboardController.current

          LaunchThreads()

          val drawerState = rememberDrawerState(DrawerValue.Open)
          val scope = rememberCoroutineScope()

          var selectedItem by rememberSaveable { mutableStateOf(-1) }
          var selectedRoom by rememberSaveable { mutableStateOf("") }

          // Main App Drawer
          ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
              ModalDrawerSheet {
                Box(Modifier.fillMaxSize()) {
                  OutlinedCard(
                    Modifier
                      .fillMaxWidth()
                      .padding(20.dp)
                  ) {
                    // Drawer Heading
                    Row(
                      Modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      // "Rooms" Icon
                      Icon(
                        painterResource(R.drawable.ic_door),
                        null,
                        Modifier
                          .padding(10.dp)
                          .size(32.dp)
                          .combinedClickable(
                            onClick = {},
                            onLongClick = { selectedItem = -2 },
                          ),
                      )

                      // "Rooms" Heading
                      Text(
                        stringResource(R.string.app_name),
                        fontSize = 24.sp
                      )

                      // Join/Create Room Button
                      IconButton(
                        content = { Icon(Icons.Default.AddCircle, null) },
                        onClick = {
                          selectedItem = -1
                          scope.launch { drawerState.close() }
                          keyboardController?.hide()
                        },
                        modifier = Modifier
                          .fillMaxWidth()
                          .wrapContentWidth(Alignment.End)
                          .padding(4.dp)
                      )
                    }
                  }

                  // Room List
                  OutlinedCard(
                    modifier = Modifier
                      .fillMaxSize()
                      .padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = 90.dp,
                        bottom = 50.dp
                      )
                  ) {
                    val entries = rooms.toList()
                    LazyColumn {
                      itemsIndexed(entries) { id, entry ->
                        val (roomID, room) = entry

                        if (selectedItem == id)
                          selectedRoom = roomID

                        NavigationDrawerItem(
                          icon = {
                            Icon(
                              painterResource(R.drawable.ic_chats),
                              contentDescription = null
                            )
                          },
                          label = { Text(room.name) },
                          selected = id == selectedItem,
                          onClick = {
                            selectedItem = id
                            scope.launch { drawerState.close() }
                            keyboardController?.hide()
                          },
                          modifier = Modifier.padding(
                            top = (if (id == 0) 12 else 0).dp,
                            start = 8.dp,
                            end = 8.dp
                          )
                        )
                      }
                    }
                  }

                  // Profile
                  Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                      .fillMaxSize()
                      .padding(start = 20.dp, end = 20.dp, bottom = 10.dp)
                      .wrapContentHeight(Alignment.Bottom)
                  ) {
                    Icon(
                      Icons.Default.AccountCircle,
                      null,
                      Modifier.padding(4.dp)
                    )

                    val openDialog = remember { mutableStateOf(false) }
                    val newName = remember { mutableStateOf(me.name) }
                    var isError by rememberSaveable { mutableStateOf(false) }

                    fun validate() {
                      isError =
                        newName.value.length > 16 || newName.value.isEmpty()
                    }

                    Text(text = "Username: ")
                    Text(
                      text = me.name,
                      style = TextStyle(textDecoration = TextDecoration.Underline),
                      modifier = Modifier.clickable {
                        openDialog.value = true
                        validate()
                      })

                    if (openDialog.value) {
                      AlertDialog(
                        onDismissRequest = { openDialog.value = false },
                        icon = {
                          Row {
                            Icon(
                              Icons.Default.AccountCircle,
                              null,
                              Modifier.padding(end = 8.dp)
                            )
                            Text("Edit Username")
                          }
                        },
                        title = null,
                        text = {
                          OutlinedTextField(
                            value = newName.value,
                            onValueChange = {
                              newName.value = it
                              validate()
                            },
                            label = { Text(if (isError) "Username*" else "Username") },
                            placeholder = { Text("Miglos Weeb") },
                            singleLine = true,
                            supportingText = {
                              Text(
                                text = "Limit: ${newName.value.length}/16",
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                              )
                            },
                            isError = isError,

                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                              onDone = {
                                validate()

                                if (!isError) {
                                  openDialog.value = false
                                  me.name = newName.value
                                }
                              }
                            ),

                            modifier = Modifier
                              .wrapContentWidth(Alignment.CenterHorizontally)
                              .wrapContentHeight(Alignment.Top)
                              .padding(top = 16.dp)
                          )
                        },
                        confirmButton = {
                          TextButton(
                            onClick = {
                              openDialog.value = false
                              me.name = newName.value
                            },
                            enabled = !isError
                          ) {
                            Text("Confirm")
                          }
                        },
                        dismissButton = {
                          TextButton(
                            onClick = {
                              openDialog.value = false
                              newName.value = me.name
                            }) {
                            Text("Dismiss")
                          }
                        }
                      )
                    }
                  }
                }
              }
            },
            content = {
              when (selectedItem) {
                -2 -> DevMode()
                -1 -> AddRoom(scope, drawerState)
                else -> if (selectedItem < 0) selectedItem = -1 else RoomView(selectedRoom)
              }
            }
          )
        }
      }
    }
  }

  @SuppressLint("CoroutineCreationDuringComposition")
  @Composable
  private fun LaunchThreads() {
    val connectionManager = rememberCoroutineScope()
    connectionManager.launch {
      while (true) {
        try {
          if (socket == null) {
            val request = Request.Builder().url("ws://$serverURL:443").build()
            socket = client.newWebSocket(request, Listener(this@MainActivity))
          }
        } finally {
          Log.w("WEBSOCKET", "Failed to create WebSocket")
        }

        delay(10000)
      }
    }
  }

  private fun getRoomData(id: String) {
    val request = Request.Builder()
      .url("http://$serverURL/room/$id")
      .build()

    client.newCall(request).enqueue(object : Callback {
      override fun onFailure(call: Call, e: IOException) {
        // Handle this
        e.cause?.message?.let { it1 -> Log.d("get", it1) }
      }

      override fun onResponse(call: Call, response: Response) {
        // here is the response from the server
        Log.d("get", response.message)
      }
    })
  }

  // DevMode Screen
  @Composable
  private fun DevMode() {
    var url by remember { mutableStateOf(serverURL) }
    var port by remember { mutableStateOf(wsPort) }

    Box(modifier = Modifier.fillMaxSize()) {
      Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = "DevMode Screen",
          modifier = Modifier.padding(8.dp)
        )

        // Change Main Server URL
        OutlinedTextField(
          value = url,
          onValueChange = { url = it },
          singleLine = true,
          label = { Text("Server URL") },
          placeholder = { Text("google.com") },

          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
          keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
        )

        // Edit WebSocket Port
        Row(horizontalArrangement = Arrangement.Center) {
          OutlinedTextField(
            value = port.toString(),
            onValueChange = {
              if (it.length <= 5 && it.isDigitsOnly()) port = it.toInt()
            },
            singleLine = true,
            label = { Text("WS Port") },
            placeholder = { Text("443") },

            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
          )
        }

        // Create GET Request
//              Button(
//                content = { Text("Get Request") },
//                onClick = { getRoomData("00000000") },
//                modifier = Modifier.padding(16.dp)
//              )
//            }

        // Apply Changes
        Button(
          content = { Text("Apply Changes") },
          onClick = {
            // Apply Changes
            if (serverURL != url || wsPort != port) {
              serverURL = url
              wsPort = port

              socket?.close(69, null)
            }

            //super.recreate()
          },
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .wrapContentWidth(Alignment.CenterHorizontally)
            .wrapContentHeight(Alignment.Bottom)
        )
      }
    }
  }

    // Join/Create Room
    @Composable
    fun AddRoom(scope: CoroutineScope, drawerState: DrawerState) {
      var roomID by rememberSaveable { mutableStateOf("") }
      var roomName by rememberSaveable { mutableStateOf("") }

      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally
      ) {

        OutlinedTextField(
          value = roomID,
          onValueChange = { roomID = it },
          label = { Text("Room ID") },
          placeholder = { Text("abcd1234") },
          singleLine = true,

          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done
          ),
          keyboardActions = KeyboardActions(
            onDone = {
            }
          ),

          modifier = Modifier
            .wrapContentWidth(Alignment.CenterHorizontally)
            .wrapContentHeight(Alignment.Top)
            .padding(top = 16.dp)
        )

        OutlinedTextField(
          value = roomName,
          onValueChange = { roomName = it },
          label = { Text("Room Name") },
          placeholder = { Text("ExampleName123") },

          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
          keyboardActions = KeyboardActions(
            onDone = {
              // TODO
            }
          ),

          modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .wrapContentHeight(Alignment.Top)
            .padding(top = 16.dp)
        )

        Button(
          content = { Text("Add Room") },
          onClick = {
            rooms[roomID] = Room(roomID, roomName)

            scope.launch { drawerState.open() }
            keyboardController?.hide()
            //selectedItem = rooms.size-1
          },
          enabled = !rooms.containsKey(roomID),

          modifier = Modifier
            .fillMaxSize()
            .wrapContentHeight(Alignment.Bottom)
            .padding(16.dp)
        )
      }
    }

  @Composable
  private fun RoomView(currentRoom: String) {
    // TODO more room types
    RoomChat(currentRoom)
  }

    // Room Chat
    @Composable
    private fun RoomChat(currentRoom: String) {
      var text by rememberSaveable { mutableStateOf("") }
      val lazyListState = rememberLazyListState()

      Box(modifier = Modifier.fillMaxSize())
      {
        // Chat Box
        LazyColumn(state = lazyListState) {
          rooms[currentRoom]?.let {
            items(it.messages) { message ->
              Row(
                horizontalArrangement = if (message.author.id == me.id) Arrangement.End else Arrangement.Start,
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(bottom = 4.dp)
              ) {

                if (message.author.id != me.id) {
                  //Icon(Icons.Default.AccountCircle, null, Modifier.padding(start=16.dp) )
                  OutlinedCard(Modifier.padding(top = 8.dp, start = 8.dp)) {
                    Text(
                      message.author.name,
                      Modifier.padding(
                        start = 4.dp,
                        end = 4.dp,
                        top = 2.dp,
                        bottom = 2.dp
                      )
                    )
                  }
                }

                Text(
                  text = message.data,
                  textAlign = TextAlign.Start,
                  modifier = Modifier
                    .padding(start = 8.dp, top = 8.dp)
                )
              }
            }
          }

          item(key = "Spacer") {
            Spacer(Modifier.height(64.dp))
          }
        }

        // Send Message
        fun send() {
          if (text.isBlank())
              return;

          socket?.let { sock ->
            // Is Room Available ?
            rooms[currentRoom]?.let { it2 -> sock.send(Message(text, me, it2.id, time()).toString()) }
            text = ""
            keyboardController?.hide()
          }
        }

        // Text Input
        TextField(
          value = text,
          onValueChange = { text = it },
          label = { Text("Send Message") },
          placeholder = { Text(stringResource(R.string.chat_placeholder)) },

          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
          keyboardActions = KeyboardActions(
            onSend = { send() }
          ),

          trailingIcon = {
            IconButton(
              content = { Icon(Icons.Default.Send, null) },
              onClick = { send() },
              enabled = socket != null
            )
          },

          modifier = Modifier
            .fillMaxSize()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .wrapContentHeight(Alignment.Bottom)
            .padding(start = 8.dp, end = 8.dp)
            .heightIn(0.dp, 120.dp)
        )
      }
    }
  }

  class Listener(mainIn: MainActivity) : WebSocketListener() {
    private val main = mainIn

    override fun onOpen(webSocket: WebSocket, response: Response) {
      Log.d("WEBSOCKET", "Connection opened")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
      Log.d("WEBSOCKET", "Received Message: $text")
      val msg = Message.fromJson(text)

      main.rooms[msg.room]?.let { it1 -> it1.messages += msg }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
      Log.d("WEBSOCKET", "Connection closed")
      main.socket = null
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
      Log.w("WEBSOCKET", "Connection failure: ${t.message}")
      main.socket = null
    }
  }

//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    ToasterHUBTheme {
//        ChatView()
//    }
//}