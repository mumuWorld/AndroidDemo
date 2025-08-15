package com.example.mmplayer.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mmplayer.adapter.FavoritesAdapter
import com.example.mmplayer.databinding.FragmentFavoritesBinding
import com.example.mmplayer.service.FavoriteManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var favoritesAdapter: FavoritesAdapter
    private lateinit var favoriteManager: FavoriteManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        favoriteManager = FavoriteManager(requireContext())
        setupRecyclerView()
        setupSwipeRefresh()
        loadFavorites()
    }

    private fun setupRecyclerView() {
        favoritesAdapter = FavoritesAdapter(
            onItemClick = { favoriteFolder ->
                // 打开文件浏览器并导航到收藏的文件夹
                val intent = Intent(requireContext(), FileBrowserActivity::class.java).apply {
                    putExtra("initial_path", favoriteFolder.path)
                }
                startActivity(intent)
            },
            onRemoveClick = { favoriteFolder ->
                showRemoveConfirmDialog(favoriteFolder)
            }
        )
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = favoritesAdapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadFavorites()
        }
    }

    private fun loadFavorites() {
        lifecycleScope.launch {
            binding.swipeRefresh.isRefreshing = true
            
            try {
                val favorites = favoriteManager.getFavorites()
                favoritesAdapter.submitList(favorites)
                
                // 显示空状态或列表
                if (favorites.isEmpty()) {
                    binding.llEmptyState.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                } else {
                    binding.llEmptyState.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun showRemoveConfirmDialog(favoriteFolder: com.example.mmplayer.model.FavoriteFolder) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("移除收藏")
            .setMessage("确定要从收藏中移除 \"${favoriteFolder.displayName}\" 吗？")
            .setPositiveButton("移除") { _, _ ->
                removeFavorite(favoriteFolder)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun removeFavorite(favoriteFolder: com.example.mmplayer.model.FavoriteFolder) {
        lifecycleScope.launch {
            val success = favoriteManager.removeFavorite(favoriteFolder.path)
            if (success) {
                loadFavorites()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadFavorites()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}