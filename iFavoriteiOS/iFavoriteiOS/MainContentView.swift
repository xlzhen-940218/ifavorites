//
//  MainContentView.swift
//  iFavoriteiOS
//
//  Created by 熊龙镇 on 2025/9/9.
//
import SwiftUI

// MARK: - 主内容视图
struct MainContentView: View {
    @StateObject private var appData = AppData()
    @State private var showingAddFolder = false
    @State private var newFolderName = ""
    
    var body: some View {
        NavigationView {
            ZStack {
                if appData.isLoading {
                    ProgressView("加载中...")
                } else if appData.mainFolders.isEmpty {
                    VStack {
                        Text("暂无文件夹")
                            .font(.title2)
                            .foregroundColor(.gray)
                        Button("刷新") {
                            appData.loadMainFolders()
                        }
                        .padding()
                    }
                } else {
                    List(appData.mainFolders) { folder in
                        NavigationLink(destination: SubMenuView(folder: folder, isMainFolder: true)) {
                            FolderRow(folder: folder)
                        }
                    }
                    .listStyle(PlainListStyle())
                }
            }
            .navigationTitle("主菜单")
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: {
                        appData.loadMainFolders()
                    }) {
                        Image(systemName: "arrow.clockwise")
                    }
                }
            }
            .alert("错误", isPresented: .constant(!appData.errorMessage.isEmpty)) {
                Button("确定") {
                    appData.errorMessage = ""
                }
            } message: {
                Text(appData.errorMessage)
            }
            .onAppear {
                appData.loadMainFolders()
            }
        }
    }
}

// MARK: - 预览
struct MainContentView_Previews: PreviewProvider {
    static var previews: some View {
        MainContentView()
    }
}
