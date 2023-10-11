package com.burnout.rooms

import android.content.Context
class DataStorage(activity: MainActivity) {
  private val sharedPreference =  activity.getSharedPreferences("UserData", Context.MODE_PRIVATE)
  private var editor = sharedPreference.edit()

  fun setUser(isSignedIn: Boolean = false, id: String? = null, token: String? = null, url: String? = null) {
    editor.putBoolean("isSignedIn", isSignedIn)

    editor.putString("id", id)
    editor.putString("token", token)
    editor.putString("url", url)

    editor.commit()
  }
}

