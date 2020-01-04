import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_radio_player/flutter_radio_player.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  FlutterRadioPlayer _flutterRadioPlayer = FlutterRadioPlayer();

  @override
  void initState() {
    super.initState();
    initRadioService();
  }

  Future<void> initRadioService() async {
    try {
      await _flutterRadioPlayer.init("Flutter Radio Example", "Live",
          "http://142.4.217.133:9735/stream", "albumCover", "appIcon");
    } on PlatformException {
      print("Exception occured while trying to register the services.");
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Flutter Radio Player Example'),
        ),
        body: Center(
          child: StreamBuilder(
              stream: _flutterRadioPlayer.isPlayingStream,
              initialData: false,
              builder: (BuildContext context, AsyncSnapshot<bool> snapshot) {
                print("object data: " + snapshot.data.toString());
                return IconButton(
                    onPressed: () async {
                      print("button press data: " + snapshot.data.toString());
                      await _flutterRadioPlayer.playOrPause();
                    },
                    icon: snapshot.data
                        ? Icon(Icons.pause)
                        : Icon(Icons.play_arrow));
              }),
        ),
      ),
    );
  }
}
