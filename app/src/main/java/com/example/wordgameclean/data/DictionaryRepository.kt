package com.example.wordgameclean.data

import android.content.Context

class DictionaryRepository(private val context: Context) {

    fun load(): Set<String> {
        return try {
            context.assets.open("words.txt").bufferedReader().useLines { lines ->
                lines.map { it.trim().lowercase() }
                    .filter { it.length >= 3 }
                    .toSet()
            }
        } catch (e: Exception) {
            setOf("кот", "ток", "дом")
        }
    }
}