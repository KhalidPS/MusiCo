package com.k.sekiro.musico.playmusic

interface ServiceViewModelEventBus {
    fun onSeekListener(mediaItemId: String)

    fun onSeekListener(mediaItemIndex: Int)
}