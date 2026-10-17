package io.github.jvhubert.semreels

/**
 * Lê JSON e devolve Map, List, String, Long, Double, Boolean ou null.
 * Feito à mão pelo mesmo motivo do [Json]: o org.json não roda nos testes do PC.
 */
class LeitorJson private constructor(private val s: String) {
    private var i = 0

    companion object {
        fun ler(texto: String): Any? {
            val l = LeitorJson(texto)
            val v = l.valor()
            l.espacos()
            require(l.i == texto.length) { "sobrou texto na posição ${l.i}" }
            return v
        }
    }

    private fun valor(): Any? {
        espacos()
        require(i < s.length) { "JSON acabou antes da hora" }
        return when (val c = s[i]) {
            '{' -> objeto()
            '[' -> lista()
            '"' -> texto()
            't' -> palavra("true", true)
            'f' -> palavra("false", false)
            'n' -> palavra("null", null)
            else -> if (c == '-' || c.isDigit()) numero() else error("caractere inesperado '$c' na posição $i")
        }
    }

    private fun objeto(): Map<String, Any?> {
        val m = LinkedHashMap<String, Any?>()
        i++
        espacos()
        if (s[i] == '}') { i++; return m }
        while (true) {
            espacos()
            val k = texto()
            espacos()
            esperar(':')
            m[k] = valor()
            espacos()
            if (s[i] == ',') { i++; continue }
            esperar('}')
            return m
        }
    }

    private fun lista(): List<Any?> {
        val l = ArrayList<Any?>()
        i++
        espacos()
        if (s[i] == ']') { i++; return l }
        while (true) {
            l.add(valor())
            espacos()
            if (s[i] == ',') { i++; continue }
            esperar(']')
            return l
        }
    }

    private fun texto(): String {
        esperar('"')
        val sb = StringBuilder()
        while (true) {
            val c = s[i++]
            when (c) {
                '"' -> return sb.toString()
                '\\' -> when (val e = s[i++]) {
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000c')
                    'u' -> { sb.append(s.substring(i, i + 4).toInt(16).toChar()); i += 4 }
                    else -> sb.append(e)
                }
                else -> sb.append(c)
            }
        }
    }

    private fun numero(): Any {
        val inicio = i
        if (s[i] == '-') i++
        while (i < s.length && (s[i].isDigit() || s[i] in ".eE+-")) i++
        val t = s.substring(inicio, i)
        return t.toLongOrNull() ?: t.toDouble()
    }

    private fun palavra(p: String, v: Any?): Any? {
        require(s.startsWith(p, i)) { "esperava $p na posição $i" }
        i += p.length
        return v
    }

    private fun esperar(c: Char) {
        require(i < s.length && s[i] == c) { "esperava '$c' na posição $i" }
        i++
    }

    private fun espacos() {
        while (i < s.length && s[i].isWhitespace()) i++
    }
}
