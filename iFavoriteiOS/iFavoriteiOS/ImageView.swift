//
//  ImageView.swift
//  iFavoriteiOS
//
//  Created by 熊龙镇 on 2025/9/10.
//


import SwiftUI

struct ImageView: View {
    let filePath: String
    
    // 假设 APIConfig.baseURL 包含你的服务器地址
    private var imageURL: URL? {
        URL(string: "\(APIConfig.baseURL)/\(filePath)")
    }
    
    var body: some View {
        VStack {
            if let url = imageURL {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .empty:
                        ProgressView("加载中...")
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFit()
                            .padding() // 增加一些边距
                    case .failure:
                        Image(systemName: "exclamationmark.triangle.fill")
                            .foregroundColor(.red)
                        Text("图片加载失败")
                            .foregroundColor(.secondary)
                    @unknown default:
                        EmptyView()
                    }
                }
            } else {
                Text("无效的图片链接")
                    .foregroundColor(.red)
            }
        }
    }
}
