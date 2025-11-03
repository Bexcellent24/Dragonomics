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
import com.TheBudgeteers.dragonomics.data.NestLayoutType
import com.TheBudgeteers.dragonomics.data.NestType
import com.TheBudgeteers.dragonomics.data.SessionStore
import com.TheBudgeteers.dragonomics.ui.adapters.NestAdapter
import com.TheBudgeteers.dragonomics.utils.RepositoryProvider
import com.TheBudgeteers.dragonomics.viewmodel.HistoryViewModel
import com.TheBudgeteers.dragonomics.viewmodel.factories.HistoryViewModelFactory
import com.TheBudgeteers.dragonomics.viewmodel.NestViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

// Fragment for displaying nests in different layouts (Grid, List, or History).
class NestFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private var adapter: NestAdapter? = null
    private lateinit var nestViewModel: NestViewModel
    private lateinit var sessionStore: SessionStore

    private lateinit var layoutType: NestLayoutType
    private lateinit var nestType: NestType

    companion object {
        private const val ARG_NEST_TYPE = "nest_type"
        private const val ARG_LAYOUT_TYPE = "layout_type"

        // Create a new instance of NestFragment with the specified nest type and layout type.
        fun newInstance(nestType: NestType, layoutType: NestLayoutType): NestFragment {
            val fragment = NestFragment()
            val args = Bundle()
            args.putString(ARG_NEST_TYPE, nestType.name)
            args.putString(ARG_LAYOUT_TYPE, layoutType.name)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve nest type and layout type from arguments or set defaults
        nestType = arguments?.getString(ARG_NEST_TYPE)?.let { NestType.valueOf(it) }
            ?: NestType.EXPENSE
        layoutType = arguments?.getString(ARG_LAYOUT_TYPE)?.let { NestLayoutType.valueOf(it) }
            ?: NestLayoutType.GRID
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

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
        val repository = RepositoryProvider.getRepository(requireContext())
        sessionStore = SessionStore(requireContext())
        nestViewModel = NestViewModel(repository)

        // Launch coroutine to setup adapter once userId is available
        viewLifecycleOwner.lifecycleScope.launch {
            // Get userId as String (Firebase UID)
            val userId = sessionStore.userId.firstOrNull()

            if (userId == null) {
                Toast.makeText(requireContext(), "No user logged in", Toast.LENGTH_SHORT).show()
                return@launch
            }

            adapter = createAdapter(userId)
            recyclerView.adapter = adapter

            // Observe nests of this type for the given user and update adapter when data changes
            repository.getReactiveNestsFlowByType(userId, nestType).collect { nests ->
                adapter?.setNests(nests)
            }
        }

        // Listen for new nest creation events
        parentFragmentManager.setFragmentResultListener("new_nest_created", viewLifecycleOwner) { _, _ ->
        }
    }

    // Create the NestAdapter for the given user and layout type.
    private fun createAdapter(userId: String): NestAdapter {
        return if (layoutType == NestLayoutType.HISTORY) {
            val repository = RepositoryProvider.getRepository(requireContext())
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
                Toast.makeText(requireContext(), "Clicked ${clickedNest.name}", Toast.LENGTH_SHORT)
                    .show()
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
                Toast.makeText(requireContext(), "Clicked ${clickedNest.name}", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}