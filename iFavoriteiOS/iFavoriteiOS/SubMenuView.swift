//
//  SubMenuView.swift
//  iFavoriteiOS
//
//  Created by 熊龙镇 on 2025/9/9.
//
import SwiftUI

// MARK: - 子菜单视图
struct SubMenuView: View {
    let folder: Folder
    let isMainFolder: Bool
    @StateObject private var appData = AppData()
    @State private var showingAddFolder = false
    @State private var newFolderName = ""
    
    var body: some View {
        ZStack {
            if appData.isLoading {
                ProgressView("加载中...")
            } else if !isMainFolder {
                VStack {
                    if !appData.currentBookmarks.isEmpty {
                        BookmarksView(bookmarks: appData.currentBookmarks)
                    }
                }
            } else {
                List {
                    ForEach(appData.currentSubFolders) { subFolder in
                        NavigationLink(destination: SubMenuView(folder: subFolder, isMainFolder: false)) {
                            FolderRow(folder: subFolder)
                        }
                    }
                    
                    // 显示当前文件夹的书签
                    if !appData.currentBookmarks.isEmpty {
                        Section(header: Text("书签")) {
                            ForEach(appData.currentBookmarks) { bookmark in
                                BookmarkRow(bookmark: bookmark)
                            }
                        }
                    }
                }
                .listStyle(GroupedListStyle())
            }
        }
        .navigationTitle(folder.name)
        .toolbar {
            if isMainFolder {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: {
                        showingAddFolder = true
                    }) {
                        Image(systemName: "plus")
                    }
                }
            }else{
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: {
                        appData.loadBookmarks(folderId: folder.id)
                    }) {
                        Image(systemName: "bookmark")
                    }
                }
            }
        }
        .alert("添加子文件夹", isPresented: $showingAddFolder) {
            TextField("文件夹名称", text: $newFolderName)
            Button("取消", role: .cancel) {
                newFolderName = ""
            }
            Button("添加") {
                if !newFolderName.isEmpty {
                    appData.createFolder(name: newFolderName, parentId: folder.id) { success in
                        if success {
                            newFolderName = ""
                        }
                    }
                }
            }
        } message: {
            Text("请输入新子文件夹的名称")
        }
        .alert("错误", isPresented: .constant(!appData.errorMessage.isEmpty)) {
            Button("确定") {
                appData.errorMessage = ""
            }
        } message: {
            Text(appData.errorMessage)
        }
        .onAppear {
            if isMainFolder{
                
                appData.loadSubFolders(parentId: folder.id)
            }else{
                appData.loadBookmarks(folderId: folder.id)
            }
        }
    }
}
// MARK: - 预览
struct SubMenuView_Previews: PreviewProvider {
    static var previews: some View {
        SubMenuView(folder: Folder(id: "e49fc103-6de2-4aeb-96c6-57181b3bb71d", name: "Videos"), isMainFolder: true)
    }
}
