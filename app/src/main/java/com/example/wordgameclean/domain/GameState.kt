package com.example.wordgameclean.domain

data class GameState(
    val word: MutableList<Char>,
    val score: Int,
    val usedWords: List<String>,
    val correctCount: Int,
    val bestScore: Int
)