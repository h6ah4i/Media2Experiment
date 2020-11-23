package com.h6ah4i.example.media2

import android.content.ContentResolver
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    lateinit var player: MediaPlayer
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
                    player.skipToPlaylistItem(position)
                    logEvent("#prepare")
                    player.prepare()
                    logEvent("#play")
                    player.play()
                }
            })

        recyclerViewPlaylist.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        recyclerViewPlaylist.adapter = playlistAdapter

        buttonPause.setOnClickListener {
            logEvent("pause")
            player.pause()
        }

        buttonPlay.setOnClickListener {
            logEvent("play")
            player.play()
        }

        buttonRepeat.setOnClickListener{
            player.repeatMode = when (player.repeatMode) {
                MediaPlayer.REPEAT_MODE_NONE -> MediaPlayer.REPEAT_MODE_ALL
                MediaPlayer.REPEAT_MODE_ALL -> MediaPlayer.REPEAT_MODE_ONE
                MediaPlayer.REPEAT_MODE_ONE -> MediaPlayer.REPEAT_MODE_NONE
                else -> TODO()
            }
            logEvent("#repeatMode = ${mapRepeatMode(player.repeatMode)}")
        }

        buttonShuffle.setOnClickListener{
            player.shuffleMode = when (player.shuffleMode) {
                MediaPlayer.SHUFFLE_MODE_NONE -> MediaPlayer.SHUFFLE_MODE_ALL
                MediaPlayer.SHUFFLE_MODE_ALL -> MediaPlayer.SHUFFLE_MODE_NONE
                else -> TODO()
            }
            logEvent("#shuffleMode = ${mapShuffleMode(player.shuffleMode)}")
        }

        seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    logEvent("#seekTo")
                    player.seekTo(progress.toLong())
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

        player = MediaPlayer(this)

        player.registerPlayerCallback(
            ContextCompat.getMainExecutor(this),
            object: MediaPlayer.PlayerCallback() {

                override fun onRepeatModeChanged(player: SessionPlayer, repeatMode: Int) {
                    logEvent("[onRepeatModeChanged] repeatMode: ${mapRepeatMode(repeatMode)}")

                    buttonRepeat.setImageResource(when (repeatMode) {
                        MediaPlayer.REPEAT_MODE_NONE -> R.drawable.ic_baseline_no_repeat_24
                        MediaPlayer.REPEAT_MODE_ALL -> R.drawable.ic_baseline_repeat_24
                        MediaPlayer.REPEAT_MODE_ONE -> R.drawable.ic_baseline_repeat_one_24
                        else -> TODO()
                    })
                }

                override fun onShuffleModeChanged(player: SessionPlayer, shuffleMode: Int) {
                    logEvent("[onShuffleModeChanged] shuffleMode: ${mapShuffleMode(shuffleMode)}")

                    buttonShuffle.setImageResource(when (shuffleMode) {
                        MediaPlayer.SHUFFLE_MODE_NONE -> R.drawable.ic_baseline_no_shuffle_24
                        MediaPlayer.SHUFFLE_MODE_ALL -> R.drawable.ic_baseline_shuffle_24
                        else -> TODO()
                    })
                }

                override fun onError(player: MediaPlayer, item: MediaItem, what: Int, extra: Int) {
                    logEvent("[onError] what: $what, extra: $extra")
                }

                override fun onInfo(player: MediaPlayer, item: MediaItem, what: Int, extra: Int) {
                    logEvent("[onInfo]  what: ${mapInfoWhat(what)}, extra: $extra")

                    if (what == MediaPlayer.MEDIA_INFO_METADATA_UPDATE) {
                        seekBar.max = player.duration.toInt()
                        seekBar.progress = player.currentPosition.toInt()

                    }
                    if (what == MediaPlayer.MEDIA_INFO_BUFFERING_UPDATE && !seekBarInTrackingTouch) {
                        seekBar.progress = player.currentPosition.toInt()
                    }
                }

                override fun onPlaybackCompleted(player: SessionPlayer) {
                    logEvent("onPlaybackCompleted")
                    logEvent("Maybe the issue has been reproduced! https://issuetracker.google.com/issues/156152491")
                }

                override fun onPlayerStateChanged(player: SessionPlayer, playerState: Int) {
                    textPlayerState.text = mapPlayerState(playerState)

                    logEvent("[onPlayerStateChanged] playerState: ${textPlayerState.text}")

                    if (playerState == SessionPlayer.PLAYER_STATE_PLAYING) {
                        seekBar.progress = player.currentPosition.toInt()
                    }
                }

                override fun onSeekCompleted(player: SessionPlayer, position: Long) {
                    logEvent("[onSeekCompleted] position: $position")
                    seekBar.progress = position.toInt()
                }

                override fun onCurrentMediaItemChanged(player: SessionPlayer, item: MediaItem) {
                    logEvent("[onCurrentMediaItemChanged]")
                    playlistAdapter.setCurrentItem(player.currentMediaItemIndex)
                }
            })

        player.playerVolume = 1.0f
        player.audioSessionId = (getSystemService(Context.AUDIO_SERVICE) as AudioManager).generateAudioSessionId()
        AudioManagerCompat.requestAudioFocus(
            getSystemService(Context.AUDIO_SERVICE) as AudioManager,
            AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener {  }
                .build())

        player.setAudioAttributes(
            AudioAttributesCompat.Builder()
                .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributesCompat.USAGE_MEDIA)
                .setLegacyStreamType(AudioManager.STREAM_MUSIC)
            .build()
        )

        player.setPlaylist(
            songFiles.map {
                val uri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + packageName + "/raw/$it")
                UriMediaItem.Builder(uri).build()
            }.toList(),
            null
        )

        player.prepare()
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

    private fun mapInfoWhat(what: Int) = when (what) {
//        MediaPlayer.MEDIA_INFO_MEDIA_ITEM_START -> "MEDIA_ITEM_START"
        MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> "VIDEO_RENDERING_START"
//        MediaPlayer.MEDIA_INFO_MEDIA_ITEM_END -> "MEDIA_ITEM_END"
//        MediaPlayer.MEDIA_INFO_MEDIA_ITEM_LIST_END -> "MEDIA_ITEM_LIST_END"
//        MediaPlayer.MEDIA_INFO_MEDIA_ITEM_REPEAT -> "MEDIA_ITEM_REPEAT"
//        MediaPlayer.MEDIA_INFO_PREPARED -> "PREPARED"
        MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING -> "VIDEO_TRACK_LAGGING"
//        MediaPlayer.MEDIA_INFO_BUFFERING_START -> "BUFFERING_START"
//        MediaPlayer.MEDIA_INFO_BUFFERING_END -> "BUFFERING_END"
//        MediaPlayer.MEDIA_INFO_NETWORK_BANDWIDTH -> "NETWORK_BANDWIDTH"
        MediaPlayer.MEDIA_INFO_BUFFERING_UPDATE -> "BUFFERING_UPDATE"
        MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING -> "BAD_INTERLEAVING"
        MediaPlayer.MEDIA_INFO_NOT_SEEKABLE -> "NOT_SEEKABLE"
        MediaPlayer.MEDIA_INFO_METADATA_UPDATE -> "METADATA_UPDATE"
//        MediaPlayer.MEDIA_INFO_EXTERNAL_METADATA_UPDATE -> "EXTERNAL_METADATA_UPDATE"
        MediaPlayer.MEDIA_INFO_AUDIO_NOT_PLAYING -> "AUDIO_NOT_PLAYING"
        MediaPlayer.MEDIA_INFO_VIDEO_NOT_PLAYING -> "VIDEO_NOT_PLAYING"
//        MediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE -> "UNSUPPORTED_SUBTITLE"
//        MediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT -> "SUBTITLE_TIMED_OUT"
        else -> "Unknown ($what)"
    }

    private fun mapRepeatMode(repeatMode: Int) = when (repeatMode) {
        MediaPlayer.REPEAT_MODE_NONE -> "NONE"
        MediaPlayer.REPEAT_MODE_ALL -> "ALL"
        MediaPlayer.REPEAT_MODE_ONE -> "ONE"
        else -> TODO()
    }

    private fun mapShuffleMode(shuffleMode: Int) = when (shuffleMode) {
        MediaPlayer.SHUFFLE_MODE_NONE -> "NONE"
        MediaPlayer.SHUFFLE_MODE_ALL -> "ALL"
        else -> TODO()
    }
}
