import SwiftUI

// 认证视图，专注于UI展示和用户交互
struct AuthView: View {
    @EnvironmentObject private var authService: AuthService
    @State private var isLoginMode = true
    @State private var email = ""
    @State private var password = ""
    @State private var confirmPassword = ""
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 16) {
                    modePicker
                    formInputs
                    submitButton
                }
                .padding(.vertical)
            }
            .navigationTitle(isLoginMode ? "登录" : "注册")
            .background(Color(.systemGroupedBackground).ignoresSafeArea())
            .alert("错误", isPresented: $authService.showError) {
                Button("确定", role: .cancel) { }
            } message: {
                Text(authService.errorMessage)
            }
//            .fullScreenCover(isPresented: $authService.isLoggedIn) {
//                MainContentView()
//                    .environmentObject(authService)
//            }
        }
    }
    
    // 模式选择器
    private var modePicker: some View {
        Picker("", selection: $isLoginMode) {
            Text("登录").tag(true)
            Text("注册").tag(false)
        }
        .pickerStyle(SegmentedPickerStyle())
        .padding()
    }
    
    // 表单输入
    private var formInputs: some View {
        Group {
            TextField("邮箱", text: $email)
                .keyboardType(.emailAddress)
                .autocapitalization(.none)
            
            SecureField("密码", text: $password)
            
            if !isLoginMode {
                SecureField("确认密码", text: $confirmPassword)
            }
        }
        .padding(12)
        .background(Color.white)
        .cornerRadius(8)
        .padding(.horizontal)
    }
    
    // 提交按钮
    private var submitButton: some View {
        Button {
            handleAction()
        } label: {
            HStack {
                if authService.isLoading {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                }
                Text(isLoginMode ? "登录" : "注册")
                    .fontWeight(.semibold)
            }
            .foregroundColor(.white)
            .frame(maxWidth: .infinity)
            .padding()
            .background(Color.blue)
            .cornerRadius(8)
            .padding(.horizontal)
        }
        .disabled(authService.isLoading)
    }
    
    // 处理用户操作
    private func handleAction() {
        if isLoginMode {
            authService.login(email: email, password: password)
        } else {
            authService.register(email: email, password: password, confirmPassword: confirmPassword)
        }
    }
}

// MARK: - 预览
struct AuthView_Previews: PreviewProvider {
    static var previews: some View {
        AuthView()
            .environmentObject(AuthService()) 
    }
}
