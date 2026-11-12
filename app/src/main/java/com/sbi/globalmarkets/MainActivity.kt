package com.sbi.globalmarkets

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 52, 32, 32)
            setBackgroundColor(Color.rgb(12, 20, 35))
        }
        fun heading(value: String, size: Float) {
            content.addView(TextView(this).apply {
                text = value
                textSize = size
                setTextColor(Color.WHITE)
                setPadding(0, 12, 0, 16)
            })
        }
        heading("SBI Global Markets", 28f)
        heading("Scanner internacional • Protótipo offline", 17f)
        heading("Mercados", 22f)
        listOf("Índices — US100, US500", "Forex — EUR/USD, GBP/USD", "Metais — XAU/USD, XAG/USD", "Energia — Petróleo", "Criptomoedas — BTC/USD, ETH/USD").forEach {
            heading("• $it", 17f)
        }
        heading("Tempos gráficos: M5 / M15", 17f)
        heading("Sinais Price Action V18: ainda não conectados.", 16f)
        heading("Cotações reais, alertas e login: pendentes de integração.", 16f)
        heading("Nenhuma ordem é executada automaticamente.", 16f)
        val scroll = ScrollView(this).apply { addView(content) }
        setContentView(scroll)
    }
}
