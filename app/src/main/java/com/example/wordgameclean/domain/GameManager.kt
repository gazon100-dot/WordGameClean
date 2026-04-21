package com.example.wordgameclean.domain

import android.content.Context

class GameManager {

    // ================= MODE =================

    private var mode: String = "classic"

    fun setMode(m: String) {
        mode = m
    }

    // ================= TYPES =================

    enum class ActionType {
        NONE,
        REPLACE,
        INSERT_DELETE,
        SWAP
    }

    // ================= DISABLED LETTERS =================

    private val disabledLetters = mutableSetOf<Char>()

    fun isLetterDisabled(c: Char): Boolean {
        return disabledLetters.contains(c)
    }

    private fun disableRandomLetter() {

        val allLetters = ('А'..'Я').toList()
        val available = allLetters.filter { it !in disabledLetters }

        if (available.isEmpty()) return

        val vowels = listOf('А','Е','И','О','У','Ы','Э','Ю','Я')

        val weightedPool = mutableListOf<Char>()

        for (c in available) {
            if (c in vowels) {
                repeat(1) { weightedPool.add(c) } // гласные реже
            } else {
                repeat(3) { weightedPool.add(c) } // согласные чаще
            }
        }

        val chosen = weightedPool.random()
        disabledLetters.add(chosen)
    }

    // ================= STATE =================

    private var lastActionType: ActionType = ActionType.NONE
    private var lastActionIndex: Int = -1

    fun getLastActionType(): ActionType = lastActionType
    fun getLastActionIndex(): Int = lastActionIndex

    private var dictionary: Set<String> = emptySet()

    private var isCompressMode = false
    private var currentWord = mutableListOf<Char>()
    private var startWord = mutableListOf<Char>()
    private var turnBaseWord = mutableListOf<Char>()

    private var score = 0
    private var correctCount = 0
    private var bestScore = 0

    private val usedWords = mutableSetOf<String>()

    private var currentAction = ActionType.NONE
    private var actionCount = 0

    // ================= INIT =================

    fun setDictionary(words: Set<String>) {
        dictionary = words.map { it.lowercase() }.toSet()
    }

    fun init(context: Context) {

        val prefs = context.getSharedPreferences("game", Context.MODE_PRIVATE)
        bestScore = prefs.getInt("best_score", 0)

        disabledLetters.clear() // 🔥 ВАЖНО

        currentWord = getRandomStartWord()
        startWord = currentWord.toMutableList()
        turnBaseWord = currentWord.toMutableList()

        score = 0
        correctCount = 0

        usedWords.clear()
        usedWords.add(currentWord.joinToString("").lowercase())
    }

    fun save(context: Context) {
        val prefs = context.getSharedPreferences("game", Context.MODE_PRIVATE)
        prefs.edit().putInt("best_score", bestScore).apply()
    }

    fun getState(): GameState {
        return GameState(
            word = currentWord,
            score = score,
            usedWords = usedWords.toList(),
            correctCount = correctCount,
            bestScore = bestScore
        )
    }

    // ================= TURN =================

    private fun getReplaceLimit(): Int {
        val len = currentWord.size
        return when {
            len <= 5 -> 1
            len <= 7 -> 2
            len == 8 -> 3
            else -> 4
        }
    }

    private fun isSubsequenceValid(): Boolean {
        val base = turnBaseWord.joinToString("")
        val current = currentWord.joinToString("")
        return base.contains(current)
    }

    private fun canDoAction(type: ActionType): Boolean {

        if (currentAction == ActionType.NONE) {
            currentAction = type
            return true
        }

        if (currentAction != type) return false

        return when (type) {
            ActionType.REPLACE -> actionCount < getReplaceLimit()
            ActionType.INSERT_DELETE -> actionCount < 1
            ActionType.SWAP -> true
            else -> false
        }
    }

    private fun resetTurn() {
        currentAction = ActionType.NONE
        actionCount = 0
        lastActionType = ActionType.NONE
        lastActionIndex = -1
    }

    // ================= GAME =================

