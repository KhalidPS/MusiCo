package com.k.sekiro.musico.playmusic.data.util

import com.k.sekiro.musico.playmusic.domain.model.Song

/** What a single Room-vs-MediaStore reconcile pass decided to change. */
data class LibraryReconcile(
    val toAdd: List<Song>,
    val toDelete: List<Song>,
)

/**
 * Pure diff of the Room library against a fresh MediaStore scan. No Android types, no repository,
 * so it is unit-testable - the reconcile is the one piece of real business logic in this flow and
 * its delete side makes *cascading* writes (`PlaylistSong` has `ON DELETE CASCADE` on `songId`, so
 * deleting a `Song` also deletes its playlist / Favorite / Recent rows).
 *
 * The delete side is deliberately narrow:
 *
 * - **[allowDeletes] must be `false` for automatic (ContentObserver-triggered) syncs.** MediaProvider
 *   indexes a mounting volume in batches and fires the observer once per batch, so a reconcile that
 *   ran then would see a *partial* scan and delete every not-yet-indexed row. Automatic syncs
 *   therefore only ever add; deletes happen on an explicit user rescan (or right after an in-app
 *   delete, where the caller knows the songs really are gone).
 * - A row is deleted only when its storage volume is in [mountedVolumeRoots] (from
 *   `Context.getExternalFilesDirs`). An ejected / not-yet-mounted card's rows are left alone rather
 *   than wiped; a mounted-but-now-empty volume's stale rows still get purged.
 * - A Room row whose `id` still appears in the scan is a move/rename - MediaStore keeps the `_ID`
 *   and only changes `DATA` - so it is never deleted. [toAdd] carries the new-path row and the
 *   repository's `@Upsert` updates it in place, so the cascade never fires for a moved file.
 * - A completely empty scan is treated as a failed query, never as "the library is now empty" -
 *   `getSongsByUri` swallows a thrown `ContentResolver` query into an empty list.
 */
fun reconcileLibrary(
    roomSongs: List<Song>,
    scannedSongs: List<Song>,
    mountedVolumeRoots: Set<String>,
    allowDeletes: Boolean,
): LibraryReconcile {
    if (scannedSongs.isEmpty()) return LibraryReconcile(emptyList(), emptyList())

    val roomByPath = roomSongs.associateBy { it.path }
    val scannedPaths = scannedSongs.mapTo(HashSet()) { it.path }
    val scannedIds = scannedSongs.mapTo(HashSet()) { it.id }

    val toAdd = scannedSongs.filter { it.path !in roomByPath }

    val toDelete = if (!allowDeletes) {
        emptyList()
    } else {
        roomSongs.filter { room ->
            room.path !in scannedPaths &&
                room.id !in scannedIds &&
                volumeRootOf(room.path).let { it != null && it in mountedVolumeRoots }
        }
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
