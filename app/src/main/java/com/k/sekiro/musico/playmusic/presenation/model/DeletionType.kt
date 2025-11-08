package com.k.sekiro.musico.playmusic.presenation.model

 sealed interface DeletionType{
     data class StorageDeletion(val songUi: SongUi? = null): DeletionType
     data class PlaylistDeletion(val playlistId: Long, val songUi: SongUi? = null): DeletionType
}
