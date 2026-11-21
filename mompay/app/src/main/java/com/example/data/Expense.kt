package com.example.data

data class Expense(
    val id: String,
    val title: String,
    val amountPaise: Long,
    val paidById: String,
    val participantIds: List<String>,
    val createdAtMillis: Long
)
