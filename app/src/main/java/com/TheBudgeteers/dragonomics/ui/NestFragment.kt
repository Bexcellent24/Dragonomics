package com.TheBudgeteers.dragonomics.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.TheBudgeteers.dragonomics.R
import com.TheBudgeteers.dragonomics.data.AppDatabase
import com.TheBudgeteers.dragonomics.data.NestLayoutType
import com.TheBudgeteers.dragonomics.data.Repository
import com.TheBudgeteers.dragonomics.data.SessionStore
import com.TheBudgeteers.dragonomics.models.NestType
import com.TheBudgeteers.dragonomics.viewmodel.HistoryViewModel
import com.TheBudgeteers.dragonomics.viewmodel.HistoryViewModelFactory
import com.TheBudgeteers.dragonomics.viewmodel.NestViewModel
import com.TheBudgeteers.dragonomics.viewmodel.NestViewModelFactory
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

// Fragment for displaying nests in different layouts (Grid, List, or History).
// Sets up RecyclerView and NestAdapter, observes nest data, and handles user interaction.
// Supports nests filtered by type (Income or Expense) and displays them according to chosen layout.


class NestFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private var adapter: NestAdapter? = null
    private lateinit var nestViewModel: NestViewModel
    private lateinit var repository: Repository
    private lateinit var sessionStore: SessionStore

    private lateinit var layoutType: NestLayoutType
    private lateinit var nestType: NestType

    companion object {
        private const val ARG_NEST_TYPE = "nest_type"
        private const val ARG_LAYOUT_TYPE = "layout_type"

        // Factory pattern for fragment creation adapted from:
        // Android Developers guide to Fragments and Bundles

        // Create a new instance of NestFragment with the specified nest type and layout type
        fun newInstance(nestType: NestType, layoutType: NestLayoutType): NestFragment {
            val fragment = NestFragment()
            val args = Bundle()
            args.putString(ARG_NEST_TYPE, nestType.name)
            args.putString(ARG_LAYOUT_TYPE, layoutType.name)
            fragment.arguments = args
            return fragment
        }

        // end code attribution (Android Developers, 2020)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // begin code attribution
        // Argument retrieval pattern adapted from:
        // Android Developers guide to Fragment arguments

        // Retrieve nest type and layout type from arguments or set defaults
        nestType = arguments?.getString(ARG_NEST_TYPE)?.let { NestType.valueOf(it) }
            ?: NestType.EXPENSE
        layoutType = arguments?.getString(ARG_LAYOUT_TYPE)?.let { NestLayoutType.valueOf(it) }
            ?: NestLayoutType.GRID

        // end code attribution (Android Developers, 2020)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_nest_list, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewNests)

        // Set RecyclerView layout manager depending on layout type
        recyclerView.layoutManager = when (layoutType) {
            NestLayoutType.GRID -> GridLayoutManager(requireContext(), 2)
            NestLayoutType.LIST -> LinearLayoutManager(requireContext())
            NestLayoutType.HISTORY -> GridLayoutManager(requireContext(), 4)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialise repository and session store
        repository = Repository(AppDatabase.getDatabase(requireContext()))
        sessionStore = SessionStore(requireContext())

        val factory = NestViewModelFactory(repository)
        nestViewModel = ViewModelProvider(this, factory)[NestViewModel::class.java]

        // Launch coroutine to setup adapter once userId is available
        viewLifecycleOwner.lifecycleScope.launch {
            val userId = sessionStore.userId.firstOrNull()

            if (userId == null) {
                Toast.makeText(requireContext(), "No user logged in", Toast.LENGTH_SHORT).show()
                return@launch
            }

            adapter = createAdapter(userId)
            recyclerView.adapter = adapter

            // begin code attribution
            // Collecting Flow and updating UI adapted from:
            // Kotlin Coroutines Flow documentation

            // Observe nests of this type for the given user and update adapter when data changes
            repository.getReactiveNestsFlowByType(userId, nestType).collect { nests ->
                adapter?.setNests(nests)
            }

            // end code attribution (Kotlinlang.org, 2020)
        }

        // Listen for new nest creation events
        parentFragmentManager.setFragmentResultListener("new_nest_created", viewLifecycleOwner) { _, _ ->
            // No manual refresh needed as Flow emits changes automatically
        }
    }

    // Create the NestAdapter for the given user and layout type
    private fun createAdapter(userId: Long): NestAdapter {
        return if (layoutType == NestLayoutType.HISTORY) {
            val historyViewModel = ViewModelProvider(
                requireActivity(),
                HistoryViewModelFactory(repository, userId)
            )[HistoryViewModel::class.java]

            NestAdapter(
                nestViewModel,
                userId,
                layoutType,
                viewLifecycleOwner.lifecycleScope,
                historyViewModel.startDate,
                historyViewModel.endDate
            ) { clickedNest ->
                Toast.makeText(requireContext(), "Clicked ${clickedNest.name}", Toast.LENGTH_SHORT).show()
            }
        } else {
            NestAdapter(
                nestViewModel,
                userId,
                layoutType,
                viewLifecycleOwner.lifecycleScope,
                null,
                null
            ) { clickedNest ->
                Toast.makeText(requireContext(), "Clicked ${clickedNest.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

// reference list
// Android Developers, 2020. Fragments guide. [online] Available at: <https://developer.android.com/guide/fragments> [Accessed 1 October 2025]
// Android Developers, 2020. RecyclerView guide. [online] Available at: <https://developer.android.com/guide/topics/ui/layout/recyclerview> [Accessed 1 October 2025]
// Kotlinlang.org, 2020. Flow API guide. [online] Available at: <https://kotlinlang.org/docs/flow.html> [Accessed 1 October 2025]