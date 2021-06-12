/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.h6ah4i.example.media2

import android.util.Log
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media2.common.MediaMetadata
import androidx.media2.common.SessionPlayer
import androidx.media2.common.UriMediaItem
import androidx.media2.session.MediaSession
import androidx.media2.session.MediaSessionService
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.media2.SessionCallbackBuilder
import com.google.android.exoplayer2.ext.media2.SessionPlayerConnector
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class MyMediaSessionService : MediaSessionService() {
    lateinit var exoPlayer: SimpleExoPlayer
    lateinit var sessionPlayerConnector: SessionPlayerConnector
    lateinit var sessionCallback: MediaSession.SessionCallback
    lateinit var mediaSessionCallbackExecutor: Executor
    lateinit var mediaSession: MediaSession

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onCreate() {
        super.onCreate()

        exoPlayer = SimpleExoPlayer.Builder(this).build()
        sessionPlayerConnector = SessionPlayerConnector(exoPlayer)
        val callback = object : SessionPlayer.PlayerCallback() {
            override fun onPlayerStateChanged(player: SessionPlayer, playerState: Int) {

                if (playerState == SessionPlayer.PLAYER_STATE_ERROR) {
                    // Error handling
                    ContextCompat.getMainExecutor(this@MyMediaSessionService).execute {
                        // re-create the player instance
                        val currentPlaylist = sessionPlayerConnector.playlist
                        sessionPlayerConnector.close()
                        sessionPlayerConnector = SessionPlayerConnector(exoPlayer)
                        sessionPlayerConnector.registerPlayerCallback(ContextCompat.getMainExecutor(this@MyMediaSessionService), this)
                        if (currentPlaylist != null) {
                            sessionPlayerConnector.setPlaylist(currentPlaylist, null)
                        }
                        mediaSession.updatePlayer(sessionPlayerConnector)
                    }
                }
            }
        }
        sessionPlayerConnector.registerPlayerCallback(ContextCompat.getMainExecutor(this), callback)
        sessionCallback = SessionCallbackBuilder(this, sessionPlayerConnector)
            .setMediaItemProvider { session, controllerInfo, mediaId ->
                UriMediaItem.Builder(Uri.parse(mediaId))
                    .setMetadata(
                        MediaMetadata.Builder()
                        .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, mediaId)
                        .build()).build()
            }
            .build()

        // NOTE:
        // Using dedicated thread causes infinite blocking in onUpdateNotification() method. Using main thread can avoid the issue.
//        mediaSessionCallbackExecutor = Executors.newSingleThreadExecutor { runnable ->
//            Thread(runnable, "MediaSessionCallbackExecutorThread")
//        }
        mediaSessionCallbackExecutor = ContextCompat.getMainExecutor(this)

        mediaSession = MediaSession.Builder(this, sessionPlayerConnector)
            .setSessionCallback(mediaSessionCallbackExecutor, sessionCallback)
            .build()
    }

    override fun onUpdateNotification(session: MediaSession): MediaNotification? {
        Log.d("MyMediaSessionSvc", "[ENTER] onUpdateNotification")
        val result = super.onUpdateNotification(session)
        Log.d("MyMediaSessionSvc", "[LEAVE] onUpdateNotification")
        return result
    }
}
