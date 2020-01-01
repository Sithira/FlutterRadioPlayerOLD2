import Flutter
import UIKit

public class SwiftFlutterRadioPlayerPlugin: NSObject, FlutterPlugin {
    
    private var radioPlayerService: FlutterRadioPlayerService = FlutterRadioPlayerService()
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "flutter_radio_player", binaryMessenger: registrar.messenger())
        let instance = SwiftFlutterRadioPlayerPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch (call.method) {
        case "initService":
            print("method called to start the radio service")
            if let args = call.arguments as? Dictionary<String, Any>,
                let streamURL = args["streamURL"] as? String,
                let appName = args["appName"] as? String,
                let subTitle = args["subTitle"] as? String
                
//                let appIcon = args["appIcon"] as? String,
//                let appIconBig = args["appIconBig"] as? String
                
            {
                radioPlayerService.initService(streamURL: streamURL, serviceName: appName, secondTitle: subTitle)
                result(nil)
            }
            break
        case "play":
            print("method called to play from service")
            let status = radioPlayerService.play()
            if (status == PlayerStatus.PLAYING) {
                result(true)
            }
            result(false)
            break
        case "pause":
            print("method called to play from service")
            let status = radioPlayerService.pause()
            if (status == PlayerStatus.IDLE) {
                result(true)
            }
            result(false)
            break
        case "stop":
            print("method called to stopped from service")
            let status = radioPlayerService.stop()
            if (status == PlayerStatus.STOPPED) {
                result(true)
            }
            result(false)
            break
        case "isPlaying":
            print("method called to is_playing from service")
            result(radioPlayerService.isPlaying())
            break
        default:
            result(nil)
        }
    }
}
