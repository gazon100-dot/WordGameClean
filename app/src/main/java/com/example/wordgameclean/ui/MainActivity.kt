package com.example.wordgameclean.ui

import android.content.ClipData
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.wordgameclean.domain.GameManager
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.FlexWrap
import android.widget.ScrollView


class MainActivity : AppCompatActivity() {
    private var timerStarted = false
    private lateinit var timerHandler: android.os.Handler
    private lateinit var usedScroll: ScrollView
    private var timerRunnable: Runnable? = null
    private var mode: String = "classic"

    private var messageAnimMode = 0

    private var timeLeft = 180
    private lateinit var timerText: TextView
    private var topWeight = 1f
    private var gameWeight = 2f
    private var bottomWeight = 1f
    private lateinit var gameManager: GameManager

    private lateinit var wordContainer: LinearLayout
    private lateinit var keyboardContainer: GridLayout
    private lateinit var scoreText: TextView
    private lateinit var usedWordsContainer: FlexboxLayout
    private lateinit var actionIndicator: TextView
    private lateinit var infoPanel: TextView
    private lateinit var rootLayout: LinearLayout
    private lateinit var topBlock: LinearLayout
    private lateinit var gameBlock: LinearLayout
    private lateinit var bottomBlock: LinearLayout
    private var isDraggingFromKeyboard = false

    private var highlightWord: String? = null
    private fun applyLayoutWeights() {

        topBlock.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            topWeight
        )

