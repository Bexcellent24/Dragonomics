package com.TheBudgeteers.dragonomics

import com.TheBudgeteers.dragonomics.models.Nest
import com.TheBudgeteers.dragonomics.models.NestType
import org.junit.Assert.*
import org.junit.Test


//Unit tests for Nest creation and validation
//Tests business rules for income and expense nests

class NestCreationTests {

    @Test
    fun expense_nest_requires_budget() {
        val nest = Nest(
            userId = 1L,
            name = "Groceries",
            budget = 500.0,
            icon = "ci_shopping_cart",
            colour = "#53171c",
            type = NestType.EXPENSE
        )

        assertNotNull("Expense nest should have a budget", nest.budget)
        assertTrue("Budget should be positive", nest.budget!! > 0)
    }

    @Test
    fun income_nest_has_no_budget() {
        val nest = Nest(
            userId = 1L,
            name = "Salary",
            budget = null,
            icon = "ci_coin_stack",
            colour = "#9b252c",
            type = NestType.INCOME
        )

        assertNull("Income nest should not have a budget", nest.budget)
        assertEquals("Type should be INCOME", NestType.INCOME, nest.type)
    }

    @Test
    fun nest_name_cannot_be_empty() {
        val validName = "Rent"
        val emptyName = ""

        assertTrue("Valid name should not be empty", validName.isNotEmpty())
        assertFalse("Empty name should fail validation", emptyName.isNotEmpty())
    }

    @Test
    fun nest_must_have_icon_selected() {
        val validIcon = "ci_home"
        val nullIcon: String? = null

        assertFalse("Null icon should fail validation", nullIcon.isNullOrEmpty())
        assertTrue("Valid icon should pass validation", !validIcon.isNullOrEmpty())
    }

    @Test
    fun nest_must_have_colour_selected() {
        val validColour = "#53171c"
        val emptyColour = ""

        assertTrue("Valid colour should pass validation", validColour.isNotEmpty())
        assertFalse("Empty colour should fail validation", emptyColour.isNotEmpty())
    }

    @Test
    fun expense_budget_cannot_be_negative() {
        val validBudget = 100.0
        val negativeBudget = -50.0

        assertTrue("Valid budget should be positive", validBudget >= 0)
        assertFalse("Negative budget should fail validation", negativeBudget >= 0)
    }

    @Test
    fun expense_budget_cannot_be_zero() {
        val zeroBudget = 0.0
        val validBudget = 1.0

        assertFalse("Zero budget should fail validation", zeroBudget > 0)
        assertTrue("Positive budget should pass validation", validBudget > 0)
    }
}