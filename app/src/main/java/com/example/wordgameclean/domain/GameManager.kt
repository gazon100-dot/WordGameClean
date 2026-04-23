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
        SWAP,
        TOGGLE_YO
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

    private var dictionary = mutableSetOf<String>()            // оригинал
    private var normalizedDictionary = mutableSetOf<String>()  // для проверки

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
        dictionary = words.map { it.lowercase() }.toMutableSet()

        normalizedDictionary = words
            .map { normalize(it) }
            .toMutableSet()    }

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

    fun toggleYo(index: Int): Boolean {

        if (index !in currentWord.indices) return false

        val c = currentWord[index]

        if (c == 'Е') {
            currentWord[index] = 'Ё'
        } else if (c == 'Ё') {
            currentWord[index] = 'Е'
        } else {
            return false
        }

        lastActionType = ActionType.TOGGLE_YO
        lastActionIndex = index

        return true
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
            ActionType.TOGGLE_YO -> actionCount < 1
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

    fun submitWord(): Pair<Boolean, String?> {

        if (mode == "ended") return Pair(false, null)

        val rawWord = currentWord.joinToString("").lowercase()
        val wordStr = normalize(rawWord)


        if (isCompressMode) {
            if (currentWord.size !in 2..3) return Pair(false, null)
            stopCompressMode()
        }

        if (dictionary.none { normalize(it) == wordStr }) {
            applyPenalty()
            currentWord = startWord.toMutableList()
            resetTurn()
            return Pair(false, null)
        }

        if (usedWords.contains(rawWord)) {
            applyPenalty()
            currentWord = startWord.toMutableList()
            resetTurn()
            return Pair(false, rawWord) // 👈 передаём слово
        }

        usedWords.add(rawWord)

        // 💥 ищем оригинальное слово из словаря
        val originalInput = currentWord.joinToString("").lowercase()

        val realWord = dictionary.find { it == originalInput }

        if (realWord != null) {
            currentWord = realWord.uppercase().toMutableList()
        } else {
            // fallback если вдруг в словаре только "е"-версия
            val fallback = dictionary.find { normalize(it) == wordStr }
            if (fallback != null) {
                currentWord = fallback.uppercase().toMutableList()
            }
            val finalWord = currentWord.joinToString("").lowercase()
            usedWords.add(normalize(finalWord))
        }

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

        return Pair(true, null)
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
            return false      }

        return true
    }

    fun replaceLetter(index: Int, c: Char): Boolean {
        if (index !in currentWord.indices) return false
        val newChar = c.uppercaseChar()   // 💥 ВОТ ЭТО КЛЮЧ
        val current = currentWord[index]

        // 🔥 toggle без лимитов
        if ((current == 'Е' && newChar == 'Е') ||
            (current == 'Ё' && newChar == 'Е')) {
            return toggleYo(index)
        }

        if (!canDoAction(ActionType.REPLACE)) return false

        currentWord[index] = newChar

        lastActionType = ActionType.REPLACE
        lastActionIndex = index

        actionCount++
        return true
    }

    fun isCompressMode(): Boolean {
        return isCompressMode
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
            ActionType.TOGGLE_YO -> "Ё переключение"
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
    fun saveGame(context: Context, timeLeft: Int, timerStarted: Boolean) {

        val prefs = context.getSharedPreferences("save_$mode", Context.MODE_PRIVATE)

        prefs.edit().apply {
            putString("currentWord", currentWord.joinToString(""))
            putString("startWord", startWord.joinToString(""))
            putString("turnBaseWord", turnBaseWord.joinToString(""))

            putInt("score", score)
            putInt("correctCount", correctCount)

            putStringSet("usedWords", usedWords)
            putString("disabledLetters", disabledLetters.joinToString(""))

            putInt("timeLeft", timeLeft)
            putBoolean("timerStarted", timerStarted)

            apply()
        }
    }
    fun loadGame(context: Context): Triple<Int, Boolean, Boolean> {

        val prefs = context.getSharedPreferences("save_$mode", Context.MODE_PRIVATE)

        if (!prefs.contains("currentWord")) {
            return Triple(0, false, false)
        }

        currentWord = prefs.getString("currentWord", "")!!.toMutableList()
        startWord = prefs.getString("startWord", "")!!.toMutableList()
        turnBaseWord = prefs.getString("turnBaseWord", "")!!.toMutableList()

        score = prefs.getInt("score", 0)
        correctCount = prefs.getInt("correctCount", 0)

        usedWords.clear()
        usedWords.addAll(prefs.getStringSet("usedWords", emptySet())!!)

        disabledLetters.clear()
        disabledLetters.addAll(
            prefs.getString("disabledLetters", "")!!.toList()
        )

        val timeLeft = prefs.getInt("timeLeft", 180)
        val timerStarted = prefs.getBoolean("timerStarted", false)

        return Triple(timeLeft, timerStarted, true)
    }
    fun hasSave(context: Context): Boolean {
        val prefs = context.getSharedPreferences("save_$mode", Context.MODE_PRIVATE)
        return prefs.contains("currentWord")
    }
    fun clearSave(context: Context) {
        context.getSharedPreferences("save_$mode", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
    private fun normalize(word: String): String {
        return word
            .lowercase()
            .replace('ё', 'е')


    }


}