package me.sithiramunasinghe.flutter_radio_player

import android.content.*
import android.os.IBinder
import android.util.Log
import androidx.annotation.NonNull
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import me.sithiramunasinghe.flutter_radio_player.enums.PlayerMethods
import me.sithiramunasinghe.flutter_radio_player.services.PlayerItem
import me.sithiramunasinghe.flutter_radio_player.services.RadioPlayerService


/** FlutterRadioPlayerPlugin */
public class FlutterRadioPlayerPlugin : FlutterPlugin, MethodCallHandler, EventChannel.StreamHandler {

    private var methodChannel: MethodChannel? = null

    private val methodChannelName = "flutter_radio_player"
    private val eventChannelName = "flutter_radio_player_stream"

    private var mEventSink: EventSink? = null

    private var playerItem: PlayerItem? = null

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPluginBinding) {
        onAttachedToEngine(flutterPluginBinding.applicationContext, flutterPluginBinding.binaryMessenger)
    }

    private fun onAttachedToEngine(context: Context, binaryMessenger: BinaryMessenger) {
        applicationContext = context
        methodChannel = MethodChannel(binaryMessenger, methodChannelName)
        methodChannel!!.setMethodCallHandler(this)

        serviceIntent = Intent(applicationContext, RadioPlayerService::class.java)

        val eventChannel = EventChannel(binaryMessenger, eventChannelName)
        eventChannel.setStreamHandler(this)

        LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, IntentFilter("playback_status"))
    }

    // This static function is optional and equivalent to onAttachedToEngine. It supports the old
    // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
    // plugin registration via this function while apps migrate to use the new Android APIs
    // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
    //
    // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
    // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
    // depending on the user's project. onAttachedToEngine or registerWith must both be defined
    // in the same class.
    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val instance = FlutterRadioPlayerPlugin()
            instance.onAttachedToEngine(registrar.activeContext(), registrar.messenger())
        }

        const val broadcastActionName = "playback_status"
        const val TAG: String = "FlutterRadioPlayerPlgin"

        var applicationContext: Context? = null
        var radioPlayerService: RadioPlayerService? = null
        var isBound = false
        var serviceIntent: Intent? = null
    }

    override fun onMethodCall(call: MethodCall, result: Result) {

        when (call.method) {
            PlayerMethods.IS_PLAYING.value -> {
                val playStatus = isPlaying()
                Log.d(TAG, "is playing service invoked with result: $playStatus")
                result.success(playStatus)
            }
            PlayerMethods.PLAY_PAUSE.value -> {
                playOrPause()
                result.success(null)
            }
            PlayerMethods.PLAY.value -> {
                Log.d(TAG, "play service invoked")
                play()
                result.success(null)
            }
            PlayerMethods.PAUSE.value -> {
                Log.d(TAG, "pause service invoked")
                pause()
                result.success(null)
            }
            PlayerMethods.STOP.value -> {
                Log.d(TAG, "stop service invoked")
                stop()
                result.success(null)
            }
            PlayerMethods.INIT.value -> {
                Log.d(TAG, "start service invoked")
                playerItem = getPlayerItem(call)
                init()
                result.success(null)
            }
            PlayerMethods.SET_VOLUME.value -> {
                val volume = call.argument<Double>("volume")!!
                Log.d(TAG, "Changing volume to: $volume")
                setVolume(volume)
                result.success(null)
            }
            else -> result.notImplemented()
        }

    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPluginBinding) {
        applicationContext = null
        methodChannel?.setMethodCallHandler(null)
        methodChannel = null
        LocalBroadcastManager.getInstance(applicationContext!!).unregisterReceiver(broadcastReceiver)
    }

    override fun onListen(arguments: Any?, events: EventSink?) {
        mEventSink = events
    }

    override fun onCancel(arguments: Any?) {
        mEventSink = null
    }

    /**
     * Make a new player item obj from method call.
     */
    private fun getPlayerItem(methodCall: MethodCall): PlayerItem {

        Log.d(TAG, "Mapping method call to player item object")

        val url = methodCall.argument<String>("streamURL")
        val appName = methodCall.argument<String>("appName")
        val subTitle = methodCall.argument<String>("subTitle")
        val appIcon = methodCall.argument<String>("appIcon")
        val bigIcon = methodCall.argument<String>("appIconBig")

        return PlayerItem(appName!!, subTitle!!, url!!, appIcon!!, bigIcon!!)
    }

    /**
     * Set data for the RadioPlayerService
     */
    private fun setIntentData(intent: Intent, playerItem: PlayerItem): Intent {
        intent.putExtra("streamUrl", playerItem.streamUrl)
        intent.putExtra("appName", playerItem.appName)
        intent.putExtra("subTitle", playerItem.subTitle)
        intent.putExtra("appIcon", playerItem.appIcon)
        intent.putExtra("appIconBig", playerItem.appIconBig)
        return intent
    }

    /**
     * Initializes the connection
     */
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            radioPlayerService = null
        }

        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as RadioPlayerService.LocalBinder
            radioPlayerService = localBinder.service
            isBound = true
        }
    }

    /**
     * Broadcast receiver for the playback callbacks
     */
    private var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                val returnStatus = intent.getStringExtra("status")
                println("Received status: $returnStatus")
                mEventSink?.success(returnStatus)
            }
        }
    }

    private fun init() {
        Log.i(TAG, "Attempting to initialize service...")
        if (!isBound) {
            Log.i(TAG, "Service not bound, binding now....")
            serviceIntent = setIntentData(serviceIntent!!, playerItem!!)
            applicationContext!!.bindService(serviceIntent, serviceConnection, Context.BIND_IMPORTANT)
            applicationContext!!.startService(serviceIntent)
        }
    }

    private fun isPlaying(): Boolean {
        Log.i(TAG, "Attempting to get playing status....")
        val playingStatus = radioPlayerService!!.isPlaying()
        Log.i(TAG, "Payback-status: $playingStatus")
        return playingStatus
    }

    private fun playOrPause() {
        Log.i(TAG, "Attempting to either play or pause...")
        if (isPlaying()) pause() else play()
    }

    private fun play() {
        Log.i(TAG, "Attempting to play music....")
        radioPlayerService?.play()
    }

    private fun pause() {
        Log.i(TAG, "Attempting to pause music....")
        radioPlayerService!!.pause()
    }

    private fun stop() {
        Log.i(TAG, "Attempting to stop music and unbind services....")
        applicationContext!!.unbindService(serviceConnection)
        radioPlayerService!!.stop()
    }

    private fun setVolume(volume: Double) {
        Log.i(TAG, "Attempting to change volume...")
        radioPlayerService!!.setVolume(volume)
    }
}
