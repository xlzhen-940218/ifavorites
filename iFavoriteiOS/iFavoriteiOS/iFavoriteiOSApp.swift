//
//  iFavoriteiOSApp.swift
//  iFavoriteiOS
//
//  Created by 熊龙镇 on 2025/9/9.
//

import SwiftUI
import SwiftData

@main
struct iFavoriteiOSApp: App {
    @StateObject private var authService = AuthService()
    
    var sharedModelContainer: ModelContainer = {
        let schema = Schema(
            [
                Item.self,
            ]
        )
        let modelConfiguration = ModelConfiguration(
            schema: schema,
            isStoredInMemoryOnly: false
        )
        
        do {
            return try ModelContainer(
                for: schema,
                configurations: [modelConfiguration]
            )
        } catch {
            fatalError(
                "Could not create ModelContainer: \(error)"
            )
        }
    }()
    
    var body: some Scene {
        WindowGroup {
            if authService.isLoggedIn {
                MainContentView()
                    .environmentObject(
                        authService
                    )
                    .transition(
                        .scale
                    )
            } else {
                AuthView()
                    .environmentObject(
                        authService
                    )
            }
        }
        .modelContainer(
            sharedModelContainer
        )
    }
}
