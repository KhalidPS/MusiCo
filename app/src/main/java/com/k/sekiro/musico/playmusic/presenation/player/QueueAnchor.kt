package com.k.sekiro.musico.playmusic.presenation.player

/**
 * Where to re-anchor the player's queue after the song list changed underneath it.
 *
 * @param index a position that is guaranteed to exist in the new list.
 * @param keepPosition whether the anchored song is the same one that was already loaded, and so
 * should resume from where it was instead of starting over.
 */
internal data class QueueAnchor(val index: Int, val keepPosition: Boolean)

/**
 * Picks the queue position to rebuild the player around.
 *
 * The list shrinks as well as grows: the user can delete the song that is playing, or a whole
 * selection that includes it. `indexOf(playingSong)` then answers **-1**, and feeding that (or a
 * now-past-the-end index) to `setMediaItems`/`seekTo` is what crashes the player with an
 * `IllegalSeekPositionException`, or lands the pager on a page that no longer exists.
 *
 * Resolution order:
 * 1. the song the player actually has loaded, if it survived - resume it where it was;
 * 2. otherwise the song the UI thinks is playing, if *that* survived;
 * 3. otherwise the nearest surviving position to where the deleted song sat, from the top - which
 *    is the song that took its place, i.e. the same thing as skipping to the next one.
 *
 * @return null when nothing is left to play.
 */
internal fun resolveQueueAnchor(
    paths: List<String>,
    playingPath: String?,
    uiPath: String?,
    previousIndex: Int,
): QueueAnchor? {
    if (paths.isEmpty()) return null

    for (survivor in listOfNotNull(playingPath, uiPath)) {
        val index = paths.indexOf(survivor)
        if (index != -1) return QueueAnchor(index, keepPosition = true)
    }

    return QueueAnchor(previousIndex.coerceIn(0, paths.lastIndex), keepPosition = false)
}
