package com.h6ah4i.example.media2

import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import androidx.media2.common.MediaItem
import androidx.media2.common.SessionPlayer
import androidx.media2.common.UriMediaItem
import androidx.media2.player.MediaPlayer
import androidx.media2.session.MediaController
import androidx.media2.session.SessionCommandGroup
import androidx.media2.session.SessionToken
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    lateinit var controller: MediaController
    lateinit var songFiles: Array<String>
    lateinit var playlistAdapter: PlaylistAdapter
    var seekBarInTrackingTouch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initPlayer()
        initUI()
    }

    private fun initUI() {
        textPlayerState.text = mapPlayerState(MediaPlayer.PLAYER_STATE_IDLE)

        playlistAdapter = PlaylistAdapter(songFiles, object: PlaylistAdapter.PlaylistAdapterEventListener {
                override fun onClickPlaylistItem(position: Int, file: String) {
                    playlistAdapter.setCurrentItem(position)

                    logEvent("#skipToPlaylistItem  position: $position, file: $file")
                    controller.skipToPlaylistItem(position)
                    logEvent("#prepare")
                    controller.prepare()
                    logEvent("#play")
                    controller.play()
                }
            })

        recyclerViewPlaylist.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        recyclerViewPlaylist.adapter = playlistAdapter

        buttonPause.setOnClickListener {
            logEvent("pause")
            controller.pause()
        }

        buttonPlay.setOnClickListener {
            logEvent("play")
            controller.play()
        }

        buttonRepeat.setOnClickListener{
            controller.repeatMode = when (controller.repeatMode) {
                MediaPlayer.REPEAT_MODE_NONE -> MediaPlayer.REPEAT_MODE_ALL
                MediaPlayer.REPEAT_MODE_ALL -> MediaPlayer.REPEAT_MODE_ONE
                MediaPlayer.REPEAT_MODE_ONE -> MediaPlayer.REPEAT_MODE_NONE
                else -> TODO()
            }
            logEvent("#repeatMode = ${mapRepeatMode(controller.repeatMode)}")
        }

        buttonShuffle.setOnClickListener{
            controller.shuffleMode = when (controller.shuffleMode) {
                MediaPlayer.SHUFFLE_MODE_NONE -> MediaPlayer.SHUFFLE_MODE_ALL
                MediaPlayer.SHUFFLE_MODE_ALL -> MediaPlayer.SHUFFLE_MODE_NONE
                else -> TODO()
            }
            logEvent("#shuffleMode = ${mapShuffleMode(controller.shuffleMode)}")
        }

        seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    logEvent("#seekTo")
                    controller.seekTo(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                seekBarInTrackingTouch = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                seekBarInTrackingTouch = false
            }
        })
    }

    private fun initPlayer() {
        songFiles = arrayOf("behind_enemy_lines", "black_knight", "hit_n_smash", "new_hero_in_town")

        controller = MediaController.Builder(this)
            .setSessionToken(SessionToken(this, ComponentName(this, MyMediaSessionService::class.java)))
            .setControllerCallback(ContextCompat.getMainExecutor(this),
                object: MediaController.ControllerCallback() {

                    override fun onRepeatModeChanged(controller: MediaController, repeatMode: Int) {
                        logEvent("[onRepeatModeChanged] repeatMode: ${mapRepeatMode(repeatMode)}")

                        buttonRepeat.setImageResource(when (repeatMode) {
                            SessionPlayer.REPEAT_MODE_NONE -> R.drawable.ic_baseline_no_repeat_24
                            SessionPlayer.REPEAT_MODE_ALL -> R.drawable.ic_baseline_repeat_24
                            SessionPlayer.REPEAT_MODE_ONE -> R.drawable.ic_baseline_repeat_one_24
                            else -> TODO()
                        })
                    }

                    override fun onShuffleModeChanged(controller: MediaController, shuffleMode:
                    Int) {
                        logEvent("[onShuffleModeChanged] shuffleMode: ${mapShuffleMode(shuffleMode)}")

                        buttonShuffle.setImageResource(when (shuffleMode) {
                            SessionPlayer.SHUFFLE_MODE_NONE -> R.drawable.ic_baseline_no_shuffle_24
                            SessionPlayer.SHUFFLE_MODE_ALL -> R.drawable.ic_baseline_shuffle_24
                            else -> TODO()
                        })
                    }

                    override fun onPlaybackInfoChanged(controller: MediaController, info: MediaController.PlaybackInfo) {
                    }

//                    override fun onError(player: MediaPlayer, item: MediaItem, what: Int, extra: Int) {
//                        logEvent("[onError] what: $what, extra: $extra")
//                    }
//
//                    override fun onInfo(player: MediaPlayer, item: MediaItem, what: Int, extra: Int) {
//                        logEvent("[onInfo]  what: ${mapInfoWhat(what)}, extra: $extra")
//
//                        if (what == MediaPlayer.MEDIA_INFO_METADATA_UPDATE) {
//                            seekBar.max = player.duration.toInt()
//                            seekBar.progress = player.currentPosition.toInt()
//
//                        }
//                        if (what == MediaPlayer.MEDIA_INFO_BUFFERING_UPDATE && !seekBarInTrackingTouch) {
//                            seekBar.max = player.duration.toInt()
//                            seekBar.progress = player.currentPosition.toInt()
//                        }
//                    }

                    override fun onPlaybackCompleted(controller: MediaController) {
                        logEvent("onPlaybackCompleted")
                        logEvent("Maybe the issue has been reproduced! https://issuetracker.google.com/issues/156152491")
                    }

                    override fun onPlayerStateChanged(controller: MediaController, playerState:
                    Int) {
                        textPlayerState.text = mapPlayerState(playerState)

                        logEvent("[onPlayerStateChanged] playerState: ${textPlayerState.text}")

                        if (playerState == SessionPlayer.PLAYER_STATE_PLAYING) {
                            seekBar.progress = controller.currentPosition.toInt()
                        }
                    }

                    override fun onSeekCompleted(controller: MediaController, position: Long) {
                        logEvent("[onSeekCompleted] position: $position")
                        seekBar.progress = position.toInt()
                    }

                    override fun onCurrentMediaItemChanged(controller: MediaController, item: MediaItem?) {
                        logEvent("[onCurrentMediaItemChanged]")
                        playlistAdapter.setCurrentItem(controller.currentMediaItemIndex)
                        seekBar.max = controller.duration.toInt()
                        seekBar.progress = controller.currentPosition.toInt()
                    }

                    override fun onConnected(
                        controller: MediaController,
                        allowedCommands: SessionCommandGroup
                    ) {
                        controller.setPlaylist(
                            songFiles.map {
                                val uri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + packageName + "/raw/$it")
                                uri.toString()
                            }.toList(),
                            null
                        )
                    }
                })
            .build()

        val handler = Handler(mainLooper)
        val r: Runnable = object : Runnable {
            override fun run() {
                if (!seekBarInTrackingTouch) {
                    seekBar.max = controller.duration.toInt()
                    seekBar.progress = controller.currentPosition.toInt()
                }

                handler.postDelayed(this, 1000)
            }
        }
        handler.post(r)
    }


    private fun logEvent(msg: String) {
        Log.d("Media2Experiment", msg)
    }

    private fun mapPlayerState(playerState: Int) = when (playerState) {
        SessionPlayer.PLAYER_STATE_IDLE -> "IDLE"
        SessionPlayer.PLAYER_STATE_PAUSED -> "PAUSED"
        SessionPlayer.PLAYER_STATE_PLAYING -> "PLAYING"
        SessionPlayer.PLAYER_STATE_ERROR -> "ERROR"
        else -> "Unknown ($playerState)"
    }

    private fun mapRepeatMode(repeatMode: Int) = when (repeatMode) {
        SessionPlayer.REPEAT_MODE_NONE -> "NONE"
        SessionPlayer.REPEAT_MODE_ALL -> "ALL"
        SessionPlayer.REPEAT_MODE_ONE -> "ONE"
        else -> TODO()
    }

    private fun mapShuffleMode(shuffleMode: Int) = when (shuffleMode) {
        SessionPlayer.SHUFFLE_MODE_NONE -> "NONE"
        SessionPlayer.SHUFFLE_MODE_ALL -> "ALL"
        else -> TODO()
    }
}
