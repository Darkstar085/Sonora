package com.sipun.sonora.data.lyrics

internal object LyricsParser {
    private val tag = Regex("""\[(\d{1,3}):(\d{2})(?:\.(\d{1,3}))?\]""")
    fun parse(value: String): Lyrics {
        val lines = value.lineSequence().flatMap { line ->
            val matches = tag.findAll(line).toList()
            if (matches.isEmpty()) sequenceOf(LyricsLine(line.trim()))
            else matches.asSequence().map { m ->
                val f = m.groupValues[3]
                val ms = m.groupValues[1].toLong() * 60000 + m.groupValues[2].toLong() * 1000 +
                        when (f.length) {
                            1 -> f.toLong() * 100; 2 -> f.toLong() * 10; 3 -> f.toLong(); else -> 0
                        }
                LyricsLine(line.substring(m.range.last + 1).trim(), ms)
            }
        }.filter { it.text.isNotEmpty() }.toList()
        return Lyrics(lines, lines.any { it.startMs != null })
    }
}
