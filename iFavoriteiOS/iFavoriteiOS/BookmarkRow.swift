//
//  BookmarkRow.swift
//  iFavoriteiOS
//
//  Created by 熊龙镇 on 2025/9/9.
//
import SwiftUI

// MARK: - 书签行视图
struct BookmarkRow: View {
    let bookmark: Bookmark
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(bookmark.title)
                .font(.headline)
            
            if !bookmark.description.isEmpty {
                Text(bookmark.description)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .lineLimit(2)
            }
            
            HStack {
                if let cover = bookmark.cover, !cover.isEmpty {
                    // 这里可以使用AsyncImage加载封面图片
                    Image(systemName: "photo")
                        .foregroundColor(.gray)
                }
                
                Text(bookmark.link)
                    .font(.caption)
                    .foregroundColor(.blue)
                    .lineLimit(1)
                
                Spacer()
                
                if let filepath = bookmark.filepath, !filepath.isEmpty {
                    Image(systemName: "paperclip")
                        .foregroundColor(.gray)
                }
            }
        }
        .padding(.vertical, 8)
    }
}
