//
//  AudioPlayerView.swift
//  iFavoriteiOS
//
//  Created by 熊龙镇 on 2025/9/10.
//


import SwiftUI
import AVKit

struct AudioPlayerView: View {
    let filePath: String
    @State private var player: AVPlayer?
    @State private var isPlaying: Bool = false
    
    // 假设 APIConfig.baseURL 包含你的服务器地址
    private var audioURL: URL? {
        URL(string: "\(APIConfig.baseURL)/\(filePath)")
    }
    
    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "music.note")
                .resizable()
                .scaledToFit()
                .frame(width: 100, height: 100)
                .foregroundColor(.blue)
            
            Text("正在播放音频文件...")
                .font(.headline)
            
            // 播放/暂停按钮
            Button(action: togglePlayPause) {
                Image(systemName: isPlaying ? "pause.circle.fill" : "play.circle.fill")
                    .resizable()
                    .frame(width: 60, height: 60)
                    .foregroundColor(.blue)
            }
        }
        .onAppear {
            if let url = audioURL {
                self.player = AVPlayer(url: url)
                // 自动开始播放
                self.player?.play()
                self.isPlaying = true
            }
        }
        .onDisappear {
            // 视图消失时停止播放
            self.player?.pause()
            self.player = nil // 释放资源
        }
    }
    
    private func togglePlayPause() {
        if isPlaying {
            player?.pause()
        } else {
            player?.play()
        }
        isPlaying.toggle()
    }
}
