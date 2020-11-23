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

import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media2.common.MediaMetadata
import androidx.media2.common.UriMediaItem
import androidx.media2.session.MediaSession
import androidx.media2.session.MediaSessionService
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.media2.DefaultMediaItemConverter
import com.google.android.exoplayer2.ext.media2.SessionCallbackBuilder
import com.google.android.exoplayer2.ext.media2.SessionPlayerConnector

class MyMediaSessionService : MediaSessionService() {
    lateinit var exoPlayer: SimpleExoPlayer
    lateinit var sessionPlayerConnector: SessionPlayerConnector
    lateinit var sessionCallback: MediaSession.SessionCallback
    lateinit var mediaSession: MediaSession

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onCreate() {
        super.onCreate()

        exoPlayer = SimpleExoPlayer.Builder(this).build()
        sessionPlayerConnector = SessionPlayerConnector(exoPlayer, object:
            DefaultMediaItemConverter() {
        })
        sessionCallback = SessionCallbackBuilder(this, sessionPlayerConnector)
            .setMediaItemProvider { session, controllerInfo, mediaId ->
                UriMediaItem.Builder(Uri.parse(mediaId))
                    .setMetadata(
                        MediaMetadata.Builder()
                        .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, mediaId)
                        .build()).build()
            }
            .build()
        mediaSession = MediaSession.Builder(this, sessionPlayerConnector)
            .setSessionCallback(ContextCompat.getMainExecutor(this), sessionCallback)
            .build()
    }
}
