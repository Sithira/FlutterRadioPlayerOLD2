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
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import me.sithiramunasinghe.flutter_radio_player.services.PlayerItem
import me.sithiramunasinghe.flutter_radio_player.services.RadioPlayerService


/** FlutterRadioPlayerPlugin */
public class FlutterRadioPlayerPlugin : FlutterPlugin, MethodCallHandler {

    private var methodChannel: MethodChannel? = null

    private val methodChannelName = "flutter_radio_player"

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPluginBinding) {
        onAttachedToEngine(flutterPluginBinding.applicationContext, flutterPluginBinding.binaryMessenger)
    }

    private fun onAttachedToEngine(context: Context, binaryMessenger: BinaryMessenger) {
        applicationContext = context
        methodChannel = MethodChannel(binaryMessenger, methodChannelName)
        methodChannel!!.setMethodCallHandler(this)

        serviceIntent = Intent(applicationContext, RadioPlayerService::class.java)
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
            "initService" -> {

                Log.d(TAG, "start service invoked")

                // todo: move to private method.
                val url = call.argument<String>("streamURL")
                val appName = call.argument<String>("appName")
                val subTitle = call.argument<String>("subTitle")
                val appIcon = call.argument<String>("appIcon")
                val bigIcon = call.argument<String>("appIconBig")

                val playerItem = PlayerItem(appName!!, subTitle!!, url!!, appIcon!!, bigIcon!!)

                if (radioPlayerService != null) {

                    if (radioPlayerService?.streamURL != url) {
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
            "stop" -> {
                Log.d(TAG, "stop service invoked")
                if (radioPlayerService != null) {
                    applicationContext?.unbindService(serviceConnection)
                    radioPlayerService?.stop()
                }
                result.success(null)
            }
            "pause" -> {
                Log.d(TAG, "pause service invoked")
                if (radioPlayerService != null) {
                    radioPlayerService?.pause()
                }
                result.success(null)
            }
            "play" -> {
                Log.d(TAG, "play service invoked")
                if (radioPlayerService != null) {
                    radioPlayerService?.play()
                }

                result.success(null)
            }
            "unbind" -> {
                Log.d(TAG, "unbind service invoked")
                if (radioPlayerService != null) {
                    applicationContext?.unbindService(serviceConnection)
                    result.success(null)
                }
            }
            "checkIfBound" -> {
                Log.d(TAG, "checking bound service invoked")
                if (!isBound) {
                    applicationContext?.bindService(serviceIntent, serviceConnection, Context.BIND_IMPORTANT)
                }
                result.success(null)
            }
            "isPlaying" -> {
                Log.d(TAG, "is playing service invoked")
                result.success(radioPlayerService?.isPlaying())
            }
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
}
