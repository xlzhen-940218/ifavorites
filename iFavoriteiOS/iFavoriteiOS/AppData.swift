//
//  AppData.swift
//  iFavoriteiOS
//
//  Created by 熊龙镇 on 2025/9/9.
//
import SwiftUI

// MARK: - 数据模型
class AppData: ObservableObject {
    @Published var mainFolders: [Folder] = []
    @Published var currentSubFolders: [Folder] = []
    @Published var currentBookmarks: [Bookmark] = []
    @Published var isLoading = false
    @Published var errorMessage = ""
    @Published var downloadedCount = 0
    @Published var taskCount = 0
    @Published var downloading = false
    @Published var dowloadingMessage = ""
    // 加载主文件夹
    func loadMainFolders() {
        isLoading = true
        APIService.shared.getMainFolders { result in
            DispatchQueue.main.async {
                self.isLoading = false
                switch result {
                case .success(let response):
                    self.mainFolders = response.folders
                case .failure(let error):
                    self.handleError(error)
                }
            }
        }
    }
    
    // 加载子文件夹
    func loadSubFolders(parentId: String) {
        isLoading = true
        APIService.shared.getSubFolders(parentId: parentId) { result in
            DispatchQueue.main.async {
                self.isLoading = false
                switch result {
                case .success(let response):
                    self.currentSubFolders = response.folders
                case .failure(let error):
                    self.handleError(error)
                }
            }
        }
    }
    
    // 加载书签
    func loadBookmarks(folderId: String) {
        isLoading = true
        APIService.shared.getBookmarks(folderId: folderId) { result in
            DispatchQueue.main.async {
                self.isLoading = false
                switch result {
                case .success(let response):
                    self.currentBookmarks = response.bookmarks
                case .failure(let error):
                    self.handleError(error)
                }
            }
        }
    }
    
    func getTaskProgress(taskIds: [String], taskIndex: Int, completion: @escaping (Bool) -> Void){
        
        APIService.shared.getTaskProgress(taskId: taskIds[taskIndex]) { result in
            DispatchQueue.main.async {
                switch result {
                case .success(let response):
                    if response.success {
                        if response.status == "COMPLETED" && taskIndex == taskIds.count - 1{
                            self.downloading = false
                            completion(true)
                        }else{
                            var index = taskIndex
                            if response.status == "COMPLETED"{
                                index += 1
                                self.downloadedCount = index + 1
                            }
                            self.dowloadingMessage = response.message
                            sleep(2)
                            self.getTaskProgress(taskIds: taskIds,taskIndex: index,completion: completion)
                        }
                    }else {
                        self.errorMessage = "获取进度失败！"
                        completion(false)
                    }
                case .failure(let error):
                    self.handleError(error)
                    completion(false)
                }
                
            }
            
        }
    }
    
    func submitDownloadTask(link: String, folderId: String, isDownload: Bool, completion: @escaping (Bool) -> Void){
        isLoading = true
        APIService.shared.submitDownloadTask(link: link, folderId: folderId, isDownload: isDownload) { result in
            DispatchQueue.main.async {
                self.isLoading = false
                switch result {
                case .success(let response):
                        if response.success {
                            
                            self.downloading = true
                            var taskIds = [String]()
                            if response.task_id != nil {
                                taskIds.append(response.task_id!)
                            }else if response.task_ids != nil {
                                taskIds.append(contentsOf: response.task_ids!)
                            }
                            if taskIds.count > 0{
                                self.taskCount = taskIds.count
                                self.getTaskProgress(taskIds: taskIds,taskIndex: 0, completion: completion)
                            }
                            //completion(true)
                        }else {
                            self.errorMessage = "添加收藏链接失败！"
                            completion(false)
                        }
                case .failure(let error):
                    self.handleError(error)
                    completion(false)
                
                }
                
            }
            
        }
    }
    
    // 创建新文件夹
    func createFolder(name: String, parentId: String, completion: @escaping (Bool) -> Void) {
        isLoading = true
        APIService.shared.createFolder(name: name, parentId: parentId) { result in
            DispatchQueue.main.async {
                self.isLoading = false
                switch result {
                case .success(let response):
                    if response.success {
                        // 刷新文件夹列表
                        if parentId.isEmpty {
                            self.loadMainFolders()
                        } else {
                            self.loadSubFolders(parentId: parentId)
                        }
                        completion(true)
                    } else {
                        self.errorMessage = "创建文件夹失败"
                        completion(false)
                    }
                case .failure(let error):
                    self.handleError(error)
                    completion(false)
                }
            }
        }
    }
    
    // 错误处理
    private func handleError(_ error: NetworkError) {
        switch error {
        case .badURL:
            errorMessage = "服务器连接失败"
        case .requestFailed:
            errorMessage = "网络请求失败，请检查网络连接"
        case .decodingFailed:
            errorMessage = "数据解析错误"
        case .apiError(let message):
            errorMessage = message
        case .invalidResponse:
            errorMessage = "服务器返回无效响应"
        case .unauthorized:
            errorMessage = "认证失败，请重新登录"
        }
    }
}
