//
//  Item.swift
//  iFavoriteiOS
//
//  Created by 熊龙镇 on 2025/9/9.
//

import Foundation
import SwiftData

@Model
final class Item {
    var timestamp: Date
    
    init(timestamp: Date) {
        self.timestamp = timestamp
    }
}
