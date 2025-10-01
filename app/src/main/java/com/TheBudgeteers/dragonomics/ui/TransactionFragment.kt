package com.TheBudgeteers.dragonomics.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.TheBudgeteers.dragonomics.R
import com.TheBudgeteers.dragonomics.data.RepositoryProvider
import com.TheBudgeteers.dragonomics.viewmodel.TransactionViewModel
import com.TheBudgeteers.dragonomics.viewmodel.RepositoryViewModelFactory
import kotlinx.coroutines.launch

// TransactionFragment
// This is the screen that shows a list of transactions.
// It uses a RecyclerView + Adapter for displaying data,
// and hooks into the TransactionViewModel to fetch the data from the repository.
class TransactionFragment : Fragment() {

    private lateinit var viewModel: TransactionViewModel   // our ViewModel for fetching transactions
    private lateinit var adapter: TransactionAdapter       // adapter that connects transactions -> RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // inflate the XML layout for this fragment
        val view = inflater.inflate(R.layout.fragment_transaction_list, container, false)

        // find the RecyclerView in the layout
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerTransactions)

        // set layout manager (vertical list)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // start adapter with an empty list
        adapter = TransactionAdapter(emptyList())

        // attach adapter to RecyclerView
        recyclerView.adapter = adapter

        // grab repository singleton
        val repository = RepositoryProvider.getRepository(requireContext())

        // pass repository into ViewModelFactory
        val factory = RepositoryViewModelFactory(repository)

        // get ViewModel using the factory
        viewModel = ViewModelProvider(this, factory)[TransactionViewModel::class.java]

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.transactionsWithNestsFlow.collect { transactions ->
                adapter.updateData(transactions)
            }
        }
    }


}
