import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_radio_player/flutter_radio_player.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  var playerState = FlutterRadioPlayer.flutter_radio_paused;

  var volume = 0.8;

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
          "http://209.133.216.3:7018/stream", "albumCover", "appIcon");
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
          child: Column(
            children: <Widget>[
              StreamBuilder(
                  stream: _flutterRadioPlayer.isPlayingStream,
                  initialData: widget.playerState,
                  builder:
                      (BuildContext context, AsyncSnapshot<String> snapshot) {
                    String returnData = snapshot.data;
                    print("object data: " + returnData);
                    if (returnData ==
                        FlutterRadioPlayer.flutter_radio_stopped) {
                      return RaisedButton(
                          child: Text("Start listening now"),
                          onPressed: () async {
                            await initRadioService();
                          });
                    } else {
                      return IconButton(
                          onPressed: () async {
                            print("button press data: " +
                                snapshot.data.toString());
                            await _flutterRadioPlayer.playOrPause();
                          },
                          icon: snapshot.data ==
                                  FlutterRadioPlayer.flutter_radio_playing
                              ? Icon(Icons.pause)
                              : Icon(Icons.play_arrow));
                    }
                  }),
              Slider(value: widget.volume, min: 0, max: 1.0, onChanged: (value) => setState(() {
                widget.volume = value;
                _flutterRadioPlayer.setVolume(widget.volume);
              }))
            ],
          ),
        ),
      ),
    );
  }
}
