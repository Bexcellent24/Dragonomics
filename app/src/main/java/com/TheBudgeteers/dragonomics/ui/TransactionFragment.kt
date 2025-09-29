package com.TheBudgeteers.dragonomics.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.TheBudgeteers.dragonomics.R
import com.TheBudgeteers.dragonomics.data.RepositoryProvider
import com.TheBudgeteers.dragonomics.models.Nest
import com.TheBudgeteers.dragonomics.models.NestType
import com.TheBudgeteers.dragonomics.models.Transaction
import com.TheBudgeteers.dragonomics.models.TransactionWithNest
import com.TheBudgeteers.dragonomics.viewmodel.TransactionViewModel
import com.TheBudgeteers.dragonomics.viewmodel.RepositoryViewModelFactory
import java.util.Date

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

        // ==== DUMMY TRANSACTIONS FOR TESTING ====
        val dummyNests = listOf(
            Nest(1, "Food", 2000.0, "ci_apple", "#FF5733", NestType.EXPENSE),
            Nest(2, "Transport", 1000.0, "ci_car", "#33FF57", NestType.EXPENSE),
            Nest(3, "Coffee", 500.0, "ci_coffee", "#3357FF", NestType.EXPENSE),
            Nest(4, "Salary", null, "ci_coin_stack", "#33FFFF", NestType.INCOME)
        )

        val dummyTransactionsWithNests = listOf(
            TransactionWithNest(
                transaction = Transaction(
                    id = 1,
                    title = "Groceries",
                    amount = 250.0,
                    date = Date(),
                    photoPath = null,
                    description = "Bought food for the week",
                    categoryId = 1,
                    fromCategoryId = null
                ),
                categoryNest = dummyNests[0], // Food
                fromNest = null
            ),
            TransactionWithNest(
                transaction = Transaction(
                    id = 2,
                    title = "Taxi",
                    amount = 80.0,
                    date = Date(),
                    photoPath = null,
                    description = "Ride to work",
                    categoryId = 2,
                    fromCategoryId = null
                ),
                categoryNest = dummyNests[1], // Transport
                fromNest = null
            ),
            TransactionWithNest(
                transaction = Transaction(
                    id = 3,
                    title = "Coffee",
                    amount = 35.0,
                    date = Date(),
                    photoPath = null,
                    description = "Morning coffee",
                    categoryId = 3,
                    fromCategoryId = null
                ),
                categoryNest = dummyNests[2], // Coffee
                fromNest = null
            ),
            TransactionWithNest(
                transaction = Transaction(
                    id = 4,
                    title = "Monthly Salary",
                    amount = 5000.0,
                    date = Date(),
                    photoPath = null,
                    description = "September pay",
                    categoryId = 4,
                    fromCategoryId = null
                ),
                categoryNest = dummyNests[3], // Salary
                fromNest = null
            )
        )



        // Feed dummy transactions into adapter
        adapter.updateData(dummyTransactionsWithNests)

        // ========================================
        // Once your real database is ready, uncomment this:
        /*
        viewModel.getTransactionsWithNests { transactions ->
        adapter.updateData(transactions)
        }

        */
    }

}
