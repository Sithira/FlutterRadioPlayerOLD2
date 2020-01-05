package me.sithiramunasinghe.flutter_radio_player.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.session.MediaSession
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import androidx.annotation.Nullable
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.util.Util.getUserAgent
import me.sithiramunasinghe.flutter_radio_player.FLUTTER_RADIO_PAUSED
import me.sithiramunasinghe.flutter_radio_player.FLUTTER_RADIO_PLAYING
import me.sithiramunasinghe.flutter_radio_player.FLUTTER_RADIO_STOPPED
import me.sithiramunasinghe.flutter_radio_player.FlutterRadioPlayerPlugin.Companion.broadcastActionName
import me.sithiramunasinghe.flutter_radio_player.R
import me.sithiramunasinghe.flutter_radio_player.enums.PlaybackStatus
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class RadioPlayerService : Service(), AudioManager.OnAudioFocusChangeListener, AnkoLogger {

    private var isBound = false

    private val iBinder = LocalBinder()

    private lateinit var playbackStatus: PlaybackStatus

    private var localBroadcastManager: LocalBroadcastManager? = null

    // context
    private val context = this

    // class instances
    private var player: SimpleExoPlayer? = null
    private var mediaSessionConnector: MediaSessionConnector? = null
    private var mediaSession: MediaSession? = null
    private val playerNotificationManager: PlayerNotificationManager? = null

    // session keys
    private val playbackNotificationId = 1025
    private val mediaSessionId = "flutter_radio_radio_media_session"
    private val playbackChannelId = "flutter_radio_player_channel_id"

    // stream URL
    var streamURL: String? = null

    inner class LocalBinder : Binder() {
        internal val service: RadioPlayerService
            get() = this@RadioPlayerService
    }

    override fun onDestroy() {
        super.onDestroy()

        streamURL = ""

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaSession?.release()
        }

        mediaSessionConnector?.setPlayer(null)
        playerNotificationManager?.setPlayer(null)
        player?.release()
        player = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        localBroadcastManager = LocalBroadcastManager.getInstance(context)

        // get details
        val appName = intent!!.getStringExtra("appName")
        val subTitle = intent.getStringExtra("subTitle")
        val streamUrl = intent.getStringExtra("streamUrl")
        val appIcon = intent.getStringExtra("appIcon")
        val appIconBig = intent.getStringExtra("appIconBig")

        streamURL = streamUrl

        player = SimpleExoPlayer.Builder(context).build()

        val dataSourceFactory = DefaultDataSourceFactory(context, getUserAgent(context, appName))

        val audioSource = buildMediaSource(dataSourceFactory, streamURL!!)

        val playerEvents = object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {

                val broadcastIntent = Intent(broadcastActionName)

                playbackStatus = when (playbackState) {
                    Player.STATE_BUFFERING -> PlaybackStatus.LOADING
                    Player.STATE_ENDED -> {
                        stopSelf()
                        localBroadcastManager?.sendBroadcast(broadcastIntent.putExtra("status", FLUTTER_RADIO_STOPPED))
                        PlaybackStatus.STOPPED
                    }
                    Player.STATE_READY -> if (playWhenReady) {
                        localBroadcastManager?.sendBroadcast(broadcastIntent.putExtra("status", FLUTTER_RADIO_PLAYING))
                        PlaybackStatus.PLAYING
                    } else {
                        localBroadcastManager?.sendBroadcast(broadcastIntent.putExtra("status", FLUTTER_RADIO_PAUSED))
                        PlaybackStatus.PAUSED
                    }
                    else -> {
                        localBroadcastManager?.sendBroadcast(broadcastIntent.putExtra("status", FLUTTER_RADIO_STOPPED))
                        PlaybackStatus.IDLE
                    }
                }

                info {
                    "onPlayerStateChanged: $playbackStatus"
                }
            }

            override fun onPlayerError(error: ExoPlaybackException) {
                playbackStatus = PlaybackStatus.ERROR
            }
        }

        // set exo player configs
        player?.let {
            it.addListener(playerEvents)
            // it.playWhenReady = true
            it.prepare(audioSource)
        }

        val playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
                context,
                playbackChannelId,
                R.string.channel_name,
                R.string.channel_description,
                playbackNotificationId,
                object : PlayerNotificationManager.MediaDescriptionAdapter {
                    override fun getCurrentContentTitle(player: Player): String {
                        return appName
                    }

                    @Nullable
                    override fun createCurrentContentIntent(player: Player): PendingIntent {
                        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                    }

                    @Nullable
                    override fun getCurrentContentText(player: Player): String? {
                        return subTitle
                    }

                    @Nullable
                    override fun getCurrentLargeIcon(player: Player, callback: PlayerNotificationManager.BitmapCallback): Bitmap? {
                        return if (intent.getStringExtra("bigIcon") == null) {
                            null
                        } else {

                            val resourceId = applicationContext?.resources?.getIdentifier(
                                    intent.getStringExtra("bigIcon"),
                                    "drawable",
                                    applicationContext?.packageName)

                            return BitmapFactory
                                    .decodeResource(applicationContext?.resources, resourceId!!)

                        }
                    }
                },
                object : PlayerNotificationManager.NotificationListener {
                    override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                        isBound = false
                        player = null
                        stopSelf()
                    }

                    override fun onNotificationPosted(notificationId: Int, notification: Notification, ongoing: Boolean) {
                        startForeground(notificationId, notification)
                    }
                }
        )

