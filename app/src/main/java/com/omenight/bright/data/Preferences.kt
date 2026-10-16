package com.omenight.bright.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

// One delegate for the whole app, two DataStores on the same file would throw at runtime
internal val Context.brightPreferences by preferencesDataStore(name = "bright")
