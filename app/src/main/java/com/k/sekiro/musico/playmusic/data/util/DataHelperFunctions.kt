package com.k.sekiro.musico.playmusic.data.util

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.Audio.AudioColumns.IS_ALARM
import android.provider.MediaStore.Audio.AudioColumns.IS_NOTIFICATION
import android.provider.MediaStore.Audio.AudioColumns.IS_RINGTONE
import android.util.Log
import com.k.sekiro.musico.R
import com.k.sekiro.musico.playmusic.domain.getUriFromDrawable
import com.k.sekiro.musico.playmusic.domain.isValidUri
import com.k.sekiro.musico.playmusic.domain.model.Song
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

suspend fun getSongsByUri(context: Context,uri: Uri) =
    coroutineScope {

            val array: ArrayList<Deferred<Song>> = ArrayList()


            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM_ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DATE_MODIFIED
            )

            val (selection, selectionArgs) = buildSongSelection()

            val sortOrder = MediaStore.Audio.Media.DATE_ADDED + " DESC"

            val cursor: Cursor? = try {
                context.contentResolver.query(
                    uri,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )
            }catch (ex: IllegalStateException){
                Log.e("ks",ex.message?:ex.stackTrace.toString())
                null
            }catch (ex: Exception){
                Log.e("ks",ex.message?:ex.stackTrace.toString())
                null
            }


            if (cursor != null) {

                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val idAlbumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumArtColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val lastModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)


                val placeholder = getUriFromDrawable(context, R.drawable.logo_musico3)
                while (cursor.moveToNext()) {


                    val id = cursor.getLong(idColumn)
                    val idAlbum = cursor.getLong(idAlbumColumn)
                    val title = cursor.getString(titleColumn)
                    val album = cursor.getString(albumColumn)
                    val albumArt = cursor.getString(albumArtColumn)
                    val artist = cursor.getString(artistColumn)
                    val path = cursor.getString(pathColumn)
                    val displayName = cursor.getString(displayNameColumn)
                    val duration = cursor.getLong(durationColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val dataUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,id)
                   // val dataUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/media"),id)
                    val lastModified = cursor.getLong(lastModifiedColumn)


                    val albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"),idAlbum)

                    /*Log.e("ks","album uri: $albumArtUri")
                    Log.e("ks","album uri path: ${albumArtUri.path}")
                    Log.e("ks","song path: $path")*/

                    val song = async {

                        val artWork = if (isValidUri(context,albumArtUri)) albumArtUri else placeholder

                        Song(
                            title = title,
                            artist = artist,
                            album = album,
                            cover = artWork.toString(),
                            path = path,
                            name = displayName,
                            addedDate = dateAdded,
                            duration = duration,
                            dataUri = dataUri.toString(),
                            lastModified = lastModified,
                            id = id
                        )
                    }



             /*       if (path.endsWith(".mp3")){
                        Log.e("ks", "song: $song")
                        Log.e("ks","album uri: $albumArtUri")
                        Log.e("ks","album uri path: ${albumArtUri.path}")
                        Log.e("ks","song path: $path")
                    }*/

                    array.add(song)
                }
                //songList.addAll(array.awaitAll())
                cursor.close()
            }

            array.awaitAll()

        }

/**
 * Shortest clip we treat as a song, in ms. Filters out UI blips and stray sound effects that
 * MediaStore still reports as music.
 */
private const val MIN_SONG_DURATION_MS = 1463L

/**
 * Paths whose audio is never a song, matched against [MediaStore.Audio.Media.DATA] with `NOT LIKE`.
 *
 * `IS_MUSIC` / `IS_RINGTONE` / `IS_ALARM` / `IS_NOTIFICATION` already exclude the standard system
 * sound folders, but they say nothing about voice recordings dropped into ordinary media folders -
 * which is why these are matched on the path instead.
 */
private val EXCLUDED_PATH_PATTERNS = listOf(
    // WhatsApp voice notes, both the legacy location and the Android 11+ scoped-storage one
    // (.../Android/media/com.whatsapp/...). Audio *files* shared over WhatsApp live in
    // "WhatsApp Audio" instead and are deliberately still included - they are usually real music.
    "%/WhatsApp Voice Notes/%",
    // MIUI's recorder, including call recordings (the user's phones are Redmi).
    "%/MIUI/sound_recorder/%",
    // The standard recordings directory (Android 11+) and Samsung's equivalent.
    "%/Recordings/%",
    // ROM-shipped ringtones/alarms/UI sounds, in case a device fails to flag them.
    "/system/%",
)

/**
 * Builds the MediaStore selection for "everything the user would call a song or a sound".
 *
 * Deliberately **not** a file-extension whitelist. The previous version listed six `DATA LIKE`
 * patterns, of which four could never match: `.m4a`, `.ogg` and `flac` were missing the leading
 * `%` (so they were compared against the whole path rather than its ending) and `%.acc` was a
 * typo for `%.aac`. Only `%.mp3` and `%.wav` worked, which is why m4a files never appeared.
 *
 * Matching on flags and paths instead means any audio format MediaStore can index shows up -
 * m4a, aac, opus, flac, ogg, wma and whatever comes next - without a list to keep in sync.
 */
private fun buildSongSelection(): Pair<String, Array<String>> {
    val conditions = mutableListOf<String>()
    val args = mutableListOf<String>()

    // IFNULL so a row with an unset flag counts as "not a ringtone" rather than dropping out:
    // in SQL, `NULL = 0` is NULL, which fails the WHERE clause and silently loses the song.
    conditions += "${MediaStore.Audio.AudioColumns.IS_MUSIC} != 0"
    conditions += "IFNULL($IS_RINGTONE, 0) = 0"
    conditions += "IFNULL($IS_ALARM, 0) = 0"
    conditions += "IFNULL($IS_NOTIFICATION, 0) = 0"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // Voice-recorder output, wherever it was saved. Only a documented column from API 30.
        conditions += "IFNULL(${MediaStore.Audio.AudioColumns.IS_RECORDING}, 0) = 0"
    }

    EXCLUDED_PATH_PATTERNS.forEach { pattern ->
        conditions += "${MediaStore.Audio.Media.DATA} NOT LIKE ?"
        args += pattern
    }

    conditions += "${MediaStore.Audio.Media.DURATION} > ?"
    args += MIN_SONG_DURATION_MS.toString()

    return conditions.joinToString(" AND ") to args.toTypedArray()
}
