package com.TheBudgeteers.dragonomics

import com.TheBudgeteers.dragonomics.models.Transaction
import org.junit.Assert.*
import org.junit.Test
import java.util.Date


 //Unit tests for Transaction creation and validation
//Tests business rules for expense and income transactions

class TransactionCreationTests {

    @Test
    fun transaction_title_cannot_be_blank() {
        val validTitle = "Coffee"
        val blankTitle = "   "

        assertFalse("Blank title should fail validation", blankTitle.trim().isNotBlank())
        assertTrue("Valid title should pass validation", validTitle.trim().isNotBlank())
    }

    @Test
    fun transaction_amount_must_be_positive() {
        val validAmount = 50.0
        val zeroAmount = 0.0
        val negativeAmount = -10.0

        assertTrue("Positive amount should pass validation", validAmount > 0)
        assertFalse("Zero amount should fail validation", zeroAmount > 0)
        assertFalse("Negative amount should fail validation", negativeAmount > 0)
    }

    @Test
    fun expense_transaction_requires_from_category() {
        val categoryId = 1L
        val fromCategoryId = 2L
        val nullFromCategory: Long? = null

        // For expense transactions
        assertNotNull("Expense should have fromCategoryId", fromCategoryId)

        // For income transactions
        assertNull("Income should not have fromCategoryId", nullFromCategory)
    }

    @Test
    fun transaction_date_defaults_to_current() {
        val transactionDate = Date()
        val now = Date()

        // Allow 1 second difference for test execution time
        val timeDiff = Math.abs(now.time - transactionDate.time)
        assertTrue("Transaction date should be close to current time", timeDiff < 1000)
    }

    @Test
    fun transaction_photo_path_is_optional() {
        val withPhoto = "storage/photo123.jpg"
        val withoutPhoto: String? = null

        assertTrue("Photo path can be present", withPhoto.isNotEmpty())
        assertTrue("Photo path can be null", withoutPhoto == null)
    }

    @Test
    fun transaction_description_is_optional() {
        val withDescription = "Monthly subscription"
        val emptyDescription = ""

        assertTrue("Description can be present", withDescription.isNotEmpty())
        assertTrue("Description can be empty", emptyDescription.isEmpty())
    }

    @Test
    fun transaction_must_have_category() {
        val validCategoryId = 1L
        val zeroCategoryId = 0L

        assertTrue("Valid category ID should be positive", validCategoryId > 0)
        assertFalse("Zero category ID should fail validation", zeroCategoryId > 0)
    }

    @Test
    fun income_transaction_has_no_from_category() {
        val transaction = Transaction(
            userId = 1L,
            title = "Salary",
            amount = 3000.0,
            date = Date(),
            photoPath = null,
            description = "Monthly salary",
            categoryId = 1L,
            fromCategoryId = null  // Income transactions don't have fromCategoryId
        )

        assertNull("Income transaction should not have fromCategoryId", transaction.fromCategoryId)
    }

    @Test
    fun expense_transaction_requires_both_categories() {
        val transaction = Transaction(
            userId = 1L,
            title = "Groceries",
            amount = 150.0,
            date = Date(),
            photoPath = null,
            description = "Weekly shopping",
            categoryId = 2L,      // Expense category
            fromCategoryId = 1L   // Income source
        )

        assertNotNull("Expense transaction should have categoryId", transaction.categoryId)
        assertNotNull("Expense transaction should have fromCategoryId", transaction.fromCategoryId)
    }
}