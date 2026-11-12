package com.sbi.globalmarkets

/** Port of uploaded BlackArrow PriceAction V18.2, using closed OHLC bars only.
 * The NTSL source remains unchanged. Index 0 is oldest, last is newest CLOSED bar.
 */
object PriceActionV18 {
    data class Candle(val time: Long, val open: Double, val high: Double, val low: Double, val close: Double)
    enum class Signal { BUY, SELL, WAIT, NONE }
    fun evaluate(bars: List<Candle>, previousBuy: Boolean = false,
                 previousSell: Boolean = false, previousWait: Boolean = false): Signal {
        if (bars.size < 13) return Signal.NONE
        fun c(i: Int) = bars[bars.size - i]
        fun body(i: Int) = kotlin.math.abs(c(i).close - c(i).open)
        val a = c(1); val b = c(2)
        val corpo = body(1); val corpoAnt = body(2)
        val faixa = a.high - a.low; val faixaAnt = b.high - b.low
        val pavioSup = a.high - maxOf(a.open, a.close)
        val pavioInf = minOf(a.open, a.close) - a.low
        val alta = a.close > a.open; val baixa = a.close < a.open
        val altaAnt = b.close > b.open; val baixaAnt = b.close < b.open
        val referenciaCorpo = (2..7).sumOf { body(it) } / 6.0
        val fechamentoRel = if (faixa > 0) (a.close - a.low) / faixa else 0.5
        val engAlta = alta && baixaAnt && a.open <= b.close && a.close >= b.open && corpo > corpoAnt
        val engBaixa = baixa && altaAnt && a.open >= b.close && a.close <= b.open && corpo > corpoAnt
        val martelo = faixa > 0 && pavioInf >= corpo * 2 && pavioSup <= corpo && fechamentoRel >= 0.60
        val estrela = faixa > 0 && pavioSup >= corpo * 2 && pavioInf <= corpo && fechamentoRel <= 0.40
        val insideAlta = a.high < b.high && a.low > b.low && alta
        val insideBaixa = a.high < b.high && a.low > b.low && baixa
        val ignoradaAlta = baixa && corpo < referenciaCorpo * 0.65 && c(2).close > c(2).open && c(3).close > c(3).open
        val ignoradaBaixa = alta && corpo < referenciaCorpo * 0.65 && c(2).close < c(2).open && c(3).close < c(3).open
        val fundoRecente = (3..5).all { b.low <= c(it).low }
        val topoRecente = (3..5).all { b.high >= c(it).high }
        val estruturaAlta = b.close > c(5).close && b.low > c(6).low
        val estruturaBaixa = b.close < c(5).close && b.high < c(6).high
        val lateral = b.high <= c(5).high && b.low >= c(5).low && c(3).high <= c(6).high && c(3).low >= c(6).low
        val rejeicaoFundo = fundoRecente && faixaAnt > 0 && minOf(b.open, b.close) - b.low >= faixaAnt * 0.35
        val rejeicaoTopo = topoRecente && faixaAnt > 0 && b.high - maxOf(b.open, b.close) >= faixaAnt * 0.35
        val rompAlta = alta && a.close > b.high
        val rompBaixa = baixa && a.close < b.low
        val confirmacaoFundo = rejeicaoFundo && rompAlta && a.low > b.low
        val confirmacaoTopo = rejeicaoTopo && rompBaixa && a.high < b.high
        val forcaAlta = alta && corpo >= referenciaCorpo * 1.15 && fechamentoRel >= 0.75
        val forcaBaixa = baixa && corpo >= referenciaCorpo * 1.15 && fechamentoRel <= 0.25
        val pausaInside = b.high < c(3).high && b.low > c(3).low
        val continuacaoAlta = estruturaAlta && rompAlta && (pausaInside || (b.close < b.open && corpoAnt < referenciaCorpo * 0.65))
        val continuacaoBaixa = estruturaBaixa && rompBaixa && (pausaInside || (b.close > b.open && corpoAnt < referenciaCorpo * 0.65))
        val tendenciaAltaForte = c(2).close > c(4).close && c(4).close > c(6).close && c(2).low > c(5).low && c(2).high > c(5).high
        val tendenciaBaixaForte = c(2).close < c(4).close && c(4).close < c(6).close && c(2).high < c(5).high && c(2).low < c(5).low
        val maximaEstrutural = (2..6).maxOf { c(it).high }
        val minimaEstrutural = (2..6).minOf { c(it).low }
        val reversaoAltaConfirmada = forcaAlta && a.close > maximaEstrutural
        val reversaoBaixaConfirmada = forcaBaixa && a.close < minimaEstrutural
        val engolfoCompra = engAlta && (fundoRecente || estruturaAlta) && a.close > b.high
        val engolfoVenda = engBaixa && (topoRecente || estruturaBaixa) && a.close < b.low
        val marteloCompra = rejeicaoFundo && rompAlta && a.low > b.low
        val estrelaVenda = rejeicaoTopo && rompBaixa && a.high < b.high
        val insideCompra = pausaInside && estruturaAlta && alta && a.close > c(3).high
        val insideVenda = pausaInside && estruturaBaixa && baixa && a.close < c(3).low
        val ignoradaCompra = estruturaAlta && b.close < b.open && corpoAnt < referenciaCorpo * 0.65 && alta && a.close > b.high
        val ignoradaVenda = estruturaBaixa && b.close > b.open && corpoAnt < referenciaCorpo * 0.65 && baixa && a.close < b.low
        val forcaCompra = forcaAlta && estruturaAlta && rompAlta
        val forcaVenda = forcaBaixa && estruturaBaixa && rompBaixa
        var compraBase = confirmacaoFundo || (!lateral && (engolfoCompra || marteloCompra || insideCompra || ignoradaCompra || forcaCompra || continuacaoAlta))
        var vendaBase = confirmacaoTopo || (!lateral && (engolfoVenda || estrelaVenda || insideVenda || ignoradaVenda || forcaVenda || continuacaoBaixa))
        if (tendenciaBaixaForte && !reversaoAltaConfirmada) compraBase = false
        if (tendenciaAltaForte && !reversaoBaixaConfirmada) vendaBase = false
        val preparacaoAlta = (martelo && a.low <= b.low) || (insideAlta && estruturaAlta) || ignoradaAlta
        val preparacaoBaixa = (estrela && a.high >= b.high) || (insideBaixa && estruturaBaixa) || ignoradaBaixa
        val esperaBase = (preparacaoAlta || preparacaoBaixa) && !compraBase && !vendaBase
        return when {
            compraBase && !vendaBase && !previousBuy -> Signal.BUY
            vendaBase && !compraBase && !previousSell -> Signal.SELL
            esperaBase && !previousWait -> Signal.WAIT
            else -> Signal.NONE
        }
    }
    fun evaluateLatest(bars: List<Candle>): Signal {
        if (bars.size < 14) return Signal.NONE
        // Reproduce NTSL previous-state gating from the preceding closed bar.
        fun baseSignalAt(n: Int) = evaluate(bars.take(n))
        val last = baseSignalAt(bars.size)
        val prior = baseSignalAt(bars.size - 1)
        return if (last == prior && last != Signal.NONE) Signal.NONE else last
    }
}
