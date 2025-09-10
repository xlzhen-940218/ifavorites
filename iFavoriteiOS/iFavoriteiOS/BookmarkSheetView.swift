//
//  BookmarkSheetView.swift
//  iFavoriteiOS
//
//  Created by 熊龙镇 on 2025/9/10.
//
import SwiftUI

struct BookmarkSheetView: View {
    @Binding var showingAddBookmark: Bool
    @State private var newFavoriteUrl: String = ""
    @State private var downloadResource: Bool = false
    // 定义一个接受一个字符串和一个布尔值的闭包
     var onConfirm: (String, Bool) -> Void

    var body: some View {
        // 使用 NavigationStack 或其他容器来包裹你的内容
        VStack(spacing: 20) {
            Text("添加收藏")
                .font(.headline)

            // 输入框
            TextField("收藏链接", text: $newFavoriteUrl)
                .textFieldStyle(RoundedBorderTextFieldStyle())

            // 复选框
            Toggle(isOn: $downloadResource) {
                Text("是否下载资源？")
            }
            .toggleStyle(CheckboxToggleStyle())
            
            // 按钮
            HStack(spacing: 20) {
                Button("取消") {
                    showingAddBookmark = false
                    // 清空输入框和复选框状态
                    newFavoriteUrl = ""
                    downloadResource = false
                }
                .buttonStyle(.bordered)

                Button("添加") {
                    // 这里执行你的添加逻辑
                    if !newFavoriteUrl.isEmpty {
                        
                        onConfirm(newFavoriteUrl, downloadResource)
                        showingAddBookmark = false
                    }
                }
                .buttonStyle(.borderedProminent)
            }
            .padding(.top)
        }
        .padding()
    }
}
