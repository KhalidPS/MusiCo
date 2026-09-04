package com.k.sekiro.musico.playmusic.presenation.exchange

/**
 * What the import-preview dialog renders after a QR / file has been decoded and matched against
 * the local library, before the user confirms. The resolved [com.k.sekiro.musico.playmusic
 * .domain.model.Song] list to actually import is held privately by the ViewModel.
 *
 * [transferOffer] is non-null only for a `MUSICO-XFER-1:` (audio-transfer) scan - `null` means
 * the plain QR-only path (this device just drops the unmatched tracks, as before).
 */
data class PlaylistImportPreview(
    val name: String,
    val matchedCount: Int,
    val totalCount: Int,
    val unmatchedTitles: List<String>,
    val transferOffer: TransferOffer? = null,
)

/** How many of [PlaylistImportPreview.unmatchedTitles] can actually be fetched, and their size. */
data class TransferOffer(
    val newTrackCount: Int,
    val totalBytes: Long,
)
