@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class,
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
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.burnout.rooms.ui.theme.RoomsTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Get Current UNIX Timestamp
fun time(): Long {
    return System.currentTimeMillis() / 1000
}

// Main Activity
class MainActivity : ComponentActivity() {
  // TODO make me & rooms rememberSavable
  private var me: User = User("(0..8191).random()", "User") // TODO save username & id
  var rooms = SnapshotStateMap<String, Room>()

  // Keyboard Controller
  private var keyboardController: SoftwareKeyboardController? = null

  private val server: RoomsAPI = RoomsAPI()

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
                      Text(stringResource(R.string.app_name), fontSize = 24.sp)

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

                        if (selectedItem == id)
                          selectedRoom = roomID

                        NavigationDrawerItem(
                          icon = { Icon(painterResource(R.drawable.ic_chats), null) },
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

                  // Utility Bar (Bottom)
                  Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                      .fillMaxSize()
                      .padding(start = 20.dp, end = 20.dp)
                      .wrapContentHeight(Alignment.Bottom)
                  ) {
                    UtilityBar()
                  }
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

    server.disconnect()
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

    // Account Button
    IconButton(
      content = { Icon(Icons.Default.AccountCircle, null) },
      onClick = { openedDialog = 1 },
      modifier = Modifier.padding(4.dp),
    )

    // Settings Button
    IconButton(
      content = { Icon(Icons.Default.Settings, null) },
      onClick = { openedDialog = 2 },
      modifier = Modifier.padding(4.dp)
    )

