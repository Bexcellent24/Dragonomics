package com.TheBudgeteers.dragonomics.testing


import com.TheBudgeteers.dragonomics.data.Repository
import com.TheBudgeteers.dragonomics.models.Nest
import com.TheBudgeteers.dragonomics.models.NestType
import com.TheBudgeteers.dragonomics.models.Transaction
import java.util.Date

class TestDataSeeder(private val repository: Repository) {

    suspend fun seedDummyData() {
        // Only insert if DB is empty to prevent duplicates
        if (repository.getNests().isNotEmpty()) return

        val dummyNests = listOf(
            Nest(name = "Food", budget = 2000.0, icon = "ci_apple", colour = "#53171c", type = NestType.EXPENSE),
            Nest(name = "Transport", budget = 1000.0, icon = "ci_car", colour = "#9b252c", type = NestType.EXPENSE),
            Nest(name = "Coffee", budget = 500.0, icon = "ci_coffee", colour = "#523295", type = NestType.EXPENSE),
            Nest(name = "Salary", budget = null, icon = "ci_coin_stack", colour = "#a44e24", type = NestType.INCOME)
        )

        dummyNests.forEach { repository.addNest(it) }

        val nestsInDb = repository.getNests()

        val dummyTransactions = listOf(
            Transaction(title = "Groceries", amount = 250.0, date = Date(), photoPath = null, description = "Bought food for the week", categoryId = nestsInDb[0].id, fromCategoryId = null),
            Transaction(title = "Taxi", amount = 80.0, date = Date(), photoPath = null, description = "Ride to work", categoryId = nestsInDb[1].id, fromCategoryId = null),
            Transaction(title = "Coffee", amount = 35.0, date = Date(), photoPath = null, description = "Morning coffee", categoryId = nestsInDb[2].id, fromCategoryId = null),
            Transaction(title = "Monthly Salary", amount = 5000.0, date = Date(), photoPath = null, description = "September pay", categoryId = nestsInDb[3].id, fromCategoryId = null)
        )

        dummyTransactions.forEach { repository.addTransaction(it) }
    }
}
