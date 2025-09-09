//
//  FolderRow.swift
//  iFavoriteiOS
//
//  Created by 熊龙镇 on 2025/9/9.
//
import SwiftUI

// MARK: - 文件夹行视图
struct FolderRow: View {
    let folder: Folder
    
    var body: some View {
        HStack {
            Image(systemName: "folder")
                .foregroundColor(.blue)
                .font(.title2)
            Text(folder.name)
                .font(.headline)
            Spacer()
        }
        .padding(.vertical, 8)
    }
}
