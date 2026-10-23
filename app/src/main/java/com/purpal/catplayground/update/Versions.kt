package com.purpal.catplayground.update

object Versions {
    private fun parse(version: String): List<Int> =
        version.trim().removePrefix("v").split(".").map { part -> part.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }

    fun isNewer(candidate: String, current: String): Boolean {
        val a = parse(candidate)
        val b = parse(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    /** 把 release-please 產生的 Markdown 更新紀錄整理成簡單的條列文字。 */
    fun releaseNotes(markdown: String): String =
        markdown.lines()
            .map { it.trim() }
            .filter { it.startsWith("* ") }
            .joinToString("\n") { line ->
                "• " + line.removePrefix("* ")
                    .replace(Regex("""\s*\(\[[0-9a-f]+]\([^)]*\)\)"""), "")
                    .replace("**", "")
            }
}
