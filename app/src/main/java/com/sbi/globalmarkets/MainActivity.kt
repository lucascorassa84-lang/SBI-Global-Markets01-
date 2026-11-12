package com.sbi.globalmarkets

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val bg = Color.rgb(11, 18, 32)
    private val panel = Color.rgb(24, 35, 55)
    private val muted = Color.rgb(170, 185, 204)
    private val accent = Color.rgb(69, 207, 160)
    private val worker = Executors.newSingleThreadExecutor()
    private val refreshHandler = Handler(Looper.getMainLooper())
    private var polling = false
    private val refreshTask = object : Runnable {
        override fun run() {
            if (!isFinishing && !isDestroyed) {
                refreshCrypto()
                refreshHandler.postDelayed(this, 30_000L)
            }
        }
    }
    private lateinit var root: LinearLayout
    private var timeframe = "M5"
    private var category = "Todos"
    private var section = "Mercados"
    private var cryptoQuotes: Map<String, String> = emptyMap()
    private var quoteStatus = "Cotações de cripto: toque em Atualizar"
    private var updatedAt = ""
    private val markets = listOf(
        Triple("Índices", "US100", "Nasdaq 100 • provedor pendente"),
        Triple("Índices", "US500", "S&P 500 • provedor pendente"),
        Triple("Brasil", "WIN", "Mini-índice futuro B3 • contrato vigente • dados pendentes"),
        Triple("Brasil", "WDO", "Mini-dólar futuro B3 • contrato vigente • dados pendentes"),
        Triple("Forex", "EUR/USD", "Euro / Dólar • provedor pendente"),
        Triple("Forex", "GBP/USD", "Libra / Dólar • provedor pendente"),
        Triple("Metais", "XAU/USD", "Ouro • provedor pendente"),
        Triple("Metais", "XAG/USD", "Prata • provedor pendente"),
        Triple("Energia", "Petróleo", "Petróleo • provedor pendente"),
        Triple("Cripto", "BTC/USD", "Bitcoin • Kraken"),
        Triple("Cripto", "ETH/USD", "Ethereum • Kraken")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        draw()
        refreshHandler.post(refreshTask)
    }

    private fun label(value: String, size: Float = 16f, color: Int = Color.WHITE, bold: Boolean = false): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
            setPadding(0, 6, 0, 6)
        }
    }

    private fun action(text: String, selected: Boolean = false, click: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            isAllCaps = false
            setTextColor(if (selected) bg else Color.WHITE)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(if (selected) accent else panel))
            setOnClickListener { click() }
        }
    }

    private fun row(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun draw() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(22, 26, 22, 32)
        }
        val scroll = ScrollView(this).apply { fillViewport = true; addView(root) }
        setContentView(scroll)
        root.addView(label("SBI Global Markets", 27f, Color.WHITE, true))
        root.addView(label("Scanner internacional • V0.2", 14f, muted))
        root.addView(label("BTC/USD e ETH/USD: atualização automática a cada 30 segundos", 12f, accent))
        root.addView(label("Brasil: WIN e WDO cadastrados; sem cotação B3 conectada", 12f, muted))
        root.addView(label("Acompanhamento de mercados • Sem execução de ordens", 13f, muted))
        listOf(listOf("Mercados", "Gráficos", "Scanner"), listOf("Alertas", "Configurações")).forEach { group ->
            val nav = row()
            group.forEach { name ->
                nav.addView(action(name, section == name) { section = name; draw() },
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            }
            root.addView(nav)
        }
        if (section != "Mercados") {
            val details = when (section) {
                "Gráficos" -> "Candles M5/M15: aguardando dados OHLC reais."
                "Scanner" -> "Price Action V18: motor original ainda não integrado. Sem sinais fictícios."
                "Alertas" -> "Notificações Android: pendentes da validação dos sinais."
                else -> "Versão 0.2. Kraken para cripto; outros provedores pendentes."
            }
            root.addView(label(section, 22f, Color.WHITE, true))
            root.addView(label(details, 16f, muted))
            return
        }

        root.addView(label("Tempo gráfico", 18f, Color.WHITE, true))
        val times = row()
        listOf("M5", "M15").forEach { t ->
            times.addView(action(t, t == timeframe) { timeframe = t; draw() },
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        root.addView(times)
        root.addView(label("Selecione o mercado", 18f, Color.WHITE, true))
        listOf(listOf("Todos", "Índices", "Brasil"), listOf("Forex", "Metais", "Energia"), listOf("Cripto")).forEach { group ->
            val r = row()
            group.forEach { name ->
                r.addView(action(name, category == name) { category = name; draw() },
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            }
            root.addView(r)
        }
        root.addView(label("Ativos", 20f, Color.WHITE, true))
        markets.filter { category == "Todos" || it.first == category }.forEach { market ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(panel)
                setPadding(18, 14, 18, 14)
            }
            item.addView(label(market.second, 19f, Color.WHITE, true))
            item.addView(label(market.third, 13f, muted))
            if (market.first == "Cripto") {
                item.addView(label(cryptoQuotes[market.second] ?: "Preço indisponível", 18f, accent, true))
            } else {
                item.addView(label("Cotação não conectada", 14f, muted))
            }
            item.addView(label("Sinal $timeframe: indisponível (motor V18 pendente)", 12f, muted))
            root.addView(item, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10 })
        }
        root.addView(action("Atualizar cotações de criptomoedas") { refreshCrypto() })
        root.addView(label(quoteStatus, 13f, muted))
        if (updatedAt.isNotEmpty()) root.addView(label("Última consulta: $updatedAt", 12f, muted))
        root.addView(label("Aviso: preço indicativo de fonte externa. Sem dados de candles ou sinais operacionais.", 12f, muted))
    }

    private fun refreshCrypto() {
        if (polling) return
        polling = true
        quoteStatus = "Consultando Kraken..."
        draw()
        worker.execute {
            var result: Map<String, String>? = null
            var error = "Falha na consulta"
            try {
                val connection = URL("https://api.kraken.com/0/public/Ticker?pair=XBTUSD,ETHUSD").openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.requestMethod = "GET"
                try {
                    if (connection.responseCode != 200) throw IllegalStateException("HTTP ${connection.responseCode}")
                    val payload = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(payload)
                    if (json.getJSONArray("error").length() > 0) throw IllegalStateException("Resposta da fonte indisponível")
                    val tickers = json.getJSONObject("result")
                    val map = mutableMapOf<String, String>()
                    val keys = tickers.keys()
                    val money = NumberFormat.getCurrencyInstance(Locale.US)
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val price = tickers.getJSONObject(key).getJSONArray("c").getString(0).toDouble()
                        if (key.contains("XBT") || key.contains("BTC")) map["BTC/USD"] = money.format(price)
                        if (key.contains("ETH")) map["ETH/USD"] = money.format(price)
                    }
                    if (map.isEmpty()) throw IllegalStateException("Nenhuma cotação recebida")
                    result = map
                } finally { connection.disconnect() }
            } catch (ex: Exception) {
                error = "Consulta indisponível: ${ex.message ?: "verifique a conexão"}"
            }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (result != null) {
                    cryptoQuotes = result!!
                    quoteStatus = "Preços spot consultados na Kraken (USD)"
                    updatedAt = java.text.SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(java.util.Date())
                } else {
                    quoteStatus = error
                }
                polling = false
                draw()
            }
        }
    }

    override fun onDestroy() {
        refreshHandler.removeCallbacks(refreshTask)
        worker.shutdownNow()
        super.onDestroy()
    }
}
