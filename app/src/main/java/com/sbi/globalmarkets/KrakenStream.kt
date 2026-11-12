package com.sbi.globalmarkets

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Kraken public spot ticker stream; never represents broker CFD quotes. */
class KrakenStream(private val onPrice: (String, String) -> Unit, private val onStatus: (String) -> Unit) {
    private val client = OkHttpClient.Builder().pingInterval(20, TimeUnit.SECONDS).build()
    private var socket: WebSocket? = null
    fun start() {
        socket = client.newWebSocket(Request.Builder().url("wss://ws.kraken.com/v2").build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    onStatus("Kraken WebSocket conectado")
                    webSocket.send("""{"method":"subscribe","params":{"channel":"ticker","symbol":["BTC/USD","ETH/USD"],"snapshot":true}}""")
                }
                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val obj = JSONObject(text)
                        if (obj.optString("channel") != "ticker") return
                        val data = obj.optJSONArray("data") ?: return
                        for (i in 0 until data.length()) {
                            val tick = data.getJSONObject(i)
                            val symbol = tick.optString("symbol")
                            val last = tick.optDouble("last", Double.NaN)
                            if ((symbol == "BTC/USD" || symbol == "ETH/USD") && last.isFinite())
                                onPrice(symbol, String.format(java.util.Locale.US, "$%,.2f", last))
                        }
                    } catch (_: Exception) { }
                }
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    onStatus("WebSocket desconectado; consulta HTTP continua disponível")
                }
            })
    }
    fun stop() { socket?.close(1000, "App paused"); socket = null }
}
