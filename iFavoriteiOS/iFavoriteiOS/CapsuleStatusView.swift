//
//  CapsuleStatusView.swift
//  iFavoriteiOS
//
//  Created by 熊龙镇 on 2025/9/10.
//


import SwiftUI

struct CapsuleStatusView: View {
    var text: String
    var backgroundColor: Color = .blue
    var textColor: Color = .white

    var body: some View {
        ZStack {
            Capsule()
                .fill(backgroundColor)
            
            Text(text)
                .font(.system(size: 14, weight: .semibold, design: .rounded))
                .foregroundColor(textColor)
                .padding(.horizontal, 10)
                .padding(.vertical, 5)
        }
        .fixedSize() // 确保视图大小根据内容自适应
    }
}

// 预览
struct CapsuleStatusView_Previews: PreviewProvider {
    static var previews: some View {
        VStack(spacing: 20) {
            CapsuleStatusView(text: "Active", backgroundColor: .green)
        }
        .padding()
    }
}
