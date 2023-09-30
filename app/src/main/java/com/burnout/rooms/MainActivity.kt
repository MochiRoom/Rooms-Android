@file:OptIn(
  ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class,
  ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class
)

package com.burnout.rooms

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
  private var me: User = User("userid", "username") // TODO save username & id
  var rooms = SnapshotStateMap<String, Room>()

  // Keyboard Controller
  private var keyboardController: SoftwareKeyboardController? = null

  private val server: RoomsAPI = RoomsAPI()

  lateinit var drawerState: DrawerState
  lateinit var scope: CoroutineScope

  // onCreate Function
  @SuppressLint("CoroutineCreationDuringComposition")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      RoomsTheme(dynamicColor = false) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          keyboardController = LocalSoftwareKeyboardController.current

//          LaunchThreads()
//          server.connect()
//          server.login("alma", "ko-rte")

          drawerState = rememberDrawerState(initialValue = DrawerValue.Open, confirmStateChange = { keyboardController?.hide(); true })
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
                      // "Rooms" Icon
                      Icon(
                        painterResource(R.drawable.ic_door),
                        null,
                        Modifier
                          .padding(10.dp)
                          .size(32.dp)
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

//      server.disconnect()
  }

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
          if (!server.connection.isConnected) server.connect()
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

    Row(
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically,

      modifier = Modifier
        .fillMaxSize()
        .padding(start = 40.dp, end = 40.dp)
        .wrapContentHeight(Alignment.Bottom)
    ) {
      // Account Button
      IconButton(
        content = { Icon(Icons.Default.AccountCircle, null) },
        onClick = { openedDialog = 1 },
      )

      // Settings Button
      IconButton(
        content = { Icon(Icons.Default.Settings, null) },
        onClick = { openedDialog = 2 },
      )

      // Open Homepage Button
      IconButton(
        content = { Icon(painterResource(R.drawable.ic_open_in_browser), null) },
        onClick = { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://chat.toaster.hu"))) }
      )

        if (BuildConfig.DEBUG) {
          // DevMode Button
          IconButton(
            content = { Icon(Icons.Default.Build, null, Modifier.padding(4.dp)) },
            onClick = { openedDialog = 3 },
          )
        }
    }

    when (openedDialog) {
      // Nothing
      0 -> {}

      // Account Dialog
      1 -> {
        val nameLimit = 16
        val idLimit = 16
        val passwordLimit = 16

        var newName by rememberSaveable { mutableStateOf(me.name) }
        var newUserID by rememberSaveable { mutableStateOf(me.id) }
        var newPassword by rememberSaveable { mutableStateOf(me.password) }

        val confirm = {
          openedDialog = 0
          me.name = newName
          me.id = newUserID
          me.password = newPassword
        }

        // Validate New User Info
        fun isValid(): Boolean {
          return newName.isValid(nameLimit) && newUserID.isValid(idLimit) && newPassword.isValid(passwordLimit)
        }

        CustomDialog(
          icon = Icons.Default.AccountCircle,
          heading = "My Account",
          onDismiss = { openedDialog = 0 },
          onConfirm = { confirm() },
          enableConfirm = isValid()
        ) {
          Column {
            // Set Display Name
            OutlinedTextField(
              value = newName,
              onValueChange = { newName = it },

              label = { Text("Display Name") },  // TODO string resource
              placeholder = { Text(stringResource(R.string.placeholder_username)) },

              singleLine = true,
              supportingText = { LimitText(newName.length, nameLimit) },
              isError = !newName.isValid(nameLimit),

              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),

              modifier = Modifier
                .wrapContentWidth(Alignment.CenterHorizontally)
                .wrapContentHeight(Alignment.Top)
                .padding(top = 16.dp)
                .width(256.dp)
            )

            // Set User ID
            OutlinedTextField(
              value = newUserID,
              onValueChange = { newUserID = it },

              label = { Text("User ID") },  // TODO string resource
              placeholder = { Text(stringResource(R.string.placeholder_userid)) },

              singleLine = true,
              supportingText = { LimitText(newUserID.length, idLimit) },
              isError = !newUserID.isValid(idLimit),

              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),

              modifier = Modifier
                .wrapContentWidth(Alignment.CenterHorizontally)
                .wrapContentHeight(Alignment.Top)
                .padding(top = 16.dp)
                .width(256.dp)
            )

            // Set Password
            OutlinedTextField(
              value = newPassword,
              onValueChange = { newPassword = it },

              label = { Text("Password") },  // TODO string resource
              placeholder = { Text(stringResource(R.string.placeholder_password)) },

              singleLine = true,
              supportingText = { LimitText(newPassword.length, passwordLimit) },
              isError = !newPassword.isValid(passwordLimit),

              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
              keyboardActions = KeyboardActions(onDone = { if (isValid()) confirm() }),

              modifier = Modifier
                .wrapContentWidth(Alignment.CenterHorizontally)
                .wrapContentHeight(Alignment.Top)
                .padding(top = 16.dp)
                .width(256.dp)
            )
          }
        }
      }

      // Settings Dialog
      2 -> {
        CustomDialog(
          icon = Icons.Default.Settings,
          heading = "Settings",
          onDismiss = { openedDialog = 0 },
          onConfirm = { openedDialog = 0 }
        ) { CircularProgressIndicator() }
      }

      // DevMode Dialog
      3 -> {
        CustomDialog(
          icon = Icons.Default.Build,
          heading = "DevMode",
          onDismiss = { openedDialog = 0 },
          onConfirm = { openedDialog = 0 }
        ) { CircularProgressIndicator() }
      }

      else -> { openedDialog = 0 }
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
      Text (
        text = "Add / Join Room",
        fontSize = 24.sp,
        modifier = Modifier
          .padding(top=24.dp)
      )
      OutlinedTextField(
        value = roomID,
        onValueChange = { roomID = it.replace(' ', '-') },

        label = { Text("Room ID") },  // TODO string resource
        placeholder = { Text("CityHall") },  // TODO string resource

        singleLine = true,
        supportingText = { if (roomID.length != idLength) Text("ID Must be $idLength Characters", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
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

        label = { Text("Room Name") }, // TODO string resource
        placeholder = { Text("My Awesome Room") },  // TODO string resource

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
        content = { Text("Add Room") },  // TODO string resource
        onClick = { createRoom() },
        enabled = !rooms.containsKey(roomID) && roomID.length == idLength && roomName.isValid(nameLimit),

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

        item(key = "Spacer") {  // TODO string resource
          Spacer(Modifier.height(64.dp))
        }
      }

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
        label = { Text("Send Message") },  // TODO string resource
        placeholder = { Text(stringResource(R.string.chat_placeholder)) },

        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { if (server.connection.isConnected) send() }),

        trailingIcon = {
          IconButton(
            content = { Icon(Icons.Default.Send, null) },
            onClick = { send() },
            enabled = server.connection.isConnected
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
    text = "Limit: $length/$limit",  // TODO string resource
    textAlign = TextAlign.End,
    modifier = Modifier.fillMaxWidth()
  )
}

@Composable
private fun CustomDialog(
  icon: ImageVector,
  heading: String,

  dismissText: String = "Dismiss",
  confirmText: String = "Confirm",

  enableDismiss: Boolean = true,
  enableConfirm: Boolean = true,

  onDismiss: () -> Unit,
  onConfirm: () -> Unit,

  content: @Composable (() -> Unit)
) {
  AlertDialog(
    icon = {
      Row {
        Icon(icon, null, Modifier.padding(end = 8.dp))
        Text(heading)
      }
    },
    text = content,

    confirmButton = {
      TextButton(
        content = { Text(confirmText) },
        onClick = onConfirm,
        enabled = enableConfirm
      )
    },
    onDismissRequest = onDismiss,
    dismissButton = {
      TextButton(
        content = { Text(dismissText) },
        onClick = onDismiss,
        enabled = enableDismiss
      )
    }
  )
}

//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    ToasterHUBTheme {
//        ChatView()
//    }
//}