    fun submitWord(): Boolean {

        if (mode == "ended") return false

        val wordStr = currentWord.joinToString("").lowercase()

        if (isCompressMode) {
            if (currentWord.size !in 2..3) return false
            stopCompressMode()
        }

        if (!dictionary.contains(wordStr)) {
            applyPenalty()
            currentWord = startWord.toMutableList()
            resetTurn()
            return false
        }

        if (usedWords.contains(wordStr)) {
            applyPenalty()
            currentWord = startWord.toMutableList()
            resetTurn()
            return false
        }

        usedWords.add(wordStr)

        val points = calcPoints(wordStr.length)
        score += points
        correctCount++

        // 🔥 ЛОГИКА TIME РЕЖИМА
        if (mode == "timer" && correctCount % 3 == 0) {
            disableRandomLetter()
        }

        if (score > bestScore) {
            bestScore = score
        }

        startWord = currentWord.toMutableList()
        turnBaseWord = startWord.toMutableList()
        resetTurn()

        return true
    }

    private fun applyPenalty() {
        score -= 1
        if (score < 0) score = 0
    }

    private fun calcPoints(len: Int): Int {
        return when {
            len <= 3 -> 0
            len <= 5 -> 1
            len == 6 -> 2
            len == 7 -> 3
            len == 8 -> 4
            else -> 6
        }
    }

    fun undoTurn() {
        currentWord = startWord.toMutableList()
        turnBaseWord = startWord.toMutableList()
        resetTurn()
    }

    fun resetGame() {

        disabledLetters.clear() // 🔥 ВАЖНО

        currentWord = getRandomStartWord()
        startWord = currentWord.toMutableList()
        turnBaseWord = currentWord.toMutableList()

        score = 0
        correctCount = 0

        usedWords.clear()
        usedWords.add(currentWord.joinToString("").lowercase())

        resetTurn()
    }

    // ================= ACTIONS =================

    fun insertLetterAt(index: Int, c: Char): Boolean {
        if (!canDoAction(ActionType.INSERT_DELETE)) return false
        if (index < 0 || index > currentWord.size) return false

        currentWord.add(index, c)

        lastActionType = ActionType.INSERT_DELETE
        lastActionIndex = index

        actionCount++
        return true
    }

    fun removeLetter(index: Int): Boolean {

        if (!isCompressMode) {
            if (!canDoAction(ActionType.INSERT_DELETE)) return false
            if (index !in currentWord.indices) return false

            currentWord.removeAt(index)
            actionCount++
            return true
        }

        if (index !in currentWord.indices) return false

        val backup = currentWord.toMutableList()
        currentWord.removeAt(index)

        if (!isSubsequenceValid()) {
            currentWord = backup
            return false
        }

        return true
    }

    fun replaceLetter(index: Int, c: Char): Boolean {
        if (!canDoAction(ActionType.REPLACE)) return false
        if (index !in currentWord.indices) return false

        currentWord[index] = c

        lastActionType = ActionType.REPLACE
        lastActionIndex = index

        actionCount++
        return true
    }

    fun swapLetters(from: Int, to: Int): Boolean {
        if (!canDoAction(ActionType.SWAP)) return false
        if (from !in currentWord.indices || to !in currentWord.indices) return false

        val tmp = currentWord[from]
        currentWord[from] = currentWord[to]
        currentWord[to] = tmp

        lastActionType = ActionType.SWAP
        lastActionIndex = to

        return true
    }

    fun getActionInfo(): String {
        return when (currentAction) {
            ActionType.NONE -> "Ход не начат"
            ActionType.REPLACE -> "Замена: $actionCount/${getReplaceLimit()}"
            ActionType.INSERT_DELETE -> "Добавление/Удаление: $actionCount/1"
            ActionType.SWAP -> "Перестановка (без лимита)"
        }
    }

    // ================= MODES =================

    fun startCompressMode() {
        isCompressMode = true
        turnBaseWord = currentWord.toMutableList()
    }

    fun stopCompressMode() {
        isCompressMode = false
    }

    private fun getRandomStartWord(): MutableList<Char> {
        val candidates = dictionary.filter { it.length == 3 }

        if (candidates.isEmpty()) {
            return mutableListOf('К', 'О', 'Т')
        }

        return candidates.random().uppercase().toMutableList()
    }
}