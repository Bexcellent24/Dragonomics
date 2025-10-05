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

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerTransactions)
        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = TransactionAdapter(emptyList())
        recyclerView.adapter = adapter

        // Initialize session store
        sessionStore = SessionStore(requireContext())

        // Setup ViewModel
        val repository = RepositoryProvider.getRepository(requireContext())
        val factory = RepositoryViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[TransactionViewModel::class.java]

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            // Get user ID from session
            val userId = sessionStore.userId.firstOrNull()

            if (userId == null) {
                Toast.makeText(requireContext(), "No user logged in", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Set user ID in ViewModel to activate the flow
            viewModel.setUserId(userId)

            // Collect transactions
            viewModel.transactionsWithNestsFlow.collect { transactions ->
                adapter.updateData(transactions)
            }
        }
    }
}