package com.k.sekiro.musico.playmusic.presenation.request_permission_screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.*

class RequestPermissionScreenViewModel:ViewModel() {

    var showDialog = MutableStateFlow(false)
        private set

    var goToSettings = MutableStateFlow(false)
        private set


    fun updateShowDialog(state:Boolean){
        showDialog.update { state }
    }

    fun updateGoToSettings(state: Boolean){
        goToSettings.update { state }
    }
}