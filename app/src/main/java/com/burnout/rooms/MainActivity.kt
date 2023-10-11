@file:OptIn(
  ExperimentalMaterial3Api::class,
  ExperimentalComposeUiApi::class
)

package com.burnout.rooms

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Paint.Align
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.burnout.rooms.ui.theme.RoomsTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Get Current UNIX Timestamp
//fun time(): Long {
//  return System.currentTimeMillis() / 1000
//}

fun String.isValid(limit: Int): Boolean {
  return this.isNotBlank() && this.length <= limit
}

// Main Activity
class MainActivity : ComponentActivity() {
  // TODO make me & rooms rememberSavable
  var rooms = SnapshotStateMap<String, Room>()

  private val server: RoomsAPI = RoomsAPI()
  private lateinit var user: User

  private var keyboardController: SoftwareKeyboardController? = null
  private lateinit var drawerState: DrawerState
  private lateinit var scope: CoroutineScope

  @SuppressLint("CoroutineCreationDuringComposition")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    user = User(this, getString(R.string.client_id))

    setContent {
      RoomsTheme(dynamicColor = false) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          keyboardController = LocalSoftwareKeyboardController.current

          user.StartAutoSave()

//          LaunchThreads()
          server.connect()
//          server.login("alma", "ko-rte")

          drawerState = rememberDrawerState(
            initialValue = DrawerValue.Open,
            confirmStateChange = { keyboardController?.hide(); true })
          scope = rememberCoroutineScope()

          var selectedItem by rememberSaveable { mutableStateOf(0) }
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
                      // TODO: Profile

                      var accountDialog by remember { mutableStateOf(false) }

                      // Account
                      IconButton(
                        content = {
                          val iconSize = 42.dp
                          if (user.state)
                            AsyncImage(
                              model = ImageRequest.Builder(context = LocalContext.current)
                                .data(user.url)
                                .build(),
                              contentDescription = user.url,

                              error = painterResource(R.drawable.google),
                              placeholder = painterResource(R.drawable.ic_downloading),

                              modifier = Modifier
                                .size(iconSize)
                                .clip(RoundedCornerShape(10.dp))
                            )
                          else
                            Icon(Icons.Default.AccountBox, null, Modifier.size(iconSize))
                        },
                        onClick = { accountDialog = true },
                        modifier = Modifier.padding(4.dp)
                      )

                      // "Rooms" Icon
//                      Icon(
//                        painter = painterResource(R.drawable.ic_door),
//                        contentDescription = stringResource(R.string.cd_rooms_icon),
//                        modifier = Modifier
//                          .padding(10.dp)
//                          .size(32.dp)
//                      )

                      // TODO: account dialog

                      if (accountDialog) {
                        var newNickname by rememberSaveable { mutableStateOf(user.nickname) }
                        val nicknameLimit = 16

                        fun confirm() {
                          if (newNickname.isBlank() && user.state && user.name.isNotBlank())
                            user.nickname = user.name

                          if (user.nickname != newNickname) {
                            // Update Nickname
                            // TODO: check server availability
                            if (newNickname.isValid(nicknameLimit)) {
                              user.nickname = newNickname

                              // TODO: update nickname
                              server.setNickname(user.nickname)
                            }
                          }

                          accountDialog = false
                        }

                        CustomDialog(
                          icon = Icons.Default.AccountCircle,
                          heading = "My Account",
                          onDismiss = { accountDialog = false },
                          onConfirm = { confirm() },
                          enableConfirm = newNickname.isNotBlank() || user.state
                        ) {
                          Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Set Nickname
                            OutlinedTextField(
                              value = newNickname,
                              onValueChange = { newNickname = it },

                              label = { Text("Nickname") },
                              placeholder = { Text (user.name.ifBlank { "Pick Nick" }) },

                              singleLine = true,

                              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                              keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),

                              modifier = Modifier
                                .padding(top = 16.dp)
                                .width(256.dp)
                            )

                            if (user.state) {
                              Button(
                                content = { Text("Sign Out") },
                                onClick = { user.logout() },

                                modifier = Modifier
                                  .padding(top = 8.dp)
                                  .width(256.dp)
                              )
                            } else {
                              Button(
                                content = {
                                  Image(
                                    painter = painterResource(R.drawable.google),
                                    contentDescription = null,
                                    modifier = Modifier
                                      .padding(8.dp)
                                      .size(16.dp)
                                  )
                                  Text("Sign In using Google")
                                },

                                onClick = { user.login() },

                                modifier = Modifier
                                  .padding(top = 8.dp)
                                  .width(256.dp)
                              )
                            }
                          }
                        }
                      }

                      // "Rooms" Heading
                      Text(
                        user.nickname.ifBlank { "Pick Nick" },
                        fontSize = 24.sp
                      )

                      // Join/Create Room Button
                      IconButton(
                        content = { Icon(Icons.Default.AddCircle, stringResource(R.string.cd_join_room)) },
                        onClick = {
                          selectedItem = 0
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

                        if (selectedItem == id + 1)
                          selectedRoom = roomID

                        NavigationDrawerItem(
                          icon = {
                            Icon(
                              painterResource(R.drawable.ic_chats),
                              null
                            )
                          },
                          label = { Text(room.name) },
                          selected = id + 1 == selectedItem,
                          onClick = {
                            selectedItem = id + 1
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

                  // Utility Bar (Bottom)
                  UtilityBar()
                }
              }
            },
            content = {
              if (selectedItem == 0)
                AddRoom(scope, drawerState)
              else
                RoomView(selectedRoom)
            }
          )
        }
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
  }

  @Deprecated("Deprecated in Java")
  override fun onBackPressed() {
    scope.launch { drawerState.open() }
  }

  @SuppressLint("CoroutineCreationDuringComposition")
  @Composable
  private fun LaunchThreads() {
    val connectionManager = rememberCoroutineScope()
    connectionManager.launch {
      while (true) {
        try {
          if (!server.isConnected) server.connect()
        } catch (exception: Exception) {
          exception.message?.let { Log.w("RoomsAPI", it) }
        }

        delay(10000)
      }
    }
  }

  @Composable
  private fun UtilityBar() {
    var openedDialog by rememberSaveable { mutableStateOf(0) }

    // Icon Buttons
    Row(
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically,

      modifier = Modifier
        .fillMaxSize()
        .padding(start = 40.dp, end = 40.dp)
        .wrapContentHeight(Alignment.Bottom)
    ) {
      // Settings Button
      IconButton(
        content = { Icon(Icons.Default.Settings, null) },
        onClick = { openedDialog = 100 }
      )

      // Open Homepage Button
      IconButton(
        content = { Icon(painterResource(R.drawable.ic_open_in_browser), null) },
        onClick = {
          startActivity(
            Intent(
              Intent.ACTION_VIEW,
              Uri.parse(getString(R.string.url_chat))
            )
          )
        }
      )

      if (BuildConfig.DEBUG) {
        // DevMode Button
        IconButton(
          content = { Icon(Icons.Default.Build, null) },
          onClick = { openedDialog = 1000 }
        )
      }
    }

    when (openedDialog) {
      // Nothing
      0 -> {}

      // Account
      10 -> {
        // TODO
      }

      // Settings
      100 -> {
        CustomDialog(
          icon = Icons.Default.Settings,
          heading = "Settings",
          onDismiss = { openedDialog = 0 },
          onConfirm = { openedDialog = 0 }
        ) { CircularProgressIndicator() }
      }

      // DevMode
      1000 -> {
        CustomDialog(
          icon = Icons.Default.Build,
          heading = "DevMode",
          onDismiss = { openedDialog = 0 },
          onConfirm = { openedDialog = 0 }
        ) { CircularProgressIndicator() }
      }

      else -> {
        openedDialog = 0
      }
    }
  }

  // Join/Create Room
  @Composable
  fun AddRoom(scope: CoroutineScope, drawerState: DrawerState) {
    val idLength = 8
    val nameLimit = 16

    var roomID by rememberSaveable { mutableStateOf("") }
    var roomName by rememberSaveable { mutableStateOf("") }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "Add / Join Room",
        fontSize = 24.sp,
        modifier = Modifier
          .padding(top = 24.dp)
      )
      OutlinedTextField(
        value = roomID,
        onValueChange = { roomID = it.replace(' ', '-') },

        label = { Text("Room ID") },  
        placeholder = { Text("CityHall") },  

        singleLine = true,
        supportingText = {
          if (roomID.length != idLength) Text(
            "ID Must be $idLength Characters",
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
          )
        },
        isError = roomID.length != idLength,

        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),

        modifier = Modifier
          .padding(top = 16.dp)
          .width(256.dp)
      )

      val createRoom = {
        rooms[roomID] = Room(roomID, roomName)

        scope.launch { drawerState.open() }
        keyboardController?.hide()
//                selectedItem = rooms.size-1
      }

      OutlinedTextField(
        value = roomName,
        onValueChange = { if (it.length <= nameLimit) roomName = it },

        label = { Text("Room Name") }, 
        placeholder = { Text("My Awesome Room") },  

        singleLine = true,
        supportingText = { LimitText(roomName.length, nameLimit) },
        isError = !roomName.isValid(nameLimit),

        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { createRoom() }),

        modifier = Modifier
          .padding(top = 16.dp)
          .width(256.dp)
      )

      Button(
        content = { Text("Add Room") },  
        onClick = { createRoom() },
        enabled = !rooms.containsKey(roomID) && roomID.length == idLength && roomName.isValid(
          nameLimit
        ),

        modifier = Modifier
          .fillMaxSize()
          .wrapContentHeight(Alignment.Bottom)
          .padding(16.dp)
          .width(256.dp)
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
    
    val isConnected by rememberUpdatedState(newValue = server.isConnected)

    Box(modifier = Modifier.fillMaxSize())
    {
      // "Connecting..." Card
      if (!isConnected) {
        Card (
          modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxSize()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .wrapContentHeight(Alignment.Top)
        ) {
          Row (verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
              Modifier
                .size(40.dp)
                .padding(8.dp))
            Text("Connecting...", Modifier.padding(8.dp))
          }
        }
      }
      // Chat Box
//      LazyColumn(state = lazyListState) {
//        rooms[currentRoom]?.let {
//          items(it.messages) { message ->
//            Row(
//              horizontalArrangement = if (message.author.id == me.id) Arrangement.End else Arrangement.Start,
//              modifier = Modifier
//                .fillMaxWidth()
//                .padding(bottom = 4.dp)
//            ) {
//              if (message.author.id != me.id) {
//                //Icon(Icons.Default.AccountCircle, null, Modifier.padding(start=16.dp) )
//                OutlinedCard(Modifier.padding(top = 8.dp, start = 8.dp)) {
//                  Text(
//                    message.author.name,
//                    Modifier.padding(
//                      start = 4.dp,
//                      end = 4.dp,
//                      top = 2.dp,
//                      bottom = 2.dp
//                    )
//                  )
//                }
//              }
//
//              Text(
//                text = message.data,
//                textAlign = TextAlign.Start,
//                modifier = Modifier
//                  .padding(start = 8.dp, top = 8.dp)
//              )
//            }
//          }
//        }
//
//        item(key = "Spacer") {
//          Spacer(Modifier.height(64.dp))
//        }
//      }

      // Send Message
      fun send() {
        if (text.isBlank())
          return

        text = ""

//          socket?.let { sock ->
//            // Is Room Available ?
//            rooms[currentRoom]?.let { it2 -> sock.send(Message(text, me, it2.id, time()).toString()) }
//            text = ""
//            keyboardController?.hide()
//          }
      }

      // Text Input
      TextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Send Message") },  
        placeholder = { Text(stringResource(R.string.chat_placeholder)) },

        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { if (isConnected) send() }),

        trailingIcon = {
          IconButton(
            content = { Icon(Icons.Default.Send, null) },
            onClick = { send() },
            enabled = isConnected
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

@Composable
fun LimitText(length: Int, limit: Int) {
  Text(
    text = "Limit: $length/$limit",  
    textAlign = TextAlign.End,
    modifier = Modifier.fillMaxWidth()
  )
}

//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    ToasterHUBTheme {
//        ChatView()
//    }
//}