    // Open Homepage Button
    IconButton(
      content = { Icon(painterResource(R.drawable.ic_open_in_browser), null) },
      onClick = { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://chat.toaster.hu"))) }, // TODO string resource
      modifier = Modifier.padding(4.dp)
    )

    // DevMode Button
    IconButton(
      content = { Icon(Icons.Default.Build, null, Modifier.padding(4.dp)) },
      onClick = { openedDialog = 3 },
      modifier = Modifier.padding(4.dp)
    )

    when (openedDialog) {
      // Nothing
      0 -> { }

      // Account Dialog
      1 -> {
        var newName by rememberSaveable { mutableStateOf(me.name) }
        var newUserID by rememberSaveable { mutableStateOf(me.id) }
        var newPassword by rememberSaveable { mutableStateOf(me.password) }

        var isInvalidDisplayName by rememberSaveable { mutableStateOf(false) }
        var isInvalidUserID by rememberSaveable { mutableStateOf(false) }
        var isInvalidPassword by rememberSaveable { mutableStateOf(false) }

        fun validate(name: String, maxLength: Int = 16): Boolean {
          return name.length > maxLength || name.isEmpty()
        }

        CustomDialog(
          icon = Icons.Default.AccountCircle,
          heading = "My Account",
          onDismiss = {
            openedDialog = 0
            newName = me.name
          },
          onConfirm = {
            openedDialog = 0
            me.name = newName
            me.id = newUserID
            me.password = newPassword
          },
          enableConfirm = !isInvalidDisplayName && !isInvalidUserID && !isInvalidPassword
        ) {
          Column {
            // Set Display Name
            OutlinedTextField(
              value = newName,
              onValueChange = {
                newName = it
                isInvalidDisplayName = validate(newName)
              },
              label = { Text(if (isInvalidDisplayName) "Display Name *" else "Display Name") },  // TODO string resource
              placeholder = { Text(stringResource(R.string.placeholder_username)) },
              singleLine = true,
              supportingText = {
                Text(
                  text = "Limit: ${newName.length}/16",  // TODO string resource
                  textAlign = TextAlign.End,
                  modifier = Modifier.fillMaxWidth()
                )
              },
              isError = isInvalidDisplayName,

              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
              keyboardActions = KeyboardActions(
                onDone = {
                  isInvalidDisplayName = validate(newName)

                  if (!isInvalidDisplayName) {
                    // TODO go to next text-field
                  }
                }
              ),

              modifier = Modifier
                .wrapContentWidth(Alignment.CenterHorizontally)
                .wrapContentHeight(Alignment.Top)
                .padding(top = 16.dp)
            )

            // Set User ID
            OutlinedTextField(
              value = newUserID,
              onValueChange = {
                newUserID = it
                isInvalidUserID = validate(newUserID)
              },
              label = { Text(if (isInvalidUserID) "User ID *" else "User ID") },  // TODO string resource
              placeholder = { Text(stringResource(R.string.placeholder_userid)) },
              singleLine = true,
              supportingText = {
                Text(
                  text = "Limit: ${newUserID.length}/16",  // TODO string resource
                  textAlign = TextAlign.End,
                  modifier = Modifier.fillMaxWidth()
                )
              },
              isError = isInvalidUserID,

              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
              keyboardActions = KeyboardActions(
                onDone = {
                  isInvalidUserID = validate(newUserID)

                  if (!isInvalidUserID) {
                    // TODO go to next text-field
                  }
                }
              ),

              modifier = Modifier
                .wrapContentWidth(Alignment.CenterHorizontally)
                .wrapContentHeight(Alignment.Top)
                .padding(top = 16.dp)
            )

            // Set Password
            OutlinedTextField(
              value = newPassword,
              onValueChange = {
                newPassword = it
                isInvalidPassword = validate(newPassword)
              },
              label = { Text(if (isInvalidPassword) "Password *" else "Password") },  // TODO string resource
              placeholder = { Text(stringResource(R.string.placeholder_password)) },
              singleLine = true,
              supportingText = {
                Text(
                  text = "Limit: ${newPassword.length}/16",  // TODO string resource
                  textAlign = TextAlign.End,
                  modifier = Modifier.fillMaxWidth()
                )
              },
              isError = isInvalidPassword,

              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
              keyboardActions = KeyboardActions(
                onDone = {
                  isInvalidPassword = validate(newPassword)

                  if (!isInvalidPassword) {
                    // TODO go to next text-field
                  }
                }
              ),

              modifier = Modifier
                .wrapContentWidth(Alignment.CenterHorizontally)
                .wrapContentHeight(Alignment.Top)
                .padding(top = 16.dp)
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
        ) {
          CircularProgressIndicator()
        }
      }

      // DevMode Dialog
      3 -> {
        CustomDialog(
          icon = Icons.Default.Build,
          heading = "DevMode",
          onDismiss = { openedDialog = 0 },
          onConfirm = { openedDialog = 0 }
        ) {
          CircularProgressIndicator()
        }
      }

      else -> { openedDialog = 0 }
    }
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

  // DevMode Screen
  @Composable
  private fun DevMode() {
//    var url by remember { mutableStateOf(serverURL) }
//    var port by remember { mutableStateOf(wsPort) }
//
//    Box(modifier = Modifier.fillMaxSize()) {
//      Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
//        Text(
//          text = "DevMode Screen",
//          modifier = Modifier.padding(8.dp)
//        )
//
//        // Change Main Server URL
//        OutlinedTextField(
//          value = url,
//          onValueChange = { url = it },
//          singleLine = true,
//          label = { Text("Server URL") },
//          placeholder = { Text("google.com") },
//
//          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
//          keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
//        )
//
//        // Edit WebSocket Port
//        Row(horizontalArrangement = Arrangement.Center) {
//          OutlinedTextField(
//            value = port.toString(),
//            onValueChange = {
//              if (it.length <= 5 && it.isDigitsOnly()) port = it.toInt()
//            },
//            singleLine = true,
//            label = { Text("WS Port") },
//            placeholder = { Text("443") },
//
//            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
//            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
//          )
//        }
//
//        // Create GET Request
////              Button(
////                content = { Text("Get Request") },
////                onClick = { getRoomData("00000000") },
////                modifier = Modifier.padding(16.dp)
////              )
////            }
//
//        // Apply Changes
//        Button(
//          content = { Text("Apply Changes") },
//          onClick = {
//            // Apply Changes
//            if (serverURL != url || wsPort != port) {
//              serverURL = url
//              wsPort = port
//
//              socket?.close(69, null)
//            }
//
//            //super.recreate()
//          },
//          modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp)
//            .wrapContentWidth(Alignment.CenterHorizontally)
//            .wrapContentHeight(Alignment.Bottom)
//        )
//      }
//    }
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
          label = { Text("Room ID") },  // TODO string resource
          placeholder = { Text("my-awesome-room") },  // TODO string resource
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
          label = { Text("Room Name") }, // TODO string resource
          placeholder = { Text("My Awesome Room") },  // TODO string resource

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
          content = { Text("Add Room") },  // TODO string resource
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

          item(key = "Spacer") {  // TODO string resource
            Spacer(Modifier.height(64.dp))
          }
        }

        // Send Message
        fun send() {
          if (text.isBlank())
              return

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
          keyboardActions = KeyboardActions(
            onSend = { send() }
          ),

          trailingIcon = {
            IconButton(
              content = { Icon(Icons.Default.Send, null) },
              onClick = { send() },
              enabled = false // socket != null
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



//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    ToasterHUBTheme {
//        ChatView()
//    }
//}