import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_radio_player/flutter_radio_player.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {

  bool isPlaying = false;

  @override
  void initState() {
    super.initState();
    initRadioService();
  }

  Future<void> initRadioService() async {
    try {
      await FlutterRadioPlayer.init("Flutter Radio Example", "Live", "http://142.4.217.133:9735/stream", "albumCover", "appIcon");
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
          child: IconButton(onPressed: () async {
            if (!isPlaying) {
              await FlutterRadioPlayer.play();
              setState(() {
                isPlaying = true;
              });
            } else {
              await FlutterRadioPlayer.pause();
              setState(() {
                isPlaying = false;
              });
            }
          },
          icon: isPlaying ? Icon(Icons.pause) : Icon(Icons.play_arrow),
          )
        ),
      ),
    );
  }
}
