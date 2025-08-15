package com.example.mmplayer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.mmplayer.databinding.ActivityMainNewBinding
import com.example.mmplayer.ui.FavoritesFragment
import com.example.mmplayer.ui.FileBrowserFragment

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainNewBinding
    private lateinit var fileBrowserFragment: FileBrowserFragment
    private lateinit var favoritesFragment: FavoritesFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainNewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        initializeFragments()
        setupBottomNavigation()
        
        // 默认显示文件浏览器
        showFragment(fileBrowserFragment)
        binding.bottomNavigation.selectedItemId = R.id.nav_file_browser
    }
    
    private fun initializeFragments() {
        fileBrowserFragment = FileBrowserFragment()
        favoritesFragment = FavoritesFragment()
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_file_browser -> {
                    showFragment(fileBrowserFragment)
                    true
                }
                R.id.nav_favorites -> {
                    showFragment(favoritesFragment)
                    true
                }
                else -> false
            }
        }
    }
    
    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.container, fragment)
            commit()
        }
    }
    
    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.container)
        if (currentFragment is FileBrowserFragment) {
            if (!currentFragment.onBackPressed()) {
                super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }
}