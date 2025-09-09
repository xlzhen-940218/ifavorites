//
//  APIConfig.swift
//  iFavoriteiOS
//
//  Created by 熊龙镇 on 2025/9/9.
//


import Foundation

// MARK: - API Service Configuration
struct APIConfig {
    // ⚠️ 请将这里替换为你的服务器基地址
    static let baseURL = "http://192.168.2.103:8090"
}

// MARK: - Network Error Enum
enum NetworkError: Error {
    case badURL(String)
    case requestFailed(Error?)
    case decodingFailed(Error)
    case apiError(String)
    case invalidResponse
    case unauthorized
}

// MARK: - API Service
class APIService {

    static let shared = APIService()
    private var currentUserID: String? = "8f3e0018-b6a6-4565-b02d-7ded5cdfec83" // 用于存储登录后获取的 user_id

    private init() {}

    // 设置和清除用户 ID (登录/登出时调用)
    func setUserID(_ id: String) {
        self.currentUserID = id
    }

    func clearUserID() {
        self.currentUserID = nil
    }

    // MARK: - 1. User Authentication

    /// 1.1 用户注册
    func register(email: String, password: String, completion: @escaping (Result<RegisterResponse, NetworkError>) -> Void) {
        let endpoint = "/register"
        let parameters = ["email": email, "password": password]
        
        performRequest(endpoint: endpoint, method: "POST", body: parameters, completion: completion)
    }

    /// 1.2 用户登录
    func login(email: String, password: String, completion: @escaping (Result<LoginResponse, NetworkError>) -> Void) {
        let endpoint = "/login"
        let parameters = ["email": email, "password": password]
        
        performRequest(endpoint: endpoint, method: "POST", body: parameters) { (result: Result<LoginResponse, NetworkError>) in
            if case .success(let response) = result {
                self.setUserID(response.user_id) // 登录成功后自动保存 user_id
            }
            completion(result)
        }
    }

    // MARK: - 2. Data Retrieval

    /// 2.1 获取主文件夹
    func getMainFolders(completion: @escaping (Result<FoldersResponse, NetworkError>) -> Void) {
        let endpoint = "/get_main_folders"
        performRequest(endpoint: endpoint, method: "GET", isAuthenticated: true, completion: completion)
    }

    /// 2.2 获取子文件夹
    func getSubFolders(parentId: String, completion: @escaping (Result<FoldersResponse, NetworkError>) -> Void) {
        let endpoint = "/get_sub_folders/\(parentId)"
        performRequest(endpoint: endpoint, method: "GET", isAuthenticated: true, completion: completion)
    }

    /// 2.3 获取文件夹内的书签
    func getBookmarks(folderId: String, completion: @escaping (Result<BookmarksResponse, NetworkError>) -> Void) {
        let endpoint = "/get_bookmarks/\(folderId)"
        performRequest(endpoint: endpoint, method: "GET", isAuthenticated: true, completion: completion)
    }
    
    // MARK: - 3. Data Creation & Modification

    /// 3.1 创建子文件夹
    func createFolder(name: String, parentId: String, completion: @escaping (Result<CreateFolderResponse, NetworkError>) -> Void) {
        let endpoint = "/create_folder"
        let parameters = ["name": name, "parent_id": parentId]
        performRequest(endpoint: endpoint, method: "POST", isAuthenticated: true, body: parameters, completion: completion)
    }

    /// 3.2 手动添加书签
    func addBookmark(title: String, description: String, folderId: String, link: String, coverId: String, fileId: String?, completion: @escaping (Result<AddBookmarkResponse, NetworkError>) -> Void) {
        let endpoint = "/add_bookmark"
        var parameters: [String: Any] = [
            "title": title,
            "description": description,
            "folder_id": folderId,
            "link": link,
            "cover": coverId
        ]
        if let fileId = fileId {
            parameters["file_id"] = fileId
        }
        performRequest(endpoint: endpoint, method: "POST", isAuthenticated: true, body: parameters, completion: completion)
    }

