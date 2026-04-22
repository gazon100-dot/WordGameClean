package com.example.wordgameclean.ui

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.wordgameclean.domain.GameManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var topPreview: LinearLayout
    private lateinit var gamePreview: LinearLayout
    private lateinit var bottomPreview: LinearLayout

    private val MIN_WEIGHT = 0.5f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("ui", MODE_PRIVATE)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        // ===== TOP =====

        topPreview = createTopBlock(prefs.getFloat("top", 1f))

        // ===== GAME (через GameView) =====

        val fakeManager = GameManager().apply {
            setMode("classic")
            setDictionary(setOf("кот", "дом"))
            init(this@SettingsActivity)
        }

        val gameView = GameView(this, fakeManager, isPreview = true)

        gamePreview = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                2f
            )

            setBackgroundColor(Color.parseColor("#A5D6A7"))
            alpha = 0.95f

            addView(
                gameView.root,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            )        }

        // ===== BOTTOM =====

        bottomPreview = createBottomBlock(prefs.getFloat("bottom", 1f))

        // ===== DIVIDER =====

        val divider = createDivider()

        // ===== LAYOUT =====

        container.addView(topPreview)
        container.addView(divider)
        container.addView(gamePreview)
        container.addView(bottomPreview)

        attachResize(divider)

        // ===== SAVE =====

        val saveBtn = Button(this).apply {
            text = "СОХРАНИТЬ"
            setOnClickListener {

                val top = (topPreview.layoutParams as LinearLayout.LayoutParams).weight
                val bottom = (bottomPreview.layoutParams as LinearLayout.LayoutParams).weight

                prefs.edit()
                    .putFloat("top", top)
                    .putFloat("bottom", bottom)
                    .apply()

                Toast.makeText(context, "Сохранено", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        root.addView(container)
        root.addView(saveBtn)

        setContentView(root)
    }

    // ================= TOP =================

    private fun createTopBlock(weight: Float): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#90CAF9"))

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                weight
            )

            addView(TextView(context).apply {
                text = "Очки: 10 | Слова: 3\n[Меню] [Сброс]"
                gravity = Gravity.CENTER
                textSize = 16f
                setPadding(0, 40, 0, 40)
            })
        }
    }

    // ================= BOTTOM =================

    private fun createBottomBlock(weight: Float): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#FFCC80"))

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                weight
            )

            addView(TextView(context).apply {
                text = "кот • дом • лес"
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
                20
            )
            setBackgroundColor(Color.DKGRAY)
            alpha = 0.5f
        }
    }

    // ================= RESIZE =================

    private fun attachResize(divider: View) {

        var startY = 0f

        divider.setOnTouchListener { _, event ->

            when (event.action) {

                MotionEvent.ACTION_DOWN -> {
                    startY = event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {

                    val diff = (event.rawY - startY) / 300f

                    val topParams = topPreview.layoutParams as LinearLayout.LayoutParams
                    val bottomParams = bottomPreview.layoutParams as LinearLayout.LayoutParams

                    val newTop = topParams.weight + diff
                    val newBottom = bottomParams.weight - diff

                    if (newTop < MIN_WEIGHT || newBottom < MIN_WEIGHT) {
                        return@setOnTouchListener true
                    }

                    topParams.weight = newTop
                    bottomParams.weight = newBottom

                    topPreview.layoutParams = topParams
                    bottomPreview.layoutParams = bottomParams

                    startY = event.rawY

                    true
                }

                else -> false
            }
        }
    }
}