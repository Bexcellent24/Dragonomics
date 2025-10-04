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

class ShopDialogFragment : DialogFragment() {

    private var _binding: DialogShopBinding? = null
    private val binding get() = _binding!!

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

        setupAdapter()
        setupTabs()
        setupCloseButton()
        observeViewModel()
    }

    private fun setupAdapter() {
        shopAdapter = ShopAdapter { item -> shopViewModel.handleItemAction(item) }

        binding.shopRecycler.apply {
            adapter = shopAdapter
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(requireContext(), 2)

            val space = (resources.displayMetrics.density * 8).toInt()
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
                ) {
                    outRect.set(space, space, space, space)
                }
            })
        }
    }

    private fun setupTabs() {
        val tabIcons = intArrayOf(
            R.drawable.palette_shop,
            R.drawable.horns_shop,
            R.drawable.wings_shop
        )

        if (binding.shopTabs.tabCount == 0) {
            tabIcons.forEach { icon ->
                binding.shopTabs.addTab(binding.shopTabs.newTab().setIcon(icon))
            }
        }

        tintShopTabs()

        binding.shopTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val shopTab = when (tab.position) {
                    0 -> ShopTab.PALETTE
                    1 -> ShopTab.HORNS
                    else -> ShopTab.WINGS
                }
                shopViewModel.setCurrentTab(shopTab)
                tintShopTabs()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                tintShopTabs()
            }

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        binding.shopTabs.selectTab(binding.shopTabs.getTabAt(0))
        resizeShopTabIcons()
    }

    private fun tintShopTabs() {
        val gold = ContextCompat.getColor(requireContext(), R.color.GoldenEmber)
        val dim = Color.parseColor("#546579")

        for (i in 0 until binding.shopTabs.tabCount) {
            val tab = binding.shopTabs.getTabAt(i)
            tab?.icon?.setTint(if (tab.isSelected) gold else dim)
        }
    }

    private fun setupCloseButton() {
        binding.shopCloseX.setOnClickListener {
            dismiss()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            shopViewModel.state.collect { state ->
                binding.shopCurrencyAmount.text = state.currency.toString()
                shopAdapter.submitList(shopViewModel.getCurrentItems())

                state.purchaseResult?.let { result ->
                    when (result) {
                        is PurchaseResult.Success -> {
                            Snackbar.make(binding.root, "Purchase successful!", Snackbar.LENGTH_SHORT).show()
                        }
                        is PurchaseResult.InsufficientFunds -> {
                            Snackbar.make(binding.root, "Not enough currency!", Snackbar.LENGTH_SHORT).show()
                        }
                    }
                    shopViewModel.clearPurchaseResult()
                }
            }
        }
    }

    private fun resizeShopTabIcons(sizeDp: Int = 80, tabHeightDp: Int = 80, horizPadDp: Int = 10) {
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