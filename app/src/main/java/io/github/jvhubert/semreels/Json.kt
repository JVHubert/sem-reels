package io.github.jvhubert.semreels

/**
 * Escreve JSON a partir de Map, List, String, número, booleano e null.
 * Não usa org.json porque ele não roda nos testes do PC (lá ele é só uma casca vazia).
 */
object Json {
    fun texto(valor: Any?): String = StringBuilder().also { escrever(it, valor) }.toString()

    private fun escrever(sb: StringBuilder, v: Any?) {
        when (v) {
            null -> sb.append("null")
            is String -> aspas(sb, v)
            is Boolean, is Int, is Long -> sb.append(v.toString())
            is Number -> sb.append(if (v.toDouble().isFinite()) v.toString() else "null")
            is Map<*, *> -> {
                sb.append('{')
                var primeiro = true
                for ((k, valor) in v) {
                    if (!primeiro) sb.append(',')
                    primeiro = false
                    aspas(sb, k.toString())
                    sb.append(':')
                    escrever(sb, valor)
                }
                sb.append('}')
            }
            is Iterable<*> -> {
                sb.append('[')
                var primeiro = true
                for (item in v) {
                    if (!primeiro) sb.append(',')
                    primeiro = false
                    escrever(sb, item)
                }
                sb.append(']')
            }
            is IntArray -> escrever(sb, v.toList())
            else -> aspas(sb, v.toString())
        }
    }

    private fun aspas(sb: StringBuilder, s: String) {
        sb.append('"')
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (c < ' ' || c == ' ' || c == ' ') {
                    sb.append("\\u").append(String.format("%04x", c.code))
                } else {
                    sb.append(c)
                }
            }
        }
        sb.append('"')
    }
}