        gameBlock.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            gameWeight
        )

        bottomBlock.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            bottomWeight
        )

        rootLayout.requestLayout()
    }
    override fun onResume() {
        super.onResume()

        if (::topBlock.isInitialized) {
            loadUiSettings()
            applyLayoutWeights()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        timerHandler = android.os.Handler(mainLooper)

        loadUiSettings()

        mode = intent.getStringExtra("mode") ?: "classic"
        println("MODE = $mode")

        timerText = TextView(this).apply {
            textSize = 16f
            gravity = Gravity.CENTER
        }


        gameManager = GameManager()
        gameManager.setMode(mode)

        val words = assets.open("words.txt")
            .bufferedReader()
            .readLines()
            .toSet()

        gameManager.setDictionary(words)
        gameManager.init(this)
        val (savedTime, savedStarted, hasSave) = gameManager.loadGame(this)

        if (hasSave) {
            timeLeft = savedTime
            timerStarted = savedStarted

            if (mode == "timer" && timerStarted) {
                startTimer()
            }
        }

        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(8, 8, 8, 8)
        }
        // ===== TOP =====

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val menuBtn = createTopButton("Меню", "⬅") {
            finish()
        }

        val rulesBtn = createTopButton("Правила", "📜") {
            showRules()
        }

        val resetBtn = createTopButton("Сброс", "🔄") {
            gameManager.resetGame()
            gameManager.clearSave(this)
            resetTimer()
            render()
            createKeyboard()
        }

        val compressBtn = createTopButton("Сжать слово", "📦") {
            gameManager.startCompressMode()
            showMessage("Режим сжатия: оставь 2–3 буквы подряд", Color.parseColor("#2196F3"))
        }

        topBar.addView(menuBtn)
        topBar.addView(rulesBtn)
        topBar.addView(resetBtn)
        topBar.addView(compressBtn)

        // ===== SCORE =====

        scoreText = TextView(this).apply {
            textSize = 18f
            gravity = Gravity.CENTER
        }

        // ===== INFO PANEL =====

        infoPanel = TextView(this).apply {
            textSize = 16f
            gravity = Gravity.CENTER

            alpha = 0f // 👈 изначально скрыт

            setPadding(16, 16, 16, 16)

            background = GradientDrawable().apply {
                setColor(Color.parseColor("#EEEEEE"))
                cornerRadius = 20f
            }
        }
        // ===== ACTION INDICATOR =====

        actionIndicator = TextView(this).apply {
            textSize = 14f
            gravity = Gravity.CENTER
        }

        // ===== WORD =====

        wordContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(8, 32, 8, 32)
        }

        // ===== CONTROLS =====

        val undo = Button(this).apply {
            text = "ОТМЕНА"
            styleButton(this)
            setOnClickListener {
                gameManager.undoTurn()
                render()
            }
        }

        val delete = TextView(this).apply {
            text = "🗑"
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(40, 20, 40, 20)
            setBackgroundColor(Color.RED)
            setTextColor(Color.WHITE)
        }

        val submit = Button(this).apply {
            text = "ГОТОВО"
            styleButton(this)
            setOnClickListener {
                val before = gameManager.getState().score
                val (ok, duplicateWord) = gameManager.submitWord()
                if (mode == "timer" && ok && !timerStarted) {
                    timerStarted = true
                    startTimer()
                }
                val after = gameManager.getState().score
                val delta = after - before

                if (!ok) shakeView(wordContainer)
                if (!ok && duplicateWord != null) {
                    highlightWord = duplicateWord
                }

                if (ok) {
                    showMessage("Слово принято +$delta", Color.parseColor("#4CAF50"))
                } else {
                    showMessage("Ошибка (-1)", Color.parseColor("#F44336"))
                }

                gameManager.saveGame(this@MainActivity, timeLeft, timerStarted)
                render()
                createKeyboard()
            }
        }

        val controls = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            addView(undo)
            addView(delete)
            addView(submit)
        }

        // ===== USED WORDS =====

        usedWordsContainer = FlexboxLayout(this).apply {
            flexWrap = FlexWrap.WRAP // 🔥 перенос строк
        }

        // ===== KEYBOARD =====

        keyboardContainer = GridLayout(this).apply {
            columnCount = 8
            rowCount = 4

            useDefaultMargins = true
            alignmentMode = GridLayout.ALIGN_BOUNDS
        }
        topBlock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

        }
        topBlock.addView(topBar)
        topBlock.addView(scoreText)
        topBlock.addView(timerText)
        topBlock.addView(infoPanel)
        topBlock.addView(actionIndicator)

        gameBlock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        gameBlock.addView(wordContainer)
        gameBlock.addView(controls)

        val keyboardHeight = (resources.displayMetrics.heightPixels * 0.35f).toInt()

        gameBlock.addView(
            keyboardContainer,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                keyboardHeight
            )
        )

        bottomBlock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        usedScroll = ScrollView(this).apply {
            isFillViewport = true
            addView(usedWordsContainer)
        }

        bottomBlock.addView(
            usedScroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )        // ===== ROOT =====

        rootLayout.addView(
            topBlock,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                topWeight
            )
        )

        rootLayout.addView(
            gameBlock,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                gameWeight
            )
        )
        rootLayout.addView(
            bottomBlock,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                bottomWeight
            )
        )
        setContentView(rootLayout)

        rootLayout.setOnDragListener { _, event ->
            if (event.action == DragEvent.ACTION_DRAG_ENDED) {
                if (isDraggingFromKeyboard) {
                    isDraggingFromKeyboard = false
                    render()
                }
            }
            true
        }

        setupDelete(delete)
        createKeyboard()
        render()
    }

    // ================= UI =================

    private fun getActionColor(type: GameManager.ActionType): Int {
        return when (type) {
            GameManager.ActionType.REPLACE -> Color.parseColor("#FFD54F") // желтый
            GameManager.ActionType.INSERT_DELETE -> Color.parseColor("#81C784") // зеленый
            GameManager.ActionType.SWAP -> Color.parseColor("#64B5F6") // синий
            else -> Color.WHITE
        }
    }
    private fun render() {
        wordContainer.removeAllViews()

        val state = gameManager.getState()

        scoreText.text =
            "Очки: ${state.score} | Слова: ${state.correctCount} | Рекорд: ${state.bestScore}"

        actionIndicator.text = gameManager.getActionInfo() // ← ДОБАВЛЕНО

        val word = state.word

        for (i in 0..word.size) {

            if (isDraggingFromKeyboard) {
                val plus = TextView(this).apply {
                    text = "+"
                    textSize = 20f
                    gravity = Gravity.CENTER
                    setTextColor(Color.GRAY)

                    layoutParams = LinearLayout.LayoutParams(40, 100)

                    background = GradientDrawable().apply {
                        setColor(Color.parseColor("#EEEEEE"))
                        cornerRadius = 16f
                    }
                }

                plus.setOnDragListener { _, e ->
                    if (e.action == DragEvent.ACTION_DROP) {

                        val data = e.clipData.getItemAt(0).text.toString()

                        if (data.startsWith("k_")) {
                            val letter = data.removePrefix("k_").first()

                            gameManager.insertLetterAt(i, letter)

                            isDraggingFromKeyboard = false
                            wordContainer.post { render() }
                        }
                    }

                    if (e.action == DragEvent.ACTION_DRAG_ENDED) {
                        isDraggingFromKeyboard = false
                    }
                    true
                }

                wordContainer.addView(plus)
            }

            if (i < word.size) {

                val actionType = gameManager.getLastActionType()
                val actionIndex = gameManager.getLastActionIndex()

                val isActionLetter = i == actionIndex

                val letterView = TextView(this).apply {
                    text = word[i].toString()
                    textSize = 20f
                    gravity = Gravity.CENTER
                    setTextColor(Color.BLACK)

                    layoutParams = LinearLayout.LayoutParams(110, 110).apply {
                        setMargins(8, 0, 8, 0)
                    }

                    background = GradientDrawable().apply {
                        setColor(
                            if (isActionLetter) getActionColor(actionType)
                            else Color.parseColor("#F5F5F5")
                        )
                        cornerRadius = 24f
                        setStroke(2, Color.parseColor("#CCCCCC"))
                    }

                    elevation = 6f
                }
                if (isActionLetter) {
                    letterView.scaleX = 0.8f
                    letterView.scaleY = 0.8f
                    letterView.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                }
                if (isActionLetter) {
                    letterView.scaleX = 0.8f
                    letterView.scaleY = 0.8f
                    letterView.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                }

                letterView.setOnTouchListener { v, e ->
                    if (e.action == MotionEvent.ACTION_DOWN) {
                        val data = ClipData.newPlainText("w_$i", "w_$i")
                        v.startDragAndDrop(data, View.DragShadowBuilder(v), null, 0)
                        true
                    } else false
                }

                letterView.setOnDragListener { _, e ->
                    if (e.action == DragEvent.ACTION_DROP) {

                        val data = e.clipData.getItemAt(0).text.toString()

                        when {
                            data.startsWith("k_") -> {
                                val letter = data.removePrefix("k_").first()

                                if (gameManager.isLetterDisabled(letter)) return@setOnDragListener true

                                val current = gameManager.getState().word[i]

                                // 🔥 ЕСЛИ Е — делаем toggle
                                if (letter == 'Е' && (current == 'Е' || current == 'Ё')) {
                                    gameManager.toggleYo(i)
                                } else {
                                    gameManager.replaceLetter(i, letter)
                                }
                            }

                            data.startsWith("w_") -> {
                                val from = data.removePrefix("w_").toInt()
                                gameManager.swapLetters(from, i)
                            }
                        }

                        wordContainer.post { render() }
                    }
                    true
                }

                wordContainer.addView(letterView)
            }
        }

        usedWordsContainer.removeAllViews()

        val currentLen = state.word.size
        val isCompress = gameManager.isCompressMode()

        val sortedWords = state.usedWords.sortedWith(
            compareByDescending<String> {
                isRelevant(it, currentLen, isCompress)
            }.thenBy { it.length }
        )

        for (w in sortedWords) {

            val isTarget = (w == highlightWord)
            val relevant = isRelevant(w, currentLen, isCompress)

            val tv = TextView(this).apply {
                text = w
                setPadding(16, 8, 16, 8)
                setTextColor(Color.WHITE)

                alpha = if (relevant) 1f else 0.35f

                layoutParams = FlexboxLayout.LayoutParams(
                    FlexboxLayout.LayoutParams.WRAP_CONTENT,
                    FlexboxLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(8, 8, 8, 8)
                }

                background = GradientDrawable().apply {
                    setColor(
                        if (isTarget) Color.RED
                        else getColorByLength(w.length)
                    )
                    cornerRadius = 50f
                }
            }

            usedWordsContainer.addView(tv)

            if (isTarget) {
                tv.post {
                    val y = tv.top - usedScroll.height / 2 + tv.height / 2
                    usedScroll.smoothScrollTo(0, y)
                }
            }
        }

        highlightWord = null
    }

    private fun createKeyboard() {
        keyboardContainer.removeAllViews()
        keyboardContainer.minimumHeight =
            (resources.displayMetrics.heightPixels * 0.3f).toInt()
        val letters = listOf(
            "А","Б","В","Г","Д","Е/Ё","Ж","З",
            "И","Й","К","Л","М","Н","О","П",
            "Р","С","Т","У","Ф","Х","Ц","Ч",
            "Ш","Щ","Ъ","Ы","Ь","Э","Ю","Я"
        )

        for (l in letters) {

            val btn = TextView(this).apply {
                text = l
                textSize = 18f
                gravity = Gravity.CENTER
                setTextColor(Color.BLACK)

                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 120
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(8, 8, 8, 8)
                }

                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#F5F5F5"))
                    cornerRadius = 24f
                    setStroke(2, Color.parseColor("#CCCCCC"))
                }

                elevation = 6f

                setOnTouchListener { v, e ->
                    when (e.action) {

                        MotionEvent.ACTION_DOWN -> {

                            val real = if (l == "Е/Ё") {
                                // 🔥 всегда отправляем Е — toggle сделает GameManager
                                'Е'
                            } else l[0]
                            // 🔴 блокировка
                            if (gameManager.isLetterDisabled(real)) return@setOnTouchListener true

                            v.scaleX = 0.92f
                            v.scaleY = 0.92f


                                isDraggingFromKeyboard = true
                                wordContainer.post { render() }


                            val data = ClipData.newPlainText("k_$real", "k_$real")
                            v.startDragAndDrop(data, View.DragShadowBuilder(v), null, 0)

                            true
                        }

                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_CANCEL -> {
                            v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                            true
                        }

                        else -> false
                    }
                }
            }

            val real = if (l == "Е/Ё") 'Е' else l[0]

            if (gameManager.isLetterDisabled(real)) {

                btn.alpha = 1f

                btn.background = GradientDrawable().apply {
                    setColor(Color.BLACK)
                    cornerRadius = 24f
                }

                btn.setTextColor(Color.WHITE)
            }
            keyboardContainer.addView(btn)
        }    }


    // ===== helpers (без изменений)
    private fun highlightUsedWord(word: String) {
        highlightWord = word
        render()
    }    private fun styleButton(btn: Button) {
        btn.setTextColor(Color.WHITE)
        btn.background = GradientDrawable().apply {
            setColor(Color.parseColor("#3F51B5"))
            cornerRadius = 20f
        }
    }
    private fun createTopButton(text: String, icon: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = "$icon $text"
            textSize = 12f
            setTextColor(Color.WHITE)

            background = GradientDrawable().apply {
                setColor(Color.parseColor("#3F51B5"))
                cornerRadius = 20f
            }

            setPadding(16, 8, 16, 8)

            setOnClickListener { onClick() }

            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f   // 🔥 равномерно растягиваются
            ).apply {
                setMargins(6, 6, 6, 6)
            }
        }
    }
    private fun getColorByLength(len: Int): Int {
        return when {
            len <= 3 -> Color.GRAY
            len <= 5 -> Color.parseColor("#4CAF50")
            len == 6 -> Color.parseColor("#2196F3")
            len == 7 -> Color.parseColor("#9C27B0")
            len == 8 -> Color.parseColor("#FF9800")
            else -> Color.parseColor("#F44336")
        }
    }

    private fun shakeView(v: View) {

        val sequence = floatArrayOf(
            0f, 25f, -20f, 15f, -10f, 6f, -3f, 0f
        )

        var i = 0

        fun next() {
            if (i >= sequence.size) return

            v.animate()
                .translationX(sequence[i])
                .setDuration(40)
                .withEndAction {
                    i++
                    next()
                }
        }

        next()
    }
    private fun setupDelete(delete: TextView) {
        delete.setOnDragListener { _, e ->
            if (e.action == DragEvent.ACTION_DROP) {
                val data = e.clipData.getItemAt(0).text.toString()
                if (data.startsWith("w_")) {
                    val index = data.removePrefix("w_").toInt()
                    gameManager.removeLetter(index)
                    wordContainer.post { render() }
                }
            }
            true
        }
    }
    private fun hideMessage(mode: Int) {

        infoPanel.postDelayed({

            val anim = infoPanel.animate()
                .alpha(0f)
                .setDuration(350)

            when (mode) {
                0 -> anim.translationY(-20f)
                1 -> anim.translationY(20f)
                2 -> anim.translationX(100f)
            }

            anim.start()

        }, 2000) // 👈 комфортное время чтения
    }    private fun showRules() {
        AlertDialog.Builder(this)
            .setTitle("Правила")
            .setMessage("Собирай слова. Не повторяйся. Ошибка = -1 очко.")
            .setPositiveButton("OK", null)
            .show()
    }
    private fun showMessage(text: String, color: Int = Color.BLACK) {

        messageAnimMode = (messageAnimMode + 1) % 3

        infoPanel.apply {
            this.text = text
            setTextColor(Color.WHITE)

            background = GradientDrawable().apply {
                setColor(color)
                cornerRadius = 24f
            }

            alpha = 0f
            visibility = View.VISIBLE
        }

        when (messageAnimMode) {

            0 -> {
                infoPanel.translationY = 80f
                infoPanel.translationX = 0f
                infoPanel.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(260)
                    .withEndAction { hideMessage(0) }
                    .start()
            }

            1 -> {
                infoPanel.translationY = -100f
                infoPanel.translationX = 0f
                infoPanel.scaleX = 1.08f
                infoPanel.scaleY = 1.08f

                infoPanel.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(260)
                    .withEndAction { hideMessage(1) }
                    .start()
            }

            2 -> {
                infoPanel.translationX = -250f
                infoPanel.translationY = 0f

                infoPanel.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(260)
                    .withEndAction { hideMessage(2) }
                    .start()
            }

            else -> {
                // fallback (на всякий случай)
                infoPanel.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .withEndAction { hideMessage(0) }
                    .start()
            }
        }
    }    private fun loadUiSettings() {
        val prefs = getSharedPreferences("ui", MODE_PRIVATE)

        topWeight = prefs.getFloat("top", 1f)
        gameWeight = prefs.getFloat("game", 2f)
        bottomWeight = prefs.getFloat("bottom", 1f)
    }

    private fun saveUiSettings() {
        val prefs = getSharedPreferences("ui", MODE_PRIVATE)

        prefs.edit()
            .putFloat("top", topWeight)
            .putFloat("game", gameWeight)
            .putFloat("bottom", bottomWeight)
            .apply()
    }
    private fun startTimer() {

        timerRunnable = object : Runnable {
            override fun run() {
                timeLeft--

                timerText.text = "⏱ $timeLeft сек"

                if (timeLeft > 0) {
                    timerHandler.postDelayed(this, 1000)
                } else {
                    endGame()
                }
            }
        }

        timerHandler.post(timerRunnable!!)
    }
    private fun resetTimer() {
        timerHandler.removeCallbacks(timerRunnable ?: return)
        timeLeft = 180
        timerText.text = "⏱ $timeLeft сек"
        timerStarted = false
    }

    private fun isRelevant(word: String, currentLen: Int, isCompress: Boolean): Boolean {
        val len = word.length

        return if (isCompress) {
            len in 2..3
        } else {
            len == currentLen ||
                    len == currentLen - 1 ||
                    len == currentLen + 1
        }
    }
    private fun endGame() {

        showMessage("⏱ Время вышло!", Color.RED)

        // 🔒 блокируем игру
        isDraggingFromKeyboard = true

        // (опционально) можно ещё:
        keyboardContainer.alpha = 0.4f
    }
    fun refresh() {
        render()
    }
}