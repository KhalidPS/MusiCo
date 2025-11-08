package com.k.sekiro.musico.playmusic.data.util

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
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

            val selection = MediaStore.Audio.AudioColumns.IS_MUSIC + " = ? AND $IS_RINGTONE = ? AND $IS_ALARM = ? AND $IS_NOTIFICATION = ?" +
                    " AND (${MediaStore.Audio.Media.DATA} LIKE ? " +
                    "OR ${MediaStore.Audio.Media.DATA} LIKE ? OR ${MediaStore.Audio.Media.DATA} LIKE ? " +
                    "OR ${MediaStore.Audio.Media.DATA} LIKE ? OR ${MediaStore.Audio.Media.DATA} LIKE ? OR " +
                    "${MediaStore.Audio.Media.DATA} LIKE ?) AND ${MediaStore.Audio.AudioColumns.DURATION} > 1463"
            val selectionArgs = arrayOf("1","0","0","0","%.mp3","%.acc","%.wav",".m4a",".ogg","flac")
            //val selection = MediaStore.Audio.Media.MIME_TYPE + " LIKE 'audio%'"
            //val selection = MediaStore.Audio.Media.DATA + " LIKE '%.mp3'"
           // val selection = "((${MediaStore.Audio.Media.DATA} LIKE '%.mp3') AND (${MediaStore.Audio.Media.MIME_TYPE} LIKE 'audio%'))"

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


                val placeholder = getUriFromDrawable(context, R.drawable.logo_2)
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

                    Log.e("ks","song: $song")


                    array.add(song)
                }
                //songList.addAll(array.awaitAll())
                cursor.close()
            }

            array.awaitAll()

        }