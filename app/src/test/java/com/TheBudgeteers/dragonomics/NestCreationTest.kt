package com.TheBudgeteers.dragonomics.nest

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

enum class NestType { Category, Savings, Income }
enum class Mood { POSITIVE, NEUTRAL, NEGATIVE }

data class Nest(
    val id: Long = 0L,
    val name: String,
    val type: NestType,
    val budget: Double,
    val spent: Double = 0.0,
    val mood: Mood = Mood.NEUTRAL
)

object NestFactory {
    fun create(name: String, type: NestType, budget: Double): Nest {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Nest name cannot be blank" }
        require(budget >= 0.0) { "Budget cannot be negative" }
        return Nest(name = trimmed, type = type, budget = budget)
    }
}

class NestCreationTest {

    @Test fun valid_setsDefaultsAndTrims() {
        val n = NestFactory.create("  Groceries  ", NestType.Category, 500.0)
        assertThat(n.name).isEqualTo("Groceries")
        assertThat(n.type).isEqualTo(NestType.Category)
        assertThat(n.budget).isWithin(1e-6).of(500.0)
        assertThat(n.spent).isWithin(1e-6).of(0.0)
        assertThat(n.mood).isEqualTo(Mood.NEUTRAL)
    }

    @Test fun blankName_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            NestFactory.create("   ", NestType.Category, 100.0)
        }
    }

    @Test fun negativeBudget_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            NestFactory.create("Rent", NestType.Category, -1.0)
        }
    }
}
