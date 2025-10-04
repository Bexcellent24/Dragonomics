package com.TheBudgeteers.dragonomics.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.TheBudgeteers.dragonomics.AchievementsAdapter
import com.TheBudgeteers.dragonomics.R
import com.TheBudgeteers.dragonomics.databinding.DialogAchievementsBinding
import com.TheBudgeteers.dragonomics.viewmodel.AchievementsViewModel
import kotlinx.coroutines.launch

class AchievementsDialogFragment : DialogFragment() {

    private var _binding: DialogAchievementsBinding? = null
    private val binding get() = _binding!!

    private val achievementsViewModel: AchievementsViewModel by activityViewModels()
    private lateinit var achievementsAdapter: AchievementsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CenteredDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAchievementsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        setupCloseButton()
        observeViewModel()
    }

    private fun setupAdapter() {
        achievementsAdapter = AchievementsAdapter(emptyList())
        binding.achRecycler.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = achievementsAdapter
        }
    }

    private fun setupCloseButton() {
        binding.closeX.setOnClickListener {
            dismiss()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            achievementsViewModel.achievements.collect { achievements ->
                achievementsAdapter.submit(achievements)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AchievementsDialogFragment"
    }
}