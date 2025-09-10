//
//  VideoPlayerView.swift
//  iFavoriteiOS
//
//  Created by 熊龙镇 on 2025/9/10.
//


import SwiftUI
import AVKit

struct VideoPlayerView: View {
    let filePath: String
    
    // 假设 APIConfig.baseURL 包含你的服务器地址
    private var videoURL: URL? {
        URL(string: "\(APIConfig.baseURL)/\(filePath)")
    }
    
    var body: some View {
        // 使用 guard let 确保 URL 有效
        guard let url = videoURL else {
            return AnyView(
                Text("无效的视频链接")
                    .foregroundColor(.red)
            )
        }
        
        return AnyView(
            VStack {
                // VideoPlayer 视图，直接传入 AVPlayer
                VideoPlayer(player: AVPlayer(url: url))
                    .edgesIgnoringSafeArea(.all)
            }
        )
    }
}