//        if (intent.getStringExtra("appIcon") != null) {
//            val appIcon = pluginRegistrar?.context()?.resources!!.getIdentifier(
//                    intent.getStringExtra("appIcon"),
//                    "drawable",
//                    pluginRegistrar?.context()?.packageName)
//
//            playerNotificationManager.setSmallIcon(appIcon)
//        }

        val mediaSession = MediaSessionCompat(context, mediaSessionId)
        mediaSession.isActive = true

        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector!!.setPlayer(player)

        playerNotificationManager.setUseStopAction(true)
        playerNotificationManager.setFastForwardIncrementMs(0)
        playerNotificationManager.setRewindIncrementMs(0)
        playerNotificationManager.setUsePlayPauseActions(true)
        playerNotificationManager.setUseNavigationActions(false)
        playerNotificationManager.setUseNavigationActionsInCompactView(false)

        playerNotificationManager.setPlayer(player)
        playerNotificationManager.setMediaSessionToken(mediaSession.sessionToken)

        playbackStatus = PlaybackStatus.PLAYING

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return iBinder
    }

    override fun onAudioFocusChange(audioFocus: Int) {
        when (audioFocus) {

            AudioManager.AUDIOFOCUS_GAIN -> {
                player?.volume = 0.8f
                play()
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                stop()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (isPlaying()) {
                    stop()
                }
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (isPlaying()) {
                    player?.volume = 0.1f
                }
            }
        }
    }


    /**
     * Build the media source depending of the URL content type.
     */
    private fun buildMediaSource(dataSourceFactory: DefaultDataSourceFactory, streamUrl: String): MediaSource {

        val uri = Uri.parse(streamUrl)

        return when (val type = Util.inferContentType(uri)) {
            C.TYPE_HLS -> HlsMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
            C.TYPE_OTHER -> ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
            else -> {
                throw IllegalStateException("Unsupported type: $type")
            }
        }
    }


    fun pause() {
        info { "pausing audio..." }
        player?.playWhenReady = false
    }

    fun play() {
        info { "playing audio $player ..." }
        player?.playWhenReady = true
    }

    fun stop() {
        info { "stopping audio $player ..." }
        player?.stop()
    }

    fun isPlaying(): Boolean {
        val isPlaying = this.playbackStatus == PlaybackStatus.PLAYING
        info { "is playing status: $isPlaying" }
        return isPlaying
    }

}