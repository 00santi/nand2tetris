import java.io.File

class Parser(val tokens: List<Pair<Char, String>>) {
    var i = 0
    var xml = mutableListOf<String>()
    init { parse() }
    fun peekval() = tokens[i].second
    fun next(n: Int = 1) = repeat(n) { make_tag(tokens[i++]) }
    fun make_tag(token: Pair<Char, String>): String {
        val tag: String
        val type = when (token.first) {
            'k' -> "keyword"
            'i' -> "integerConstant"
            'I' -> "identifier"
            'S' -> "symbol"
            's' -> "stringConstant"
            else -> { println("error trying to make xml with type ${token.first}"); throw Exception() }
        }
        val value = when (token.second) {
            "<" -> "&lt;"
            ">" -> "&gt;"
            "&" -> "&amp;"
            else -> token.second
        }
        tag = "<$type> $value </$type>"
        xml += tag
        return tag
    }

    fun parse() {
        xml += "<class>"
        next(3) // 1.class-keyword 2.classname 3.openbracket{
        parse_rest()
        xml += "</class>"
    }

    fun parse_rest() {
        while (peekval() != "}") {
            when (peekval()) {
                "static", "field" -> parse_class_var()
                "constructor", "function", "method" -> parse_subroutine()
                else -> throw Exception("error with token: ${peekval()}")
            }
        }
        next() //closebracket} for class
    }

    fun parse_class_var() {
        xml += "<classVarDec>"
        next(3) // 1.static|field 2.vartype 3.varname
        while(peekval() == ",")
            next(2) // 1.comma, 2.varname ... 1.comma, 2.varname ...
        next() // semicolon;
        xml += "</classVarDec>"
    }

    fun parse_subroutine() {
        xml += "<subroutineDec>"
        next(4) // 1.constructor|fn|method 2.fntype 3.fnname 4. open-parenthesis(
        parse_params()
        next() // close-parenthesis)
        parse_body()
        xml += "</subroutineDec>"
    }

    fun parse_params() {
        if (peekval() == ")") {
            xml += listOf("<parameterList>", "</parameterList>")
            return
        }
        xml += "<parameterList>"
        next(2) //1.param-type 2.param-name
        while (peekval() == ",")
            next(3) //1. comma, 2.param-type 3.param-name
        xml += "</parameterList>"
    }

    fun parse_body() {
        xml += "<subroutineBody>"
        next() //open-bracket
        while(peekval() == "var") {
            parse_vardec()
        }
        parse_statements()
        next() //closing-bracket
        xml += "</subroutineBody>"
    }

    fun parse_vardec() {
        xml += "<varDec>"
        next(3) // var type name
        while(peekval() == ",")
            next(2)// comma name
        next()// ;
        xml += "</varDec>"
    }

    fun parse_statements() {
        xml += "<statements>"
        while(true) {
            when (peekval()) {
                "let" -> Let()
                "if" -> If()
                "while" -> While()
                "do" -> Do()
                "return" -> Return()
                "}" -> break
                else -> { println("error with statement ${peekval()}"); throw Exception() }
            }
        }
        xml += "</statements>"
    }
    fun Let() {
        xml += "<letStatement>" // let varname = expression ;
        next(2) // let varname
        if(peekval() == "[") { // varname == arr [ expression ]
            next() // [
            parse_expression()
            next() // ]
        }
        next() // =
        parse_expression()
        next() // ;
        xml += "</letStatement>"
    }
    fun If() {
        xml += "<ifStatement>"
        next(2) // if (
        parse_expression()
        next(2) // ) {
        parse_statements()
        next() // }
        while(peekval() == "else") {
            next(2) // else {
            parse_statements()
            next() // }
        }
        xml += "</ifStatement>"
    }
    fun While() {
        xml += "<whileStatement>"
        next(2) //while (
        parse_expression()
        next(2) // ) {
        parse_statements()
        next() // }
        xml += "</whileStatement>"
    }
    fun Do() {
        xml += "<doStatement>"
        next() // do
        parse_subroutine_call()
        next() // ;
        xml += "</doStatement>"
    }
    fun Return() {
        xml += "<returnStatement>"
        next() // return
        if(peekval() != ";") { parse_expression() }
        next() // ;
        xml += "</returnStatement>"
    }

    fun parse_subroutine_call() {
        //xml += "<subroutineCall>" // routine-name ( expressions ) ... or class-name . routine-name ( expressions )
        next() // class or routine
        if(peekval() == ".") { next(2) } // it was class, consume . routine
        next() // (
        parse_expression_list()
        next() // )
        //xml += "</subroutineCall>"
    }
    fun parse_expression() {
        xml += "<expression>" // term (op term)? (op term)? ...
        parse_term()
        while(operator()) { // op term
            next() // op
            parse_term() // term
        }
        xml += "</expression>"
    }
    fun operator(): Boolean {
        return when (peekval()) {
            "+", "-", "*", "/", "&", "|", "<", ">", "=" -> true
            else -> false
        }
    }

    fun parse_term() {
        xml += "<term>"
        val type = tokens[i].first
        val value = peekval()
        when {
            type in arrayOf('i', 's') || value in arrayOf("true", "false", "null", "this") -> { next() }
            type == 'I' -> { parse_identifier() }
            value == "(" -> {
                next() // (
                parse_expression()
                next() // )
            }
            value in arrayOf("-", "~") -> {
                next()
                parse_term()
            }
            else -> { println("error processing term ($type, $value)"); throw Exception() }
        }
        xml += "</term>"
    }

    fun parse_identifier() {
        // could be x, x[], subroutine(expressions), or class.subroutine(expressions)
        val temp = tokens[i + 1].second
        when (temp) {
            ".", "(" -> { parse_subroutine_call() }
            "[" -> {
                next(2)
                parse_expression()
                next()
            }
            else -> { next() }
        }
    }

    fun parse_expression_list() {
        xml += "<expressionList>"
        while(peekval() != ")") {
            parse_expression()
            if(peekval() == ",") next()
        }
        xml += "</expressionList>"
    }

    fun output(f: String) {
        val f = "${f}.xml"
        File(f).writeText(xml.joinToString("\n"))
    }
}