@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)

package com.burnout.rooms

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

// Basic Message Data Class
@Serializable
data class Message (
    val author: Int = 0,
    val room: Int = 0,
    val date: Long = 0,
    val data: String = ""
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
data class Room (
    val id: Int,
    val name: String,
    val messages: SnapshotStateList<Message> = SnapshotStateList()
)

// Get Current UNIX Timestamp
fun time(): Long {
    return System.currentTimeMillis() / 1000
}

// Main Activity
class MainActivity : ComponentActivity() {
    // Networking
    private var socket: WebSocket? = null
    var isConnected = false

    private var userID = (0..8191).random()
    var rooms = SnapshotStateMap<Int, Room>()

    // onCreate Function
    @SuppressLint("CoroutineCreationDuringComposition")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RoomsTheme {
                Surface(Modifier.fillMaxSize(), color=MaterialTheme.colorScheme.background) {
                    val connectionManager = rememberCoroutineScope()
                    connectionManager.launch {
                        while (true) {
                            delay(10000)
                            if (!isConnected)
                                socket = OkHttpClient().newWebSocket(Request.Builder().url(getString(R.string.server_url)).build(),Listener(this@MainActivity))
                        }
                    }

                    val drawerState = rememberDrawerState(DrawerValue.Open)
                    val scope = rememberCoroutineScope()

                    var selectedItem by rememberSaveable { mutableStateOf(-1) }
                    var selectedRoom by rememberSaveable { mutableStateOf(0) }

                    var devMode = 0

                    // Main App Drawer
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet {
                                Box(Modifier.fillMaxSize()) {
                                    OutlinedCard(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp)) {
                                        // Drawer Heading
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                            // "Rooms" Icon
                                            Icon(
                                                painterResource(R.drawable.ic_door),
                                                null,
                                                Modifier
                                                    .padding(10.dp)
                                                    .size(32.dp)
                                                    .clickable {
                                                        if (devMode > 10) {
                                                            selectedItem = -2
                                                            devMode = 0
                                                        }
                                                        devMode++
                                                    }
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
                                    Row (
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(start = 20.dp, end = 20.dp, bottom = 10.dp)
                                            .wrapContentHeight(Alignment.Bottom)
                                            ) {
                                        Icon(Icons.Default.AccountCircle, null, Modifier.padding(4.dp))

                                        Text("User ID: $userID", Modifier.padding(4.dp))
                                    }
                                }
                            }
                        },
                        content = {
                            when (selectedItem) {
                                -2 -> DevMode()
                                -1 -> AddRoom(scope, drawerState)
                                else -> if (selectedItem < 0) selectedItem = -1 else RoomChat(selectedRoom)
                            }
                        }
                    )
                }
            }
        }
    }

    // DevMode Screen
    @Composable
    private fun DevMode() {
        Text("This is the DevMode Screen",
            Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center))
    }

    // Join/Create Room
    @Composable
    private fun AddRoom(scope: CoroutineScope, drawerState: DrawerState) {
        var roomNumber by rememberSaveable { mutableStateOf(0) }
        var roomName by rememberSaveable { mutableStateOf("") }

        Column(modifier = Modifier
            .fillMaxSize()
            .padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {

            OutlinedTextField(
                value = roomNumber.toString(),
                onValueChange = { if(it.isNotBlank()) roomNumber = it.toInt() },
                label = { Text("Room ID") },
                placeholder = { Text("1234") },
                singleLine = true,

                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
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
                label = { Text("Room Display Name") },
                placeholder = { Text("City Center") },

                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                    }
                ),

                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .wrapContentHeight(Alignment.Top)
                    .padding(top = 16.dp)
            )

            Button (
                content = { Text("Add Room") },
                onClick = {
                    rooms[roomNumber] = Room(roomNumber, roomName)

                    scope.launch { drawerState.open() }
                    //selectedItem = rooms.size-1
                },
                enabled = !rooms.containsKey(roomNumber),

                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentHeight(Alignment.Bottom)
                    .padding(16.dp)
            )
        }
    }

    // Room Chat
    @Composable
    private fun RoomChat(currentRoom: Int) {
        var text by rememberSaveable { mutableStateOf("") }

        Column(modifier = Modifier.fillMaxSize())
        {
           if (!isConnected)
            PopupMessage(stringResource(R.string.connecting))

            // Chat Box
            LazyColumn {
                rooms[currentRoom]?.let {
                    items(it.messages) { message ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                        ) {
                            //Icon(Icons.Default.AccountCircle, null, Modifier.padding(start=16.dp) )
                            OutlinedCard(Modifier.padding(top=8.dp, start=8.dp)) {
                                Text(message.author.toString(), Modifier.padding(start=4.dp,end=4.dp,top=2.dp,bottom=2.dp))
                            }

                            Text(
                                text = message.data,
                                textAlign = TextAlign.Start,
                                modifier = Modifier
                                    .padding(start=8.dp, top=8.dp)
                            )
                        }
                    }
                }
            }

            // Send Message
            fun send() {
                if (isConnected) {
                    if (text != "") {
                        // Is Socket Available ?
                            socket?.let { it1 ->
                                // Is Room Available ?
                                rooms[currentRoom]?.let { it2 ->
                                    // Send Message
                                    it1.send(Message(userID, it2.id, time(), text).toString())
                                }
                            }
                    }

                    text = ""
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
                    onSend = {send() }
                ),

                trailingIcon = {
                    IconButton (
                        content = { Icon(Icons.Default.Send, null) },
                        onClick =  { send() },
                        enabled = isConnected
                    )
                },

                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .wrapContentHeight(Alignment.Bottom)
                    .padding(start = 8.dp, end = 8.dp)
            )
        }
    }
}

class Listener(mainIn: MainActivity) : WebSocketListener() {
    private val main = mainIn
    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d("WEBSOCKET", "Connection opened")
        main.isConnected = true
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d("WEBSOCKET", "Received Message: $text")
        val msg = Message.fromJson(text)

        main.rooms[msg.room]?.let { it1 -> it1.messages += msg }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.d("WEBSOCKET", "Connection closed")
        main.isConnected = false
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.w("WEBSOCKET", "Connection failure: ${t.message}")
        main.isConnected = false
    }
}

@Composable
fun PopupMessage(text: String) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Card(elevation = CardDefaults.cardElevation(defaultElevation = 10.dp), modifier=Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                Text(text, Modifier.padding(8.dp))
            }
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