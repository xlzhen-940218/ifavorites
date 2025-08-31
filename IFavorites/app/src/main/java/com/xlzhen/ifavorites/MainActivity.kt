// File: app/src/main/java/com/example/cloudfavorites/MainActivity.kt
package com.xlzhen.ifavorites

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNavigation: BottomNavigationView
    private var mainFolders: List<Folder> = emptyList()
    private val categoryNames = listOf("视频", "音频", "图片", "文档", "压缩文件", "其他")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewPager = findViewById(R.id.view_pager)
        bottomNavigation = findViewById(R.id.bottom_navigation)

        setupBottomNavigation()
        loadMainFolders()
    }

    private fun setupBottomNavigation() {
        bottomNavigation.menu.add(0, 0, 0, "视频")
        bottomNavigation.menu.add(0, 1, 1, "音频")
        bottomNavigation.menu.add(0, 2, 2, "图片")
        bottomNavigation.menu.add(0, 3, 3, "文档")
        bottomNavigation.menu.add(0, 4, 4, "压缩文件")

        bottomNavigation.setOnItemSelectedListener { item ->
            viewPager.currentItem = item.itemId
            true
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                bottomNavigation.selectedItemId = position
            }
        })
    }

    private fun loadMainFolders() {
        val userId = getSharedPreferences("prefs", Context.MODE_PRIVATE).getString("user_id", "") ?: return
        val auth = "Bearer $userId"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                var response = RetrofitClient.instance.getMainFolders(auth)
                if (response.folders.isNullOrEmpty()) {
                    // Create if empty
                    RetrofitClient.instance.createMainFolders()
                    response = RetrofitClient.instance.getMainFolders(auth)
                }
                mainFolders = response.folders ?: emptyList()
                withContext(Dispatchers.Main) {
                    if (mainFolders.size >= 6) {
                        val adapter = CategoryPagerAdapter(this@MainActivity, mainFolders)
                        viewPager.adapter = adapter
                    } else {
                        Toast.makeText(this@MainActivity, "主文件夹加载失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}