package com.k.sekiro.musico.playmusic.data.util

import com.k.sekiro.musico.playmusic.domain.model.Song

/** What a single Room-vs-MediaStore reconcile pass decided to change. */
data class LibraryReconcile(
    val toAdd: List<Song>,
    val toDelete: List<Song>,
)

/**
 * Pure diff of the Room library against a fresh MediaStore scan. Kept as a plain function (no
 * Android types, no repository) so it is unit-testable - the reconcile is the one piece of real
 * business logic in this flow and it makes *destructive* writes.
 *
 * The delete side is deliberately conservative: a Room song missing from [scannedSongs] is only
 * removed when its own storage volume actually appeared in the scan. A volume that has Room rows
 * but produced zero scan results is simply not mounted right now (SD card ejected, or still
 * mounting on cold start) - deleting those rows would cascade away their playlist / Favorite /
 * Recent memberships via `PlaylistSong`'s `ON DELETE CASCADE`, and remounting the card would not
 * bring the memberships back. See reports/reviewFix-work-architecture-and-code-review.md (C2).
 */
fun reconcileLibrary(roomSongs: List<Song>, scannedSongs: List<Song>): LibraryReconcile {
    val roomByPath = roomSongs.associateBy { it.path }
    val scannedByPath = scannedSongs.associateBy { it.path }

    val scannedVolumeRoots = scannedSongs.mapNotNull { volumeRootOf(it.path) }.toHashSet()

    val toAdd = scannedSongs.filter { it.path !in roomByPath }
    val toDelete = roomSongs.filter { room ->
        room.path !in scannedByPath && volumeRootOf(room.path).let { it != null && it in scannedVolumeRoots }
    }
    return LibraryReconcile(toAdd, toDelete)
}

/**
 * The storage-volume root of an absolute media path, or null if it is not under `/storage/`.
 * Primary shared storage is `/storage/emulated/<user>` (or `/storage/self/primary`); a removable
 * card / USB drive is `/storage/<VOLUME-ID>` (e.g. `/storage/1A2B-3C4D`).
 */
fun volumeRootOf(path: String): String? {
    if (!path.startsWith("/storage/")) return null
    val rest = path.removePrefix("/storage/")
    return when {
        rest.startsWith("emulated/") ->
            "/storage/emulated/" + rest.removePrefix("emulated/").substringBefore('/')
        rest.startsWith("self/") ->
            "/storage/self/" + rest.removePrefix("self/").substringBefore('/')
        else -> "/storage/" + rest.substringBefore('/')
    }
}
