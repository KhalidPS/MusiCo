package com.k.sekiro.musico.playmusic.presenation.exchange

/**
 * What the import-preview dialog renders after a QR / file has been decoded and matched against
 * the local library, before the user confirms. The resolved [com.k.sekiro.musico.playmusic
 * .domain.model.Song] list to actually import is held privately by the ViewModel.
 */
data class PlaylistImportPreview(
    val name: String,
    val matchedCount: Int,
    val totalCount: Int,
    val unmatchedTitles: List<String>,
)