    // MARK: - 4. File Uploads

    /// 4.1 上传文件
    func uploadFile(fileData: Data, fileName: String, mimeType: String, completion: @escaping (Result<UploadResponse, NetworkError>) -> Void) {
        let endpoint = "/upload_file"
        performUpload(endpoint: endpoint, fileData: fileData, parameterName: "file", fileName: fileName, mimeType: mimeType, completion: completion)
    }

    /// 4.2 上传封面
    func uploadCover(imageData: Data, fileName: String, mimeType: String, completion: @escaping (Result<UploadResponse, NetworkError>) -> Void) {
        let endpoint = "/upload_cover"
        performUpload(endpoint: endpoint, fileData: imageData, parameterName: "file", fileName: fileName, mimeType: mimeType, completion: completion)
    }
    
    // MARK: - 5. Background Task Management

    /// 5.1 提交下载任务
    func submitDownloadTask(link: String, folderId: String, isDownload: Bool?, completion: @escaping (Result<CrawlURLResponse, NetworkError>) -> Void) {
        let endpoint = "/craw_url"
        var parameters: [String: Any] = [
            "link": link,
            "folder_id": folderId,
        ]
        if let isDownload = isDownload {
            parameters["is_download"] = isDownload
        }
        performRequest(endpoint: endpoint, method: "POST", isAuthenticated: true, body: parameters, completion: completion)
    }

    /// 5.2 获取任务进度
    func getTaskProgress(taskId: String, completion: @escaping (Result<TaskProgress, NetworkError>) -> Void) {
        let endpoint = "/get_progress/\(taskId)"
        performRequest(endpoint: endpoint, method: "GET", isAuthenticated: true, completion: completion)
    }
    
    /// 5.3 查询未完成的任务
    func recoverTasks(folderId: String, completion: @escaping (Result<RecoverTasksResponse, NetworkError>) -> Void) {
        let endpoint = "/recover_tasks"
        let parameters = ["folder_id": folderId]
        performRequest(endpoint: endpoint, method: "POST", isAuthenticated: true, body: parameters, completion: completion)
    }

    // MARK: - Private Helper Methods

