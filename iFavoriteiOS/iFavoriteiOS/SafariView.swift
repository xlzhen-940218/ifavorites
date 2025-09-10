//
//  SafariView.swift
//  iFavoriteiOS
//
//  Created by 熊龙镇 on 2025/9/10.
//


import SwiftUI
import SafariServices

// MARK: - SFSafariViewController 的 SwiftUI 包装器
struct SafariView: UIViewControllerRepresentable {
    let url: URL
    
    func makeUIViewController(context: Context) -> SFSafariViewController {
        let safariVC = SFSafariViewController(url: url)
        return safariVC
    }
    
    func updateUIViewController(_ uiViewController: SFSafariViewController, context: Context) {
        // 无需更新
    }
}
