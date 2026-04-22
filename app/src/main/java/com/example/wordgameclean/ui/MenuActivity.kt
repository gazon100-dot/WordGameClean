package com.example.wordgameclean.ui

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class MenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        // КЛАССИКА
        val classicBtn = Button(this).apply {
            text = "Классический режим"
            setOnClickListener {

                val gm = com.example.wordgameclean.domain.GameManager()
                gm.setMode("classic")

                if (gm.hasSave(this@MenuActivity)) {

                    androidx.appcompat.app.AlertDialog.Builder(this@MenuActivity)
                        .setTitle("Продолжить игру?")
                        .setMessage("Найдена сохранённая игра")
                        .setPositiveButton("Продолжить") { _, _ ->
                            startActivity(Intent(this@MenuActivity, MainActivity::class.java)
                                .putExtra("mode", "classic"))
                        }
                        .setNegativeButton("Новая игра") { _, _ ->
                            gm.clearSave(this@MenuActivity)
                            startActivity(Intent(this@MenuActivity, MainActivity::class.java)
                                .putExtra("mode", "classic"))
                        }
                        .show()

                } else {
                    startActivity(Intent(this@MenuActivity, MainActivity::class.java)
                        .putExtra("mode", "classic"))
                }
            }
        }

        // ТАЙМЕР
        val timerBtn = Button(this).apply {
            text = "Режим на время"
            setOnClickListener {

                val gm = com.example.wordgameclean.domain.GameManager()
                gm.setMode("timer")

                if (gm.hasSave(this@MenuActivity)) {

                    androidx.appcompat.app.AlertDialog.Builder(this@MenuActivity)
                        .setTitle("Продолжить игру?")
                        .setMessage("Найдена сохранённая игра")
                        .setPositiveButton("Продолжить") { _, _ ->
                            startActivity(Intent(this@MenuActivity, MainActivity::class.java)
                                .putExtra("mode", "timer"))
                        }
                        .setNegativeButton("Новая игра") { _, _ ->
                            gm.clearSave(this@MenuActivity)
                            startActivity(Intent(this@MenuActivity, MainActivity::class.java)
                                .putExtra("mode", "timer"))
                        }
                        .show()

                } else {
                    startActivity(Intent(this@MenuActivity, MainActivity::class.java)
                        .putExtra("mode", "timer"))
                }
            }
        }

        // ПРАВИЛА
        val rulesBtn = Button(this).apply {
            text = "Правила"
            setOnClickListener {
                startActivity(Intent(this@MenuActivity, RulesActivity::class.java))
            }
        }

        // НАСТРОЙКИ
        val settingsBtn = Button(this).apply {
            text = "Настройки"
            setOnClickListener {
                startActivity(Intent(this@MenuActivity, SettingsActivity::class.java))
            }
        }

        // ВЫХОД
        val exitBtn = Button(this).apply {
            text = "Выход"
            setOnClickListener {
                finishAffinity()
            }
        }

        // 👇 ВАЖНО: добавляем ВСЕ кнопки
        layout.addView(classicBtn)
        layout.addView(timerBtn)
        layout.addView(rulesBtn)
        layout.addView(settingsBtn)
        layout.addView(exitBtn)

        setContentView(layout)
    }}