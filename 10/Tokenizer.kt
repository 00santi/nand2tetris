import java.io.File
// s - string constant, i - int constant, S - symbol, I - identifier, k - keyword
val SYMBOLS = arrayOf('{', '}', '(', ')', '[', ']', '.', ',', ';', '+', '-', '*', '/', '&', '|', '<', '>', '=', '~')
val KEYWORDS = arrayOf("class", "constructor", "function", "method", "field", "static", "var", "int", "char",
    "boolean", "void", "true", "false", "null", "this", "let", "do", "if", "else", "while", "return")

fun tokenize(lines: List<String>): List<Pair<Char, String>> {
    val tokens = mutableListOf<Pair<Char, String>>()
    for(l in lines) {
        var i = 0
        while(i < l.length) {
            if(l[i].isWhitespace()) { i += 1 }
            else if(l[i] in SYMBOLS) { tokens += Pair('S', l[i++].toString()) }
            else if(l[i] == '"') {
                i++
                val s = StringBuilder()
                while(i < l.length && l[i] != '"') { s.append(l[i++]) }
                tokens += Pair('s', s.toString())
                i++
            }
            else if(l[i].isDigit()) {
                val s = StringBuilder()
                while(l[i].isDigit()) { s.append(l[i++]) }
                tokens += Pair('i', s.toString())
            }
            else {
                val s = StringBuilder()
                while(i < l.length && !l[i].isWhitespace() && l[i] !in SYMBOLS) { s.append(l[i++]) }
                val second = s.toString()
                val first = if(second in KEYWORDS) 'k' else 'I'
                tokens += Pair(first, second)
            }
        }
    }
    return tokens
}

fun make_Txml(tokens: List<Pair<Char, String>>): List<String> {
    val Txml = mutableListOf("<tokens>")
    for (t in tokens) {
        val type = when (t.first) {
            'k' -> "keyword"
            'i' -> "integerConstant"
            'I' -> "identifier"
            'S' -> "symbol"
            's' -> "stringConstant"
            else -> { println("error trying to make xml with type ${t.first}"); throw Exception() }
        }
        val value = when (t.second) {
            "<" -> "&lt;"
            ">" -> "&gt;"
            "&" -> "&amp;"
            else -> t.second
        }
        Txml += "<$type> $value </$type>"
    }
    Txml += "</tokens>"
    return Txml
}

fun outputTxml(Txml: List<String>, filename: String) {
    val f = "${filename}T.xml"
    File(f).writeText(Txml.joinToString("\n"))
}