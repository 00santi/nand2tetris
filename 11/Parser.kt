import java.io.File

class Parser(val tokens: List<Pair<Char, String>>, val SymbolTable: SymbolTable) {
    private var i = 0
    private var xml = mutableListOf<String>()
    init { parse() }
    private fun peek(offset: Int = 0) = tokens[i + offset].second
    private fun next(times: Int = 1) = repeat(times) { make_tag(tokens[i++]) }
    private fun make_tag(token: Pair<Char, String>): String {
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

    private var classname = ""
    private fun parse() {
        xml += "<class>"
        classname = peek(1)
        next(3) // 1.class(keyword) 2.classname 3.openbracket{
        parse_rest()
        xml += "</class>"
    }

    private fun parse_rest() {
        while (peek() != "}") {
            when (peek()) {
                "static", "field" -> parse_class_var()
                "constructor", "function", "method" -> parse_subroutine()
                else -> throw Exception("error with token: ${peek()}")
            }
        }
        next() //closebracket} for class
    }

    private fun parse_class_var() {
        xml += "<classVarDec>"
        val kind = peek()
        val type = peek(1)
        send(); next(3) // 1.field|static 2.vartype 3.varname
        while(peek() == ",") {
            send(kind, type, peek(1))
            next(2) // 1.comma, 2.varname ... 1.comma, 2.varname ...
        }
        next() // semicolon;
        xml += "</classVarDec>"
    }
    private fun send(kind: String = peek(), type: String = peek(1), name: String = peek(2)) {
        SymbolTable.def(kind, type, name)
    }

    private fun parse_subroutine() {
        xml += "<subroutineDec>"
        val kind = peek(); //constructor|fn|method
        val fnName = peek(2)
        SymbolTable.start_subroutine()
        if(kind == "method") { SymbolTable.def("arg", classname, "this") }
        next(4) // 1.constructor|fn|method 2.fntype 3.fnname 4. open-parenthesis(
        parse_params()
        next() // close-parenthesis)
        parse_body(fnName, kind)
        xml += "</subroutineDec>"
    }

    private fun parse_params() {
        if (peek() == ")") {
            xml += listOf("<parameterList>", "</parameterList>")
            return
        }
        xml += "<parameterList>"
        send(kind = "arg", type = peek(), name = peek(1)); next(2) //1.param-type 2.param-name
        while (peek() == ","){
            send(kind = "arg")
            next(3) //1. comma, 2.param-type 3.param-name
        }
        xml += "</parameterList>"
    }

    private fun parse_body(fnName: String, kind: String) {
        xml += "<subroutineBody>"
        next() //open-bracket
        while(peek() == "var") { parse_vardec() }
        val nvars = SymbolTable.varcount("var")
        vm_push("function $classname.$fnName $nvars")
        if(kind == "constructor") {
            val nfields = SymbolTable.varcount("field")
            vm_push("push constant $nfields", "call Memory.alloc 1", "pop pointer 0")
        }
        else if(kind == "method") vm_push("push argument 0", "pop pointer 0")
        parse_statements()
        next() //closing-bracket
        xml += "</subroutineBody>"
    }

    private fun parse_vardec() {
        xml += "<varDec>"
        val kind = peek()
        val type = peek(1)
        send(); next(3) // var type name
        while(peek() == ",") {
            send(kind=kind, type=type, name=peek(1))
            next(2)// comma name
        }
        next()// ;
        xml += "</varDec>"
    }

    private fun parse_statements() {
        xml += "<statements>"
        while(true) {
            when (peek()) {
                "let" -> Let()
                "if" -> If()
                "while" -> While()
                "do" -> Do()
                "return" -> Return()
                "}" -> break
                else -> { println("error with statement ${peek()}"); throw Exception() }
            }
        }
        xml += "</statements>"
    }
    private fun Let() {
        xml += "<letStatement>"
        if(peek(2) != "[") { // let varname = expression ;
            val name = peek(1)
            next(3) // let varname =
            parse_expression()
            next() // ;
            val index = SymbolTable.getindex(name)
            val segment = SymbolTable.getsegment(name)
            vm_push("pop $segment $index")
        } else {
            val arrname = peek(1)
            next(3) // let x[
            parse_expression()
            next(2) // ] =
            val index = SymbolTable.getindex(arrname)
            val segment = SymbolTable.getsegment(arrname)
            vm_push("push $segment $index", "add")
            parse_expression()
            next() // ;
            vm_push("pop temp 0", "pop pointer 1", "push temp 0", "pop that 0")
        }
        xml += "</letStatement>"
    }

    private var ifcounter = 0
    private fun If() {
        val truelabel = "IF_TRUE$ifcounter"
        val falselabel = "IF_FALSE$ifcounter"
        val endlabel = "IF_END$ifcounter"
        ifcounter += 1
        xml += "<ifStatement>"
        next(2) // if (
        parse_expression()
        vm_push("if-goto $truelabel", "goto $falselabel")
        next(2) // ) {
        vm_push("label $truelabel")
        parse_statements()
        vm_push("goto $endlabel", "label $falselabel")
        next() // }
        while(peek() == "else") {
            next(2) // else {
            parse_statements()
            next() // }
        }
        vm_push("label $endlabel")
        xml += "</ifStatement>"
    }

    private var whilecounter = 0
    private fun While() {
        val body = "WHILE_BODY$whilecounter"
        val cond = "WHILE_COND$whilecounter"
        val end = "WHILE_END$whilecounter"
        whilecounter += 1
        xml += "<whileStatement>"
        next(2) //while (
        vm_push("label $cond")
        parse_expression()
        vm_push("if-goto $body", "goto $end")
        next(2) // ) {
        vm_push("label $body")
        parse_statements()
        vm_push("goto $cond")
        next() // }
        vm_push("label $end")
        xml += "</whileStatement>"
    }

    private fun Do() {
        xml += "<doStatement>"
        next() // do
        parse_subroutine_call()
        vm_push("pop temp 0")
        next() // ;
        xml += "</doStatement>"
    }
    private fun Return() {
        xml += "<returnStatement>"
        next() // return
        if(peek() != ";") { parse_expression() }
        else { vm_push("push constant 0") }
        vm_push("return")
        next() // ;
        xml += "</returnStatement>"
    }

    private fun parse_subroutine_call() {
        var name = peek()
        next()
        var extra_args = 0
        if (peek() == ".") {
            val left = name
            next() // .
            val sub = peek()
            next() // subroutine-name
            if (SymbolTable.has(left)) {
                val seg = SymbolTable.getsegment(left)
                val idx = SymbolTable.getindex(left)
                val typ = SymbolTable.gettype(left)
                vm_push("push $seg $idx")
                name = "$typ.$sub"
                extra_args = 1
            } else {
                name = "$left.$sub"
            }
        } else {
            vm_push("push pointer 0")
            name = "$classname.$name"
            extra_args = 1
        }
        next() // (
        val no_args = parse_expression_list()
        vm_push("call $name ${no_args + extra_args}")
        next() // )
    }
    private fun parse_expression() {
        xml += "<expression>" // term (op term)? (op term)? ...
        parse_term()
        while(operator()) { // op term
            val op = peek()
            next() // op
            parse_term() // term
            SymbolTable.vmlines += when(op) {
                "+" -> "add"
                "-" -> "sub"
                "*" -> "call Math.multiply 2"
                "/" -> "call Math.divide 2"
                "&" -> "and"
                "|" -> "or"
                "<" -> "lt"
                ">" -> "gt"
                "=" -> "eq"
                else -> throw Exception()
            }
        }
        xml += "</expression>"
    }
    private fun operator(): Boolean {
        return when (peek()) {
            "+", "-", "*", "/", "&", "|", "<", ">", "=" -> true
            else -> false
        }
    }

    private fun parse_term() {
        xml += "<term>"
        val type = tokens[i].first
        val value = peek()
        when {
            type == 'i' -> {
                vm_push("push constant $value")
                next()
            }
            type == 's' -> {
                next()
                vm_push("push constant ${value.length}", "call String.new 1")
                for (c in value) { vm_push("push constant ${c.code}", "call String.appendChar 2") }
            }
            value == "true" -> {
                next()
                vm_push("push constant 0", "not")
            }
            value == "false" || value == "null" -> {
                next()
                vm_push("push constant 0")
            }
            value == "this" -> {
                next()
                vm_push("push pointer 0")
            }
            type == 'I' -> {
                when(peek(1)) {
                    ".", "(" -> parse_subroutine_call()
                    "[" -> {
                        val arrname = peek()
                        val index = SymbolTable.getindex(arrname)
                        val segment = SymbolTable.getsegment(arrname)
                        next(2) // varname [
                        parse_expression()
                        next() // ]
                        vm_push("push $segment $index", "add", "pop pointer 1", "push that 0")
                    }
                    else -> { // variable
                        val index = SymbolTable.getindex(value)
                        val segment = SymbolTable.getsegment(value)
                        vm_push("push $segment $index")
                        next()
                    }
                }
            }
            value == "(" -> {
                next() // (
                parse_expression()
                next() // )
            }
            value in arrayOf("-", "~") -> {
                next()
                parse_term()
                SymbolTable.vmlines += when(value) {
                    "-" -> "neg"
                    "~" -> "not"
                    else -> throw Exception("error with operator $value")
                }
            }
            else -> { println("error processing term ($type, $value)"); throw Exception() }
        }
        xml += "</term>"
    }

    fun vm_push(vararg lines: String) {
        for (line in lines) { SymbolTable.vmlines += line }
    }

    private fun parse_expression_list(): Int {
        xml += "<expressionList>"
        var no_args = 0
        while(peek() != ")") {
            parse_expression()
            no_args += 1
            if(peek() == ",") next()
        }
        xml += "</expressionList>"
        return no_args
    }

    fun output(f: String) {
        val f = "${f}.xml"
        File(f).writeText(xml.joinToString("\n"))
    }
}