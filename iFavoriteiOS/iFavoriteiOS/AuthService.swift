//
//  AuthService.swift
//  iFavoriteiOS
//
//  Created by 熊龙镇 on 2025/9/9.
//


import Foundation
import Combine

// 认证服务类，处理所有认证相关的逻辑
class AuthService: ObservableObject {
    @Published var isLoggedIn = false
    @Published var isLoading = false
    @Published var errorMessage = ""
    @Published var showError = false
    
    init(){
        if let userId = UserDefaults.standard.string(forKey: "userId") {
            print("读取的用户ID: \(userId)")
            isLoggedIn = true
            APIService.shared.setUserID(userId)
        }
    }
    
    // 登录用户
    func login(email: String, password: String) {
        isLoading = true
        errorMessage = ""
        
        // 表单验证
        guard validateForm(email: email, password: password, isLogin: true) else {
            isLoading = false
            return
        }
        
        // API 调用
        APIService.shared.login(email: email, password: password) { [weak self] result in
            DispatchQueue.main.async {
                self?.isLoading = false
                switch result {
                case .success(let response):
                    if response.success {
                        UserDefaults.standard.set(response.user_id, forKey: "userId")

                        self?.isLoggedIn = true
                    } else {
                        self?.errorMessage = "登录失败"
                        self?.showError = true
                    }
                case .failure(let error):
                    self?.handleError(error)
                }
            }
        }
    }
    
    // 注册用户
    func register(email: String, password: String, confirmPassword: String) {
        isLoading = true
        errorMessage = ""
        
        // 表单验证
        guard validateForm(email: email, password: password, 
                          confirmPassword: confirmPassword, isLogin: false) else {
            isLoading = false
            return
        }
        
        // API 调用
        APIService.shared.register(email: email, password: password) { [weak self] result in
            DispatchQueue.main.async {
                self?.isLoading = false
                switch result {
                case .success(let response):
                    if response.success {
                        // 注册成功后自动登录
                        self?.login(email: email, password: password)
                    } else {
                        self?.errorMessage = "注册失败"
                        self?.showError = true
                    }
                case .failure(let error):
                    self?.handleError(error)
                }
            }
        }
    }
    
    // 表单验证
    private func validateForm(email: String, password: String, 
                             confirmPassword: String = "", isLogin: Bool) -> Bool {
        if email.isEmpty || password.isEmpty {
            errorMessage = "请填写所有必填字段"
            showError = true
            return false
        }
        
        if !isLogin && password != confirmPassword {
            errorMessage = "两次输入的密码不一致"
            showError = true
            return false
        }
        
        if !isValidEmail(email) {
            errorMessage = "请输入有效的邮箱地址"
            showError = true
            return false
        }
        
        return true
    }
    
    // 处理错误
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
        showError = true
    }
    
    // 验证邮箱格式
    private func isValidEmail(_ email: String) -> Bool {
        let emailRegEx = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}"
        let emailPred = NSPredicate(format:"SELF MATCHES %@", emailRegEx)
        return emailPred.evaluate(with: email)
    }
    
    // 登出
    func logout() {
        isLoggedIn = false
        // 这里可以添加清理token等操作
    }
}
