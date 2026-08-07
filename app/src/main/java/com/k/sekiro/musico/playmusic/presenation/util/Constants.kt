package com.k.sekiro.musico.playmusic.presenation.util

object Constants {
    const val IMAGE_KEY = "ImageKey"
    const val TITLE_KEY = "TitleKey"
    const val ARTIST_KEY = "ArtistKey"

    // Separate namespace from IMAGE_KEY/TITLE_KEY/ARTIST_KEY: the song list card and the mini
    // player bottom bar can both be composed at the same time for the same song, and Compose's
    // SharedTransitionScope throws if more than one non-transitioning element shares a key.
    const val LIST_IMAGE_KEY = "ListImageKey"
    const val LIST_TITLE_KEY = "ListTitleKey"
    const val LIST_ARTIST_KEY = "ListArtistKey"
}