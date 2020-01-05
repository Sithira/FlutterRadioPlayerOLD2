# Flutter radio player

This plugin was developed for streaming media such as radio with the support for both Android and iOS in mind.
Hence it will not support functions such as skipping, seeking, forward and backwards.

## Features
Flutter's EventStreams have been used to extract player events from both Android and iOS for reactivity of the player

### For Android
This plugin wraps around Google's famous EXOPlayer Library for playback support. We utilize audio capabilities that
the EXOPlayer library offers us such as *foreground* running and notification control and lock-screen control support.

### For iOS
This plugin wraps around Apple's native AVFoundation for playback support. it also supports Notification center
controls and Lock-screen control support.

## Getting Started

This project is a starting point for a Flutter
[plug-in package](https://flutter.dev/developing-packages/),
a specialized package that includes platform-specific implementation code for
Android and/or iOS.

For help getting started with Flutter, view our 
[online documentation](https://flutter.dev/docs), which offers tutorials, 
samples, guidance on mobile development, and a full API reference.
