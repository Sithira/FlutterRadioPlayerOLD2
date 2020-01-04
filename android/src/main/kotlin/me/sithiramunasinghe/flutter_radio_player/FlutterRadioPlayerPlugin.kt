package me.sithiramunasinghe.flutter_radio_player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.annotation.NonNull
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
        var applicationContext: Context? = null
        var radioPlayerService: RadioPlayerService? = null
        var isBound = false
        var serviceIntent: Intent? = null

        const val TAG: String = "FlutterRadioPlayerPlgin"
    }

    override fun onMethodCall(call: MethodCall, result: Result) {

        when (call.method) {
            PlayerMethods.IS_PLAYING.value -> {
                if (mEventSink != null) {
                    val playStatus = radioPlayerService?.isPlaying()
                    Log.d(TAG, "is playing service invoked with result: $playStatus")
                    result.success(playStatus)
                    mEventSink!!.success(playStatus)
                }
            }
            PlayerMethods.PLAYORPAUSE.value -> {
                if (radioPlayerService?.isPlaying()!!) {
                    radioPlayerService?.pause()
                } else {
                    radioPlayerService?.play()
                }

                result.success(radioPlayerService?.isPlaying())
            }
            PlayerMethods.PLAY.value -> {
                Log.d(TAG, "play service invoked")
                if (radioPlayerService != null) {
                    radioPlayerService?.play()
                    methodChannel?.invokeMethod("isPlaying", true)

                }

                result.success(null)
            }
            PlayerMethods.PAUSE.value -> {
                Log.d(TAG, "pause service invoked")
                if (radioPlayerService != null) {
                    radioPlayerService?.pause()
                    methodChannel?.invokeMethod("isPlaying", false)
                }
                result.success(null)
            }
            PlayerMethods.STOP.value -> {
                Log.d(TAG, "stop service invoked")
                if (radioPlayerService != null) {
                    applicationContext?.unbindService(serviceConnection)
                    radioPlayerService?.stop()
                    methodChannel?.invokeMethod("isPlaying", false)
                }
                result.success(null)
            }
            PlayerMethods.INIT.value -> {

                Log.d(TAG, "start service invoked")

                // player item
                val playerItem = getPlayerItem(call)

                if (radioPlayerService != null) {

                    if (radioPlayerService?.streamURL != playerItem.streamUrl) {
                        radioPlayerService?.stop()
                        serviceIntent = setIntentData(serviceIntent!!, playerItem)

                        if (!isBound) {
                            applicationContext?.bindService(serviceIntent, serviceConnection, Context.BIND_IMPORTANT)
                        }
                        applicationContext?.startService(serviceIntent)
                    } else {
                        Log.d(TAG, "Player is already playing..")
                    }
                } else {
                    serviceIntent = setIntentData(serviceIntent!!, playerItem)

                    if (!isBound) {
                        applicationContext?.bindService(serviceIntent, serviceConnection, Context.BIND_IMPORTANT)
                    }
                    applicationContext?.startService(serviceIntent)
                }

                result.success(null)

            }
//            "unbind" -> {
//                Log.d(TAG, "unbind service invoked")
//                if (radioPlayerService != null) {
//                    applicationContext?.unbindService(serviceConnection)
//                    result.success(null)
//                }
//            }
//            "checkIfBound" -> {
//                Log.d(TAG, "checking bound service invoked")
//                if (!isBound) {
//                    applicationContext?.bindService(serviceIntent, serviceConnection, Context.BIND_IMPORTANT)
//                }
//                result.success(null)
//            }
            else -> result.notImplemented()
        }

    }

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

    private fun setIntentData(intent: Intent, playerItem: PlayerItem): Intent {
        intent.putExtra("streamUrl", playerItem.streamUrl)
        intent.putExtra("appName", playerItem.appName)
        intent.putExtra("subTitle", playerItem.subTitle)
        intent.putExtra("appIcon", playerItem.appIcon)
        intent.putExtra("appIconBig", playerItem.appIconBig)
        return intent
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPluginBinding) {
        applicationContext = null
        methodChannel?.setMethodCallHandler(null)
        methodChannel = null
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

    override fun onListen(arguments: Any?, events: EventSink?) {
        mEventSink = events
    }

    override fun onCancel(arguments: Any?) {
        mEventSink = null
    }
}
