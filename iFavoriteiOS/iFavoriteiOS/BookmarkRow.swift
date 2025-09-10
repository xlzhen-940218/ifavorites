import SwiftUI

// MARK: - 书签行视图
struct BookmarkRow: View {
    let bookmark: Bookmark
    @State private var showingPlayerView: Bool = false
    @State private var playerType: PlayerType?
     
    // 让 PlayerType 遵循 Identifiable 协议
    enum PlayerType: Identifiable {
        case video
        case audio
        case image
        case link
        
        var id: String {
            switch self {
            case .video: return "video"
            case .audio: return "audio"
            case .image: return "image"
            case .link: return "link"
            }
        }
    }
    
    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            // 左侧：封面图
            BookmarkCoverImage(coverURLString: bookmark.cover)
                .frame(width: 80, height: 80)
                .cornerRadius(8)
            
            // 右侧：标题、描述和链接
            VStack(alignment: .leading, spacing: 8) {
                // 标题
                Text(bookmark.title)
                    .font(.headline)
                    .lineLimit(1)
                
                // 描述
                if !bookmark.description.isEmpty {
                    Text(bookmark.description)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .lineLimit(2)
                }
                
                Spacer() // 将下面的链接和附件图标推到底部
                
                // 链接和附件
                HStack(spacing: 4) {
                    Text(bookmark.link)
                        .font(.caption)
                        .foregroundColor(.blue)
                        .lineLimit(1)
                    
                    if let filepath = bookmark.filepath, !filepath.isEmpty {
                        Image(systemName: "paperclip")
                            .foregroundColor(.gray)
                            .font(.caption)
                    }
                }
            }
        }
        .padding(.vertical, 8)
        .onTapGesture {
               handleTapAction()
           }
           .sheet(item: $playerType) { type in
               switch type {
               case .video:
                   VideoPlayerView(filePath: bookmark.filepath!)
               case .audio:
                   AudioPlayerView(filePath: bookmark.filepath!)
               case .image:
                   ImageView(filePath: bookmark.filepath!)
               case .link:
                   SafariView(url: URL(string: "\(APIConfig.baseURL)/\(bookmark.link)")!)
               }
           }
    }
    
    // MARK: - 点击处理逻辑
        private func handleTapAction() {
            guard let filepath = bookmark.filepath, !filepath.isEmpty else {
                  return
              }
              
              let fileExtension = (filepath as NSString).pathExtension.lowercased()
              
              switch fileExtension {
              case "mp4", "mov", "mkv", "avi":
                  playerType = .video
              case "mp3", "wav", "aac":
                  playerType = .audio
              case "jpg", "jpeg", "png", "gif":
                  playerType = .image
              default:
                  playerType = .link
              }
              // 注意：这里不需要再设置 showingPlayerView = true
        }
}



// MARK: - 独立封面图加载视图
struct BookmarkCoverImage: View {
    let coverURLString: String?
    
    var body: some View {
        if let coverURLString = coverURLString, !coverURLString.isEmpty,
           let coverURL = URL(string: "\(APIConfig.baseURL)/\(coverURLString)") {
            AsyncImage(url: coverURL) { phase in
                switch phase {
                case .empty:
                    ProgressView()
                case .success(let image):
                    image
                        .resizable()
                        .scaledToFill() // 使用 scaledToFill 确保图片填满容器
                case .failure:
                    // 加载失败或 URL 无效时显示默认图标
                    Image(systemName: "photo")
                        .resizable()
                        .scaledToFit()
                        .foregroundColor(.gray)
                @unknown default:
                    EmptyView()
                }
            }
        } else {
            // cover 为 nil 或为空时显示默认图标
            Image(systemName: "photo")
                .resizable()
                .scaledToFit()
                .foregroundColor(.gray)
        }
    }
}
