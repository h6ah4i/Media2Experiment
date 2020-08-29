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

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media.AudioAttributesCompat
import androidx.media2.common.MediaItem
import androidx.media2.common.MediaMetadata
import androidx.media2.common.UriMediaItem
import androidx.media2.player.MediaPlayer
import androidx.media2.session.MediaSession
import androidx.media2.session.MediaSessionService
import androidx.media2.session.SessionCommand
import androidx.media2.session.SessionCommandGroup

class MyMediaSessionService : MediaSessionService() {
    lateinit var player: MediaPlayer

    override fun onCreate() {
        super.onCreate()

        player = MediaPlayer(this)

        player.audioSessionId = (getSystemService(Context.AUDIO_SERVICE) as AudioManager)
            .generateAudioSessionId()

        player.setAudioAttributes(
            AudioAttributesCompat.Builder()
                .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributesCompat.USAGE_MEDIA)
                .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                .build()
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        return MediaSession.Builder(this, player).setSessionCallback(ContextCompat
            .getMainExecutor(this), object: MediaSession.SessionCallback() {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): SessionCommandGroup? {
                return super.onConnect(session, controller)
            }

            override fun onCommandRequest(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                command: SessionCommand
            ): Int {
                return super.onCommandRequest(session, controller, command)
            }

            override fun onCreateMediaItem(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaId: String
            ): MediaItem? {
                return UriMediaItem.Builder(Uri.parse(mediaId))
                    .setMetadata(MediaMetadata.Builder()
                        .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, mediaId)
                        .build()).build()
            }
        }).build()
    }

}