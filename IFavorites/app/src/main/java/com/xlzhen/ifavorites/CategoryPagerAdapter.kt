// File: app/src/main/java/com/example/cloudfavorites/CategoryPagerAdapter.kt
package com.xlzhen.ifavorites

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class CategoryPagerAdapter(activity: FragmentActivity, private val mainFolders: List<Folder>) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = mainFolders.size

    override fun createFragment(position: Int): Fragment {
        return CategoryFragment.newInstance(mainFolders[position].id)
    }
}