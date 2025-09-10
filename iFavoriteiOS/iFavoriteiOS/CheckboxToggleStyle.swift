//
//  CheckboxToggleStyle.swift
//  iFavoriteiOS
//
//  Created by 熊龙镇 on 2025/9/10.
//
import SwiftUI

// 自定义复选框样式
struct CheckboxToggleStyle: ToggleStyle {
    func makeBody(configuration: Configuration) -> some View {
        HStack {
            // 切换按钮的视图
            Image(systemName: configuration.isOn ? "checkmark.square.fill" : "square")
                .resizable()
                .frame(width: 24, height: 24)
                .foregroundColor(configuration.isOn ? .blue : .gray) // 选中时显示蓝色，否则显示灰色
                .onTapGesture {
                    configuration.isOn.toggle() // 点击时切换状态
                }
            // 标签视图
            configuration.label
        }
    }
}
