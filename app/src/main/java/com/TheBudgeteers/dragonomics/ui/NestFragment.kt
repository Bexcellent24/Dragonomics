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
import com.TheBudgeteers.dragonomics.models.NestType
import com.TheBudgeteers.dragonomics.viewmodel.NestViewModel
import com.TheBudgeteers.dragonomics.viewmodel.NestViewModelFactory
import kotlinx.coroutines.launch

class NestFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private var adapter: NestAdapter? = null
    private lateinit var nestViewModel: NestViewModel
    private lateinit var repository: Repository

    private lateinit var layoutType: NestLayoutType
    private lateinit var nestType: NestType

    companion object {
        private const val ARG_NEST_TYPE = "nest_type"
        private const val ARG_LAYOUT_TYPE = "layout_type"

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
        nestType = arguments?.getString(ARG_NEST_TYPE)?.let { NestType.valueOf(it) } ?: NestType.EXPENSE
        layoutType = arguments?.getString(ARG_LAYOUT_TYPE)?.let { NestLayoutType.valueOf(it) } ?: NestLayoutType.GRID
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_nest_list, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewNests)

        recyclerView.layoutManager = when (layoutType) {
            NestLayoutType.GRID -> GridLayoutManager(requireContext(), 2)
            NestLayoutType.LIST -> LinearLayoutManager(requireContext())
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = Repository(AppDatabase.getDatabase(requireContext()))
        val factory = NestViewModelFactory(repository)
        nestViewModel = ViewModelProvider(this, factory).get(NestViewModel::class.java)

        // Create adapter once
        adapter = NestAdapter(nestViewModel, layoutType, viewLifecycleOwner.lifecycleScope) { clickedNest ->
            Toast.makeText(requireContext(), "Clicked ${clickedNest.name}", Toast.LENGTH_SHORT).show()
        }
        recyclerView.adapter = adapter

        // Collect flow and update adapter's data
        viewLifecycleOwner.lifecycleScope.launch {
            repository.getReactiveNestsFlowByType(nestType).collect { nests ->
                adapter?.setNests(nests)
            }
        }

        parentFragmentManager.setFragmentResultListener("new_nest_created", viewLifecycleOwner) { _, _ ->
            // No manual refresh needed, flow will emit automatically if DB changes
        }
    }

}

