package com.iwakura.an9hack

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class TabPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> ControlTab1()
        1 -> ControlTab2()
        2 -> ControlTab3()
        else -> throw IllegalStateException("Unexpected position $position")
    }

    override fun getItemCount(): Int = 3
}


