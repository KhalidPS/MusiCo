package com.k.sekiro.musico.playmusic.data.util

import com.k.sekiro.musico.playmusic.domain.model.Song

/** What a single Room-vs-MediaStore reconcile pass decided to change. */
data class LibraryReconcile(
    val toAdd: List<Song>,
    val toDelete: List<Song>,
)

/**
 * Pure diff of the Room library against a fresh MediaStore scan. No Android types, no repository,
 * so it is unit-testable.
 *
 * The library mirrors device storage: a song MediaStore no longer reports is removed from Room,
 * on every sync. Only two things are ever exempt, and neither is a policy choice - both are cases
 * where the *scan itself* is not trustworthy evidence that a file is gone:
 *
 * - **An empty scan is a failed query, not an empty device.** `getSongsByUri` swallows a thrown
 *   `ContentResolver.query` into an empty list, so "no rows at all" means the query blew up far
 *   more often than it means the user deleted every song they own.
 * - **A row on an unmounted volume is unreadable, not deleted.** [mountedVolumeRoots] comes from
 *   `Context.getExternalFilesDirs`, which omits an ejected SD card entirely. Its songs are still on
 *   the card, so they keep their rows (and their playlist membership) until it is back.
 *
 * A Room row whose `id` still appears in the scan is a move/rename - MediaStore keeps the `_ID` and
 * only changes `DATA` - so it is never deleted. [toAdd] carries the new-path row and the
 * repository's `@Upsert` updates it in place.
 *
 * That `id` check matters because deletes here cascade: `PlaylistSong` references `Song(id)` with
 * `ON DELETE CASCADE`, so removing a `Song` also removes its playlist / Favorite / Recent rows. A
 * re-scan brings the song back, but never the memberships - which is why a moved file must not go
 * out through [toDelete] and back in through [toAdd].
 */
fun reconcileLibrary(
    roomSongs: List<Song>,
    scannedSongs: List<Song>,
    mountedVolumeRoots: Set<String>,
): LibraryReconcile {
    if (scannedSongs.isEmpty()) return LibraryReconcile(emptyList(), emptyList())

    val roomPaths = roomSongs.mapTo(HashSet()) { it.path }
    val scannedPaths = scannedSongs.mapTo(HashSet()) { it.path }
    val scannedIds = scannedSongs.mapTo(HashSet()) { it.id }

    val toAdd = scannedSongs.filter { it.path !in roomPaths }

    val toDelete = roomSongs.filter { room ->
        val volume = volumeRootOf(room.path)
        room.path !in scannedPaths &&
            room.id !in scannedIds &&
            volume != null &&
            volume in mountedVolumeRoots
    }

    return LibraryReconcile(toAdd, toDelete)
}

/**
 * The storage-volume root of an absolute media path - `/storage/emulated/<user>` for built-in
 * storage, `/storage/<VOLUME-ID>` (e.g. `/storage/1A2B-3C4D`) for a removable card or USB drive.
 * `null` if the path is not under `/storage/` (app-private / legacy locations, which are never
 * reconcile-deleted).
 */
fun volumeRootOf(path: String): String? {
    if (!path.startsWith("/storage/")) return null
    val rest = path.removePrefix("/storage/")
    return if (rest.startsWith("emulated/")) {
        "/storage/emulated/" + rest.removePrefix("emulated/").substringBefore('/')
    } else {
        "/storage/" + rest.substringBefore('/')
    }
}
