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
    private var krakenStream: KrakenStream? = null
    private var streamStatus = "WebSocket: conectando..."
    private val refreshTask = object : Runnable {
        override fun run() {
            if (!isFinishing && !isDestroyed) {
                refreshCrypto()
                refreshHandler.postDelayed(this, 60_000L)
            }
        }
    }
    private lateinit var root: LinearLayout
    private var timeframe = "M5"
    private var category = "Todos"
    private var section = "Mercados"
    private var chartSymbol = "BTC/USD"
    private var candles: List<String> = emptyList()
    private var candleStatus = "Carregue candles reais"
    private var cryptoQuotes: Map<String, String> = emptyMap()
    private var marketQuotes: Map<String, String> = emptyMap()
    private val yahooSymbols = linkedMapOf("US100" to "%5ENDX", "US500" to "%5EGSPC", "EUR/USD" to "EURUSD%3DX", "GBP/USD" to "GBPUSD%3DX", "XAU/USD" to "GC%3DF", "XAG/USD" to "SI%3DF", "Petróleo" to "CL%3DF")
    private var quoteStatus = "Cotações de cripto: toque em Atualizar"
    private var updatedAt = ""
    private val markets = listOf(
        Triple("Índices", "US100", "Nasdaq 100 • provedor pendente"),
        Triple("Índices", "US500", "S&P 500 • provedor pendente"),
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

    override fun onStart() {
        super.onStart()
        krakenStream = KrakenStream(
            { symbol, price -> runOnUiThread {
                cryptoQuotes = cryptoQuotes + (symbol to price)
                if (section == "Mercados" && (category == "Todos" || category == "Cripto")) draw()
            } },
            { status -> runOnUiThread { streamStatus = status; if (section == "Mercados") draw() } }
        ).also { it.start() }
    }
    override fun onStop() {
        krakenStream?.stop()
        krakenStream = null
        super.onStop()
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
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(root) }
        setContentView(scroll)
        root.addView(label("SBI Global Markets", 27f, Color.WHITE, true))
        root.addView(label("Scanner internacional • V0.2", 14f, muted))
        root.addView(label("Cripto: WebSocket Kraken • demais mercados: consultas indicativas", 12f, accent))
        root.addView(label(streamStatus, 12f, muted))
        root.addView(label("Acompanhamento de mercados • Sem execução de ordens", 13f, muted))
        listOf(listOf("Mercados", "Gráficos", "Scanner"), listOf("Alertas", "Configurações")).forEach { group ->
            val nav = row()
            group.forEach { name ->
                nav.addView(action(name, section == name) { section = name; draw() },
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            }
            root.addView(nav)
        }
        if (section == "Gráficos") {
            root.addView(label("Candles reais • Kraken Spot", 20f, Color.WHITE, true))
            root.addView(label("BTC/USD e ETH/USD; demais ativos aguardam API OHLC.", 13f, muted))
            val assets = row()
            listOf("BTC/USD", "ETH/USD").forEach { asset ->
                assets.addView(action(asset, asset == chartSymbol) { chartSymbol = asset; candles = emptyList(); draw() }, LinearLayout.LayoutParams(0, -2, 1f))
            }
            root.addView(assets)
            val periods = row()
            listOf("M5", "M15").forEach { period ->
                periods.addView(action(period, period == timeframe) { timeframe = period; candles = emptyList(); draw() }, LinearLayout.LayoutParams(0, -2, 1f))
            }
            root.addView(periods)
            root.addView(action("Carregar candles") { loadCandles() })
            root.addView(label(candleStatus, 13f, muted))
            candles.forEach { root.addView(label(it, 13f)) }
            root.addView(label("Somente candles fechados; não são sinais.", 12f, muted))
            return
        }
        if (section != "Mercados") {
            val details = when (section) {
                "Gráficos" -> "Candles M5/M15: aguardando dados OHLC reais."
                "Scanner" -> "Price Action V18: motor original ainda não integrado. Sem sinais fictícios."
                "Alertas" -> "Notificações Android: pendentes da validação dos sinais."
                else -> "Versão 0.3. Kraken WebSocket para cripto; outros provedores pendentes."
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
        listOf(listOf("Todos", "Índices"), listOf("Forex", "Metais", "Energia"), listOf("Cripto")).forEach { group ->
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
                item.addView(label(marketQuotes[market.second] ?: "Cotação indisponível", 17f, accent, true))
                item.addView(label("Fonte Yahoo Finance não oficial • pode haver atraso", 12f, muted))
            }
            item.addView(label("Sinal $timeframe: indisponível (motor V18 pendente)", 12f, muted))
            root.addView(item, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10 })
        }
        root.addView(action("Atualizar cotações online") { refreshCrypto() })
        root.addView(label(quoteStatus, 13f, muted))
        if (updatedAt.isNotEmpty()) root.addView(label("Última consulta: $updatedAt", 12f, muted))
        root.addView(label("Aviso: preço indicativo de fonte externa. Sem dados de candles ou sinais operacionais.", 12f, muted))
    }

    private fun refreshCrypto() {
        if (polling) return
        polling = true
        quoteStatus = "Consultando fontes online..."
        draw()
        worker.execute {
            var result: Map<String, String>? = null
            val yahooResult = mutableMapOf<String, String>()
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
            val targets = yahooSymbols.filter { entry ->
                category == "Todos" || markets.any { it.second == entry.key && it.first == category }
            }
            for ((symbol, ticker) in targets) {
                try {
                    val conn = URL("https://query1.finance.yahoo.com/v8/finance/chart/$ticker?interval=5m&range=1d").openConnection() as HttpURLConnection
                    conn.connectTimeout = 7000
                    conn.readTimeout = 7000
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                    try {
                        if (conn.responseCode != 200) continue
                        val data = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                        val chart = data.getJSONObject("chart").getJSONArray("result").getJSONObject(0)
                        val meta = chart.getJSONObject("meta")
                        val price = meta.optDouble("regularMarketPrice", Double.NaN)
                        if (!price.isNaN()) {
                            val currency = meta.optString("currency", "")
                            yahooResult[symbol] = String.format(Locale.US, "%,.3f %s", price, currency)
                        }
                    } finally { conn.disconnect() }
                } catch (_: Exception) { /* display unavailable, never invent a quote */ }
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
                marketQuotes = marketQuotes + yahooResult
                polling = false
                draw()
            }
        }
    }

    private fun loadCandles() {
        val symbol = chartSymbol
        val period = timeframe
        candleStatus = "Carregando..."
        draw()
        worker.execute {
            val values = mutableListOf<String>()
            var status = "Falha ao consultar"
            try {
                val pair = if (symbol == "BTC/USD") "XBTUSD" else "ETHUSD"
                val interval = if (period == "M5") 5 else 15
                val conn = URL("https://api.kraken.com/0/public/OHLC?pair=$pair&interval=$interval").openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                try {
                    if (conn.responseCode != 200) throw IllegalStateException("HTTP ${conn.responseCode}")
                    val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                    if (json.getJSONArray("error").length() > 0) throw IllegalStateException("API indisponível")
                    val result = json.getJSONObject("result")
                    val key = result.keys().asSequence().first { it != "last" }
                    val bars = result.getJSONArray(key)
                    for (i in maxOf(0, bars.length() - 11) until bars.length() - 1) {
                        val bar = bars.getJSONArray(i)
                        val time = java.text.SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(java.util.Date(bar.getLong(0) * 1000))
                        values.add("$time O:${bar.getString(1)} H:${bar.getString(2)} L:${bar.getString(3)} C:${bar.getString(4)}")
                    }
                    status = "${values.size} candles fechados • $symbol • $period"
                } finally { conn.disconnect() }
            } catch (e: Exception) { status = "Indisponível: ${e.message}" }
            runOnUiThread {
                if (!isFinishing && !isDestroyed && chartSymbol == symbol && timeframe == period) {
                    candles = values
                    candleStatus = status
                    if (section == "Gráficos") draw()
                }
            }
        }
    }
    override fun onDestroy() {
        refreshHandler.removeCallbacks(refreshTask)
        worker.shutdownNow()
        super.onDestroy()
    }
}
