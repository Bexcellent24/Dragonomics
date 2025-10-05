package com.TheBudgeteers.dragonomics.ui

import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.TheBudgeteers.dragonomics.R
import com.TheBudgeteers.dragonomics.databinding.DialogShopBinding
import com.TheBudgeteers.dragonomics.models.ShopTab
import com.TheBudgeteers.dragonomics.viewmodel.PurchaseResult
import com.TheBudgeteers.dragonomics.viewmodel.ShopViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

// Full-screen dialog that displays the shop interface.
// Has tabs for different item categories (palette, horns, wings).
// Shows user's currency balance and lets them buy/equip items.
// Communicates with ShopViewModel to handle purchases and equipment changes.

class ShopDialogFragment : DialogFragment() {

    private var _binding: DialogShopBinding? = null
    private val binding get() = _binding!!

    // Using activityViewModels() so the ViewModel survives dialog dismissal
    private val shopViewModel: ShopViewModel by activityViewModels()
    private lateinit var shopAdapter: ShopAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CenteredDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogShopBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupTabs()
        setupCloseButton()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        // When user clicks an item, tell the ViewModel to handle it
        shopAdapter = ShopAdapter { item -> shopViewModel.handleItemAction(item) }

        binding.shopRecycler.apply {
            adapter = shopAdapter
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(requireContext(), 2)
            addItemDecoration(createSpacingDecoration())
        }
    }

    // begin code attribution
    // ItemDecoration for spacing adapted from:
    // Android Developers guide to RecyclerView ItemDecoration

    // Add spacing between grid items for better visual separation
    private fun createSpacingDecoration(): RecyclerView.ItemDecoration {
        val space = (resources.displayMetrics.density * 8).toInt()
        return object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
            ) {
                outRect.set(space, space, space, space)
            }
        }
    }
    // end code attribution (Android Developers, 2020)

    private fun setupTabs() {
        createTabs()
        setupTabListeners()
        selectDefaultTab()
    }

    private fun createTabs() {
        // Icons for each shop category
        val tabIcons = listOf(
            R.drawable.palette_shop,
            R.drawable.horns_shop,
            R.drawable.wings_shop
        )

        // Only create tabs once
        if (binding.shopTabs.tabCount == 0) {
            tabIcons.forEach { icon ->
                binding.shopTabs.addTab(binding.shopTabs.newTab().setIcon(icon))
            }
            resizeTabIcons()
        }

        updateTabColors()
    }

    private fun setupTabListeners() {
        binding.shopTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                // Map tab position to shop category
                val shopTab = when (tab.position) {
                    0 -> ShopTab.PALETTE
                    1 -> ShopTab.HORNS
                    else -> ShopTab.WINGS
                }
                shopViewModel.setCurrentTab(shopTab)
                updateTabColors()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                updateTabColors()
            }

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun selectDefaultTab() {
        binding.shopTabs.selectTab(binding.shopTabs.getTabAt(0))
    }

    // Colour selected tab gold, unselected tabs gray
    private fun updateTabColors() {
        val gold = ContextCompat.getColor(requireContext(), R.color.GoldenEmber)
        val dim = Color.parseColor("#546579")

        for (i in 0 until binding.shopTabs.tabCount) {
            val tab = binding.shopTabs.getTabAt(i)
            tab?.icon?.setTint(if (tab.isSelected) gold else dim)
        }
    }


    // begin code attribution
    // Tab icon resizing adapted from:
    // Stack Overflow answer on resizing TabLayout icons

    // Make tab icons bigger and easier to tap
    private fun resizeTabIcons(sizeDp: Int = 80, tabHeightDp: Int = 80, horizPadDp: Int = 10) {
        binding.shopTabs.post {
            val strip = binding.shopTabs.getChildAt(0) as? ViewGroup ?: return@post
            val iconSize = sizeDp.dp()
            val tabH = tabHeightDp.dp()
            val padH = horizPadDp.dp()

            for (i in 0 until strip.childCount) {
                val tabView = strip.getChildAt(i) as? ViewGroup ?: continue
                tabView.layoutParams = tabView.layoutParams.apply { height = tabH }
                tabView.setPadding(padH, 0, padH, 0)

                val iconView = tabView.findViewById<ImageView>(R.id.icon)
                iconView?.layoutParams = iconView?.layoutParams?.apply {
                    width = iconSize
                    height = iconSize
                }
                iconView?.scaleType = ImageView.ScaleType.CENTER_INSIDE
                iconView?.requestLayout()
            }
            strip.requestLayout()
        }
    }

    // end code attribution (Bill Shannon, 2018)

    private fun setupCloseButton() {
        binding.shopCloseX.setOnClickListener {
            dismiss()
        }
    }

    // Listen for changes from the ViewModel and update UI
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            shopViewModel.state.collect { state ->
                updateCurrencyDisplay(state.currency)
                updateShopItems()
                handlePurchaseResult(state.purchaseResult)
            }
        }
    }

    private fun updateCurrencyDisplay(currency: Int) {
        binding.shopCurrencyAmount.text = currency.toString()
    }

    private fun updateShopItems() {
        shopAdapter.submitList(shopViewModel.getCurrentItems())
    }

    private fun handlePurchaseResult(result: PurchaseResult?) {
        result?.let {
            val message = when (it) {
                is PurchaseResult.Success -> "Purchase successful!"
                is PurchaseResult.InsufficientFunds -> "Not enough currency!"
            }
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
            shopViewModel.clearPurchaseResult()
        }
    }

    // Helper to convert dp to pixels
    private fun Int.dp(): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        resources.displayMetrics
    ).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ShopDialogFragment"
    }
}


// Android Developers, 2020. ItemDecoration. [online] Available at: <https://developer.android.com/reference/androidx/recyclerview/widget/RecyclerView.ItemDecoration> [Accessed 5 October 2025]
// Bill Shannon, 2020. How to resize TabLayout icon (Stack Overflow). [online] Available at: <https://stackoverflow.com/questions/48411243/how-to-change-tab-icon-size-in-tablayout> [Accessed 5 October 2025]
