package com.example.wordgameclean.ui

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var topPreview: LinearLayout
    private lateinit var gamePreview: LinearLayout
    private lateinit var bottomPreview: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("ui", MODE_PRIVATE)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }

        val previewContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        // ===== BLOCKS =====

        topPreview = createBlock(
            "СЧЁТ / КНОПКИ",
            "#90CAF9",
            prefs.getFloat("top", 1f)
        )

        gamePreview = createBlock(
            "СЛОВО + КЛАВИАТУРА",
            "#A5D6A7",
            prefs.getFloat("game", 2f)
        )

        bottomPreview = createBlock(
            "ИСПОЛЬЗОВАННЫЕ СЛОВА",
            "#FFCC80",
            prefs.getFloat("bottom", 1f)
        )

        // ===== DIVIDERS =====

        val divider1 = createDivider()
        val divider2 = createDivider()

        // ===== LAYOUT =====

        previewContainer.addView(topPreview)
        previewContainer.addView(divider1)
        previewContainer.addView(gamePreview)
        previewContainer.addView(divider2)
        previewContainer.addView(bottomPreview)

        // ===== RESIZE LOGIC =====

        attachResizeBetween(divider1, topPreview, gamePreview)
        attachResizeBetween(divider2, gamePreview, bottomPreview)

        // ===== SAVE BUTTON =====

        val saveBtn = Button(this).apply {
            text = "СОХРАНИТЬ"

            setOnClickListener {

                val top = (topPreview.layoutParams as LinearLayout.LayoutParams).weight
                val game = (gamePreview.layoutParams as LinearLayout.LayoutParams).weight
                val bottom = (bottomPreview.layoutParams as LinearLayout.LayoutParams).weight

                prefs.edit()
                    .putFloat("top", top)
                    .putFloat("game", game)
                    .putFloat("bottom", bottom)
                    .apply()

                Toast.makeText(context, "Сохранено", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        root.addView(previewContainer)
        root.addView(saveBtn)

        setContentView(root)
    }

    // ================= BLOCK =================

    private fun createBlock(text: String, color: String, weight: Float): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(color))

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                weight
            )

            addView(TextView(context).apply {
                this.text = text
                gravity = Gravity.CENTER
                textSize = 16f
                setPadding(0, 40, 0, 40)
            })
        }
    }

    // ================= DIVIDER =================

    private fun createDivider(): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                24
            )

            setBackgroundColor(Color.DKGRAY)
            alpha = 0.5f
        }
    }

    // ================= RESIZE =================

    private fun attachResizeBetween(
        divider: View,
        upper: View,
        lower: View
    ) {

        var startY = 0f

        divider.setOnTouchListener { _, event ->

            when (event.action) {

                MotionEvent.ACTION_DOWN -> {
                    startY = event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {

                    val diff = (event.rawY - startY) / 600f

                    val upperParams = upper.layoutParams as LinearLayout.LayoutParams
                    val lowerParams = lower.layoutParams as LinearLayout.LayoutParams

                    val newUpper = upperParams.weight + diff
                    val newLower = lowerParams.weight - diff

                    // 🔒 ограничения
                    if (newUpper < 0.7f || newLower < 0.7f) {
                        return@setOnTouchListener true
                    }

                    upperParams.weight = newUpper
                    lowerParams.weight = newLower

                    upper.layoutParams = upperParams
                    lower.layoutParams = lowerParams

                    startY = event.rawY

                    true
                }

                else -> false
            }
        }
    }
}