package com.example.wordgameclean.ui

import android.content.ClipData
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.widget.*
import com.example.wordgameclean.domain.GameManager

class GameView(
    private val context: android.content.Context,
    private val gameManager: GameManager,
    private val isPreview: Boolean = false
) {

    val root = LinearLayout(context)

    private val wordContainer = LinearLayout(context)
    private val keyboardContainer = GridLayout(context)
    private val controls = LinearLayout(context)

    init {
        setup()
        render()
        createKeyboard()
    }

    private fun setup() {

        root.orientation = LinearLayout.VERTICAL

        // ===== WORD =====
        wordContainer.orientation = LinearLayout.HORIZONTAL
        wordContainer.gravity = Gravity.CENTER

        // ===== CONTROLS =====
        controls.gravity = Gravity.CENTER
        controls.addView(createBtn("ОТМЕНА"))
        controls.addView(createBtn("ГОТОВО"))

        val topPart = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(wordContainer)
            addView(controls)
        }

        // ===== KEYBOARD =====
        keyboardContainer.columnCount = 8
        keyboardContainer.rowCount = 4

        // верх (слово + кнопки)
        root.addView(
            topPart,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // клавиатура (растягивается)
        root.addView(
            keyboardContainer,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
    }

    private fun createBtn(text: String): Button {
        return Button(context).apply { this.text = text }
    }

    // ================= WORD =================

    fun render() {
        wordContainer.removeAllViews()

        val word = if (isPreview) {
            listOf('К','О','Т')
        } else {
            gameManager.getState().word
        }

        val currentWord = word.joinToString("")
        val isUsed = !isPreview &&
                currentWord.length > 2 &&
                gameManager.getState().usedWords.contains(currentWord)

        word.forEachIndexed { index, c ->

            val tv = TextView(context).apply {
                text = c.toString()
                textSize = 20f
                gravity = Gravity.CENTER

                layoutParams = LinearLayout.LayoutParams(
                    110,
                    110
                ).apply {
                    setMargins(8, 0, 8, 0)
                }

                background = createLetterBackground(isUsed)
            }

            if (!isPreview) {
                tv.setOnTouchListener { v, e ->
                    if (e.action == MotionEvent.ACTION_DOWN) {
                        val data = ClipData.newPlainText("w_$index", "w_$index")
                        v.startDragAndDrop(data, View.DragShadowBuilder(v), null, 0)
                        true
                    } else false
                }
            }

            wordContainer.addView(tv)
        }
    }

    // ================= REAL-TIME HIGHLIGHT =================

    private fun updateWordHighlight() {

        if (isPreview) return

        val word = gameManager.getState().word
        val currentWord = word.joinToString("")

        val isUsed = currentWord.length > 2 &&
                gameManager.getState().usedWords.contains(currentWord)

        for (i in 0 until wordContainer.childCount) {

            val tv = wordContainer.getChildAt(i) as TextView

            tv.background = createLetterBackground(isUsed)
        }
    }

    private fun createLetterBackground(isUsed: Boolean): GradientDrawable {
        return GradientDrawable().apply {

            if (isUsed) {
                setColor(Color.parseColor("#FFE0E0"))
                setStroke(3, Color.parseColor("#FF5252"))
            } else {
                setColor(Color.parseColor("#F5F5F5"))
                setStroke(2, Color.parseColor("#CCCCCC"))
            }

            cornerRadius = 24f
        }
    }

    // ================= KEYBOARD =================

    fun createKeyboard() {

        keyboardContainer.removeAllViews()

        val letters = listOf(
            "А","Б","В","Г","Д","Е","Ж","З",
            "И","Й","К","Л","М","Н","О","П",
            "Р","С","Т","У","Ф","Х","Ц","Ч",
            "Ш","Щ","Ъ","Ы","Ь","Э","Ю","Я"
        )

        for (l in letters) {

            val btn = TextView(context).apply {
                text = l
                textSize = 16f
                gravity = Gravity.CENTER

                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 0
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(6, 6, 6, 6)
                }

                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#F5F5F5"))
                    cornerRadius = 20f
                }
            }

            if (!isPreview) {
                btn.setOnTouchListener { v, e ->
                    if (e.action == MotionEvent.ACTION_DOWN) {

                        val data = ClipData.newPlainText("k_$l", "k_$l")
                        v.startDragAndDrop(data, View.DragShadowBuilder(v), null, 0)

                        // 🔥 обновляем подсветку во время игры
                        updateWordHighlight()

                        true
                    } else false
                }
            }

            keyboardContainer.addView(btn)
        }
    }

    // 🔥 ВАЖНО: вызывай это извне после изменений слова
    fun refresh() {
        render()
        updateWordHighlight()
    }
}