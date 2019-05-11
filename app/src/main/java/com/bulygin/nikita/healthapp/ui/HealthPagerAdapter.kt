package com.bulygin.nikita.healthapp.ui

class HealthPagerAdapter(fragmentManager: androidx.fragment.app.FragmentManager,
                         private val fragments: Array<androidx.fragment.app.Fragment>,
                         private val titles: Array<String>) : androidx.fragment.app.FragmentPagerAdapter(fragmentManager) {

    override fun getItem(index: Int): androidx.fragment.app.Fragment {
        return fragments[index]
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return titles[position]
    }

    override fun getCount(): Int = fragments.size

}