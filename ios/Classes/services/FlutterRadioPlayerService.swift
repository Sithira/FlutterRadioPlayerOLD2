//
//  FlutterRadioPlayerService.swift
//  flutter_radio_player
//
//  Created by Sithira on 12/30/19.
//

import Foundation
import AVFoundation
import MediaPlayer

class FlutterRadioPlayerService {
    
    private var avPlayer: AVPlayer?
    private var avPlayerItem: AVPlayerItem?
    
    func initService(streamURL: String, serviceName: String, secondTitle: String) -> Void {
        print("stream url: " + streamURL)
        
        let streamURLInstance = URL(string: streamURL)
        
        avPlayer = AVPlayer()
        avPlayerItem = AVPlayerItem(url: streamURLInstance!)
        avPlayer = AVPlayer(playerItem: avPlayerItem!)
        
        initRemoteTransportControl(appName: serviceName, subTitle: secondTitle);
    }
    
    func play() -> PlayerStatus {
        print("invoking play method on service")
        if(!isPlaying()) {
            avPlayer?.play()
            return PlayerStatus.PLAYING
        }
        
        return PlayerStatus.IDLE
    }
    
    func pause() -> PlayerStatus {
        print("invoking pause method on service")
        if (isPlaying()) {
            avPlayer?.pause()
            return PlayerStatus.IDLE
        }
        
        return PlayerStatus.IDLE
    }
    
    func stop() -> PlayerStatus {
        print("invoking stop method on service")
        if (isPlaying()) {
            avPlayer = AVPlayer()
        }
        
        return PlayerStatus.IDLE
    }
    
    func isPlaying() -> Bool {
        let status = (avPlayer?.rate != 0 && avPlayer?.error == nil) ? true : false
        print("isPlaying status: \(status)")
        return status
    }
    
    private func initRemoteTransportControl(appName: String, subTitle: String) {
        
        do {
            let commandCenter = MPRemoteCommandCenter.shared()
            
            // build now playing info
            let nowPlayingInfo = [MPMediaItemPropertyTitle : appName, MPMediaItemPropertyArtist: subTitle]
            
            MPNowPlayingInfoCenter.default().nowPlayingInfo = nowPlayingInfo
            
            // basic command center options
            commandCenter.togglePlayPauseCommand.isEnabled = true
            commandCenter.playCommand.isEnabled = true
            commandCenter.pauseCommand.isEnabled = true
            commandCenter.nextTrackCommand.isEnabled = false
            commandCenter.previousTrackCommand.isEnabled = false
            commandCenter.changePlaybackRateCommand.isEnabled = false
            commandCenter.skipForwardCommand.isEnabled = false
            commandCenter.skipBackwardCommand.isEnabled = false
            commandCenter.ratingCommand.isEnabled = false
            commandCenter.likeCommand.isEnabled = false
            commandCenter.dislikeCommand.isEnabled = false
            commandCenter.bookmarkCommand.isEnabled = false
            commandCenter.changeRepeatModeCommand.isEnabled = false
            commandCenter.changeShuffleModeCommand.isEnabled = false
            
            // only available in iOS 9
            if #available(iOS 9.0, *) {
                commandCenter.enableLanguageOptionCommand.isEnabled = false
                commandCenter.disableLanguageOptionCommand.isEnabled = false
            }
            
            // control center play button callback
            commandCenter.playCommand.addTarget { (MPRemoteCommandEvent) -> MPRemoteCommandHandlerStatus in
                print("command center play command...")
                _ = self.play()
                return .success
            }
            
            // control center pause button callback
            commandCenter.pauseCommand.addTarget { (MPRemoteCommandEvent) -> MPRemoteCommandHandlerStatus in
                print("command center pause command...")
                _ = self.pause()
                return .success
            }
            
            // control center stop button callback
            commandCenter.stopCommand.addTarget { (MPRemoteCommandEvent) -> MPRemoteCommandHandlerStatus in
                print("command center stop command...")
                _ = self.stop()
                return .success
            }
            
            // create audio session for background playback and control center callbacks.
            let audioSession = AVAudioSession.sharedInstance()
            
            if #available(iOS 10.0, *) {
                try audioSession.setCategory(.playback, mode: .default, options: .defaultToSpeaker)
                try audioSession.overrideOutputAudioPort(.speaker)
                try audioSession.setActive(true)
            }
            
            UIApplication.shared.beginReceivingRemoteControlEvents()
        } catch {
            print("Something went wrong ! \(error)")
        }
    }
}

