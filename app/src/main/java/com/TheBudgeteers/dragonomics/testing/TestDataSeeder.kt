package com.TheBudgeteers.dragonomics.testing

import com.TheBudgeteers.dragonomics.data.Repository
import com.TheBudgeteers.dragonomics.models.Nest
import com.TheBudgeteers.dragonomics.models.NestType
import com.TheBudgeteers.dragonomics.models.Transaction
import java.util.Calendar
import java.util.Date

class TestDataSeeder(private val repository: Repository) {

    suspend fun seedDummyData(userId: Long) {
        // Only insert if user has no nests to prevent duplicates
        if (repository.getNests(userId).isNotEmpty()) return

        val dummyNests = listOf(
            Nest(
                userId = userId,
                name = "Food",
                budget = 2000.0,
                icon = "ci_apple",
                colour = "#53171c",
                type = NestType.EXPENSE
            ),
            Nest(
                userId = userId,
                name = "Fuel",
                budget = 1500.0,
                icon = "ci_car",
                colour = "#9b252c",
                type = NestType.EXPENSE
            ),
            Nest(
                userId = userId,
                name = "Games",
                budget = 1000.0,
                icon = "ci_heart",
                colour = "#523295",
                type = NestType.EXPENSE
            ),
            Nest(
                userId = userId,
                name = "Coffee",
                budget = 500.0,
                icon = "ci_coffee",
                colour = "#a44e24",
                type = NestType.EXPENSE
            ),
            Nest(
                userId = userId,
                name = "Salary",
                budget = null,
                icon = "ci_coin_stack",
                colour = "#8b98ad",
                type = NestType.INCOME
            ),
            Nest(
                userId = userId,
                name = "Side Hustle",
                budget = null,
                icon = "ci_piggy_bank",
                colour = "#231c2a",
                type = NestType.INCOME
            )
        )

        dummyNests.forEach { repository.addNest(it) }

        val nestsInDb = repository.getNests(userId)

        // Helper to generate past dates
        fun daysAgo(days: Int): Date {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -days)
            return cal.time
        }

        val dummyTransactions = listOf(
            // Food expenses
            Transaction(
                userId = userId,
                title = "Groceries",
                amount = 450.0,
                date = daysAgo(2),
                photoPath = null,
                description = "Monthly groceries at Checkers",
                categoryId = nestsInDb.first { it.name == "Food" }.id,
                fromCategoryId = nestsInDb.first { it.name == "Salary" }.id
            ),
            Transaction(
                userId = userId,
                title = "Takeaway",
                amount = 120.0,
                date = daysAgo(1),
                photoPath = null,
                description = "Nando's lunch",
                categoryId = nestsInDb.first { it.name == "Food" }.id,
                fromCategoryId = nestsInDb.first { it.name == "Salary" }.id
            ),

            // Fuel expenses
            Transaction(
                userId = userId,
                title = "Petrol",
                amount = 700.0,
                date = daysAgo(1),
                photoPath = null,
                description = "Filled up at Shell",
                categoryId = nestsInDb.first { it.name == "Fuel" }.id,
                fromCategoryId = nestsInDb.first { it.name == "Salary" }.id
            ),
            Transaction(
                userId = userId,
                title = "Petrol",
                amount = 650.0,
                date = daysAgo(2),
                photoPath = null,
                description = "Refuel before road trip",
                categoryId = nestsInDb.first { it.name == "Fuel" }.id,
                fromCategoryId = nestsInDb.first { it.name == "Salary" }.id
            ),

            // Games expenses
            Transaction(
                userId = userId,
                title = "Steam Purchase",
                amount = 299.0,
                date = daysAgo(2),
                photoPath = null,
                description = "Bought indie game on sale",
                categoryId = nestsInDb.first { it.name == "Games" }.id,
                fromCategoryId = nestsInDb.first { it.name == "Side Hustle" }.id
            ),
            Transaction(
                userId = userId,
                title = "PSN Subscription",
                amount = 120.0,
                date = daysAgo(3),
                photoPath = null,
                description = "PlayStation Plus renewal",
                categoryId = nestsInDb.first { it.name == "Games" }.id,
                fromCategoryId = nestsInDb.first { it.name == "Side Hustle" }.id
            ),

            // Coffee expenses
            Transaction(
                userId = userId,
                title = "Morning Coffee",
                amount = 38.0,
                date = daysAgo(3),
                photoPath = null,
                description = "Flat white at Vida",
                categoryId = nestsInDb.first { it.name == "Coffee" }.id,
                fromCategoryId = nestsInDb.first { it.name == "Side Hustle" }.id
            ),
            Transaction(
                userId = userId,
                title = "Afternoon Coffee",
                amount = 42.0,
                date = daysAgo(1),
                photoPath = null,
                description = "Iced latte at Starbucks",
                categoryId = nestsInDb.first { it.name == "Coffee" }.id,
                fromCategoryId = nestsInDb.first { it.name == "Side Hustle" }.id
            ),

            // Income transactions
            Transaction(
                userId = userId,
                title = "Monthly Salary",
                amount = 22000.0,
                date = daysAgo(1),
                photoPath = null,
                description = "September salary",
                categoryId = nestsInDb.first { it.name == "Salary" }.id,
                fromCategoryId = null
            ),
            Transaction(
                userId = userId,
                title = "Freelance Job",
                amount = 4500.0,
                date = daysAgo(2),
                photoPath = null,
                description = "Website design side gig",
                categoryId = nestsInDb.first { it.name == "Side Hustle" }.id,
                fromCategoryId = null
            )
        )

        dummyTransactions.forEach { repository.addTransaction(it) }
    }
}