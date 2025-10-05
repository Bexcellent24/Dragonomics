package com.TheBudgeteers.dragonomics.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.TheBudgeteers.dragonomics.R
import com.TheBudgeteers.dragonomics.data.SessionStore
import com.TheBudgeteers.dragonomics.utils.RepositoryProvider
import com.TheBudgeteers.dragonomics.viewmodel.TransactionViewModel
import com.TheBudgeteers.dragonomics.viewmodel.RepositoryViewModelFactory
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

// Fragment that shows a scrollable list of all user's transactions.
// Gets transaction data from the TransactionViewModel.
// Automatically updates the list when new transactions are added.
// Only shows transactions for the currently logged-in user.

class TransactionFragment : Fragment() {

    private lateinit var viewModel: TransactionViewModel
    private lateinit var adapter: TransactionAdapter
    private lateinit var sessionStore: SessionStore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_transaction_list, container, false)

        setupRecyclerView(view)
        setupViewModel()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadTransactions()
    }


    private fun setupRecyclerView(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerTransactions)
        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = TransactionAdapter(emptyList())
        recyclerView.adapter = adapter
    }

    private fun setupViewModel() {
        sessionStore = SessionStore(requireContext())
        val repository = RepositoryProvider.getRepository(requireContext())
        val factory = RepositoryViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[TransactionViewModel::class.java]
    }


    private fun loadTransactions() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Get current user ID from session
            val userId = sessionStore.userId.firstOrNull()

            if (userId == null) {
                showNoUserError()
                return@launch
            }

            // Tell ViewModel which user's transactions to load
            viewModel.setUserId(userId)

            // Listen for transaction updates and refresh the list
            observeTransactions()
        }
    }

    private suspend fun observeTransactions() {
        viewModel.transactionsWithNestsFlow.collect { transactions ->
            adapter.updateData(transactions)
        }
    }

    //Error handling
    private fun showNoUserError() {
        Toast.makeText(requireContext(), "No user logged in", Toast.LENGTH_SHORT).show()
    }
}