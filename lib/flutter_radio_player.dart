import 'dart:async';

import 'package:flutter/services.dart';
import 'package:rxdart/rxdart.dart';

class FlutterRadioPlayer {
  static const MethodChannel _channel =
      const MethodChannel('flutter_radio_player');

  static const EventChannel _eventChannel = const EventChannel("flutter_radio_player_stream");

  static Stream<bool> _isPlayingStream;

  Future<void> init(String appName, String subTitle, String streamURL,
      String albumCover, String appIcon) async {
    return await _channel.invokeMethod("initService", {
      "streamURL": streamURL,
      "appName": appName,
      "subTitle": subTitle,
      "appIcon": appIcon,
      "appIconBig": albumCover
    });
  }

  Future<bool> play() async {
    return await _channel.invokeMethod("play");
  }

  Future<bool> pause() async {
    return await _channel.invokeMethod("pause");
  }

  Future<bool> playOrPause() async {
    return await _channel.invokeMethod("playOrPause");
  }

  Future<bool> stop() async {
    return await _channel.invokeMethod("stop");
  }

  Future<bool> isPlaying() async {
    bool isPlaying = await _channel.invokeMethod("isPlaying");
    return isPlaying;
  }

  get isPlayingStream {
    if (_isPlayingStream == null) {
      _isPlayingStream = _eventChannel.receiveBroadcastStream().map<bool>((value) => value);
    }
    return _isPlayingStream;
  }

}
