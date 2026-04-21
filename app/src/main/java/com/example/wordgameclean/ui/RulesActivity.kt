package com.example.wordgameclean.ui

import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class RulesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }

        val text = TextView(this).apply {
            textSize = 18f
            text = """
                🧠 Правила игры:
                
                • Собирай слова из букв
                • Нельзя повторять слова
                • Ошибка = -1 очко
                • Можно:
                  - менять буквы
                  - добавлять / удалять
                  - переставлять
                
                Удачи 🙂
            """.trimIndent()
        }

        layout.addView(text)

        setContentView(layout)
    }
}