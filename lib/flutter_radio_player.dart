import 'dart:async';
import 'dart:wasm';

import 'package:flutter/services.dart';

class FlutterRadioPlayer {

  static const MethodChannel _channel = const MethodChannel('flutter_radio_player');

  static Future<Void> init(String appName, String subTitle, String streamURL,
      String albumCover, String appIcon) async {
    return await _channel.invokeMethod("initService", {
      "streamURL": streamURL,
      "appName": appName,
      "subTitle": subTitle,
      "appIcon": appIcon,
      "appIconBig": albumCover
    });
  }

  static Future<bool> play() async {
    return await _channel.invokeMethod("play");
  }

  static Future<bool> pause() async {
    return await _channel.invokeMethod("pause");
  }

  static Future<bool> stop() async {
    return await _channel.invokeMethod("stop");
  }

  static Future<bool> isPlaying() async {
    return await _channel.invokeMethod("isPlaying");
  }
}
