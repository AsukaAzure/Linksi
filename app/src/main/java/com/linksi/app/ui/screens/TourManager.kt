package com.linksi.app.ui.screens

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.linksi.app.ui.components.CoachMarkTarget
import com.linksi.app.utils.dataStore
import kotlinx.coroutines.flow.map

val TOUR_COMPLETE = booleanPreferencesKey("tour_complete")

fun isTourComplete(context: Context) =
    context.dataStore.data.map { it[TOUR_COMPLETE] ?: false }

suspend fun setTourComplete(context: Context) {
    context.dataStore.edit { it[TOUR_COMPLETE] = true }
}

enum class TourStep {
    SAVE_BUTTON,        // highlight FAB - "tap to save a link"
    ADD_LINK_DIALOG,    // show add link dialog with prefilled google.com
    REMINDER_IN_DIALOG, // highlight reminder picker in dialog
    FOLDER_IN_DIALOG,   // highlight folder picker in dialog
    FOLDERS_ICON,       // highlight folders icon in top bar
    SAVED_LINK_CARD,    // highlight the first saved link card
    SWIPE_LEFT,
    SWIPE_RIGHT,
    DONE
}