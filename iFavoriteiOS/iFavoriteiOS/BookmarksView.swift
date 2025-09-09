//
//  BookmarksView.swift
//  iFavoriteiOS
//
//  Created by 熊龙镇 on 2025/9/9.
//
import SwiftUI

// MARK: - 书签列表视图
struct BookmarksView: View {
    let bookmarks: [Bookmark]
    
    var body: some View {
        List(bookmarks) { bookmark in
            BookmarkRow(bookmark: bookmark)
        }
        .navigationTitle("书签")
        .listStyle(GroupedListStyle())
    }
}