    /// 通用 JSON 请求处理器
    private func performRequest<T: Decodable>(endpoint: String, method: String, isAuthenticated: Bool = false, body: [String: Any]? = nil, completion: @escaping (Result<T, NetworkError>) -> Void) {
        guard let url = URL(string: APIConfig.baseURL + endpoint) else {
            completion(.failure(.badURL(endpoint)))
            return
        }

        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        if isAuthenticated {
            guard let userID = currentUserID else {
                completion(.failure(.unauthorized))
                return
            }
            request.setValue("Bearer \(userID)", forHTTPHeaderField: "Authorization")
        }

        if let body = body {
            do {
                request.httpBody = try JSONSerialization.data(withJSONObject: body, options: [])
            } catch {
                completion(.failure(.requestFailed(error)))
                return
            }
        }

        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error {
                completion(.failure(.requestFailed(error)))
                return
            }

            guard let data = data else {
                completion(.failure(.invalidResponse))
                return
            }

            // 首先解码通用响应结构
            do {
                let genericResponse = try JSONDecoder().decode(GenericResponse<T>.self, from: data)
                if genericResponse.success {
                    // 检查 data 字段是否存在
                    if let responseData = genericResponse.data {
                        completion(.success(responseData))
                    } else {
                        // 如果 data 为空，但 success 为 true，则可能是一个不需要返回数据的成功操作
                        // 我们可以尝试将整个响应体作为 T 解码
                        do {
                           let fullResponse = try JSONDecoder().decode(T.self, from: data)
                           completion(.success(fullResponse))
                        } catch {
                           completion(.failure(.decodingFailed(error)))
                        }
                    }
                } else {
                    completion(.failure(.apiError(genericResponse.message ?? "未知错误")))
                }
            } catch {
                completion(.failure(.decodingFailed(error)))
            }
        }.resume()
    }

    /// 通用文件上传处理器
    private func performUpload<T: Decodable>(endpoint: String, fileData: Data, parameterName: String, fileName: String, mimeType: String, completion: @escaping (Result<T, NetworkError>) -> Void) {
        guard let url = URL(string: APIConfig.baseURL + endpoint) else {
            completion(.failure(.badURL(endpoint)))
            return
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        
        guard let userID = currentUserID else {
            completion(.failure(.unauthorized))
            return
        }
        request.setValue("Bearer \(userID)", forHTTPHeaderField: "Authorization")

        let boundary = "Boundary-\(UUID().uuidString)"
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")

        var body = Data()
        body.append("--\(boundary)\r\n".data(using: .utf8)!)
        body.append("Content-Disposition: form-data; name=\"\(parameterName)\"; filename=\"\(fileName)\"\r\n".data(using: .utf8)!)
        body.append("Content-Type: \(mimeType)\r\n\r\n".data(using: .utf8)!)
        body.append(fileData)
        body.append("\r\n".data(using: .utf8)!)
        body.append("--\(boundary)--\r\n".data(using: .utf8)!)
        request.httpBody = body
        
        // 与 performRequest 相同的后续处理
        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error {
                completion(.failure(.requestFailed(error)))
                return
            }
            guard let data = data else {
                completion(.failure(.invalidResponse))
                return
            }
            do {
                let genericResponse = try JSONDecoder().decode(GenericResponse<T>.self, from: data)
                if genericResponse.success {
                    // 上传接口的 data 就是 T 本身
                    let result = try JSONDecoder().decode(T.self, from: data)
                    completion(.success(result))
                } else {
                    completion(.failure(.apiError(genericResponse.message ?? "上传失败")))
                }
            } catch {
                completion(.failure(.decodingFailed(error)))
            }
        }.resume()
    }
}


// MARK: - Data Models

// 通用响应模型
struct GenericResponse<T: Decodable>: Decodable {
    let success: Bool
    let message: String?
    let data: T?
}

// 1.1 用户注册
struct RegisterResponse: Codable {
    let success: Bool
    let user_id: String
}

// 1.2 用户登录
struct LoginResponse: Codable {
    let success: Bool
    let user_id: String
}

// 文件夹模型
struct Folder: Codable, Identifiable {
    let id: String
    let name: String
}

// 2.1 & 2.2 获取文件夹
struct FoldersResponse: Codable {
    let folders: [Folder]
}

// 书签模型
struct Bookmark: Codable, Identifiable {
    let id: String
    let title: String
    let description: String
    let link: String
    let cover: String?
    let filepath: String?
}

// 2.3 获取书签
struct BookmarksResponse: Codable {
    let bookmarks: [Bookmark]
}

// 3.1 创建子文件夹
struct CreateFolderResponse: Codable {
    let success: Bool
    let folder_id: String
}

// 3.2 手动添加书签
struct AddBookmarkResponse: Codable {
    let success: Bool
    let bookmark_id: String
}

// 4.1 & 4.2 文件/封面上传
struct UploadResponse: Codable {
    let success: Bool
    let file_id: String
    let file_path: String
}

// 5.1 提交下载任务
struct CrawlURLResponse: Codable {
    let success: Bool
    let message: String
    let task_id: String?      // 单个任务
    let task_ids: [String]?   // 播放列表
}

// 5.2 获取任务进度
struct TaskProgress: Codable {
    let success: Bool
    let task_id: String
    let status: String // PENDING, DOWNLOADING, COMPLETED, FAILED
    let progress: Int
    let message: String
}

// 5.3 查询未完成的任务
struct RecoverTasksResponse: Codable {
    let success: Bool
    let message: String
    let task_ids: [String]
}
