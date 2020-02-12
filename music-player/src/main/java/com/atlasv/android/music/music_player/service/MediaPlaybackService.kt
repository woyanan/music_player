package com.atlasv.android.music.music_player.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import com.atlasv.android.music.music_player.AudioPlayer
import com.atlasv.android.music.music_player.R
import com.atlasv.android.music.music_player.playback.IPlaybackManager

/**
 * Created by woyanan on 2020-02-10
 */
class MediaPlaybackService : MediaBrowserServiceCompat() {
    companion object {
        private const val BROWSABLE_ROOT = "/"
        private const val EMPTY_ROOT = "@empty@"
    }

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var packageValidator: PackageValidator
    private var playbackManager: IPlaybackManager? = null
    //UI可能被销毁,Service需要保存播放列表,并处理循环模式
    private var playList = arrayListOf<MediaSessionCompat.QueueItem>()
    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            super.onPlayFromMediaId(mediaId, extras)
            mediaId?.apply {
                playList.forEach {
                    if (it.description.mediaId == mediaId) {
                        playbackManager?.play(it.description, true)
                        return@forEach
                    }
                }
            }
        }

        override fun onAddQueueItem(description: MediaDescriptionCompat?) {
            super.onAddQueueItem(description)
            println("--------------------->mediaId: " + description?.mediaId)
            if (playList.find { it.description.mediaId == description?.mediaId } == null) {
                playList.add(
                    MediaSessionCompat.QueueItem(description, description.hashCode().toLong())
                )
            }
//            mediaSession.setQueue(playList)
        }

        override fun onRemoveQueueItem(description: MediaDescriptionCompat?) {
            super.onRemoveQueueItem(description)

        }
    }

    override fun onCreate() {
        super.onCreate()
        playbackManager = AudioPlayer.getInstance(this).getPlaybackManager()
        mediaSession = MediaSessionCompat(this, "MusicService")
            .apply {
                setSessionActivity(getPendingIntent())
                isActive = true
            }
        sessionToken = mediaSession.sessionToken
        mediaSession.setSessionActivity(getPendingIntent())
        mediaSession.setCallback(mediaSessionCallback)
        mediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                    or MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS
                    or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )

        packageValidator = PackageValidator(this, R.xml.allowed_media_browser_callers)
    }

    private fun getPendingIntent(): PendingIntent {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        return PendingIntent.getActivity(this, 0, intent, 0)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        println("--------------------->onLoadChildren, parentId: $parentId")
        //no-op
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        val isKnownCaller = packageValidator.isKnownCaller(clientPackageName, clientUid)
        return if (isKnownCaller) {
            BrowserRoot(BROWSABLE_ROOT, null)
        } else {
            BrowserRoot(EMPTY_ROOT, null)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}