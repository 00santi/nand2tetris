import java.io.File

var current_filename = ""
fun main(args: Array<String>) {
    for (filename in args) {
        println("\n-----$filename-----")
        val lines = File(filename).readLines().map { clean_line(it) }.filter { it.isNotEmpty() }
        current_filename = File(filename).name.substringBeforeLast('.')
        parse(lines)
        File("$current_filename.asm").writeText(output.joinToString("\n"))
    }
}

fun clean_line(s: String): String {
    val index = s.indexOf("//")
    return if (index != -1) {
        s.substring(0, index).trim()
    } else {
        s.trim()
    }
}

val output = mutableListOf<String>()
fun parse(lines: List<String>) {
    output.clear()
    eq_counter = 0
    gt_counter = 0
    lt_counter = 0
    for (line in lines) {
        val tokens = line.split(Regex("\\s+")).filter { it.isNotEmpty() }
        when (tokens[0]) {
            "add" -> add()
            "sub" -> sub()
            "neg" -> neg()
            "eq" -> eq()
            "gt" -> gt()
            "lt" -> lt()
            "and" -> and()
            "or" -> or()
            "not" -> not()
            "push" -> push(tokens)
            "pop" -> pop(tokens)
        }
    }
}

fun add() {
    output += listOf(
        "@SP",
        "AM=M-1",
        "D=M",
        "A=A-1",
        "M=M+D"
    )
}
fun sub() {
    output += listOf(
        "@SP",
        "AM=M-1",
        "D=M",
        "A=A-1",
        "M=M-D"
    )
}
fun neg() {
    output += listOf(
        "@SP",
        "A=M-1",
        "M=0-M"
    )
}

var eq_counter = 0
fun eq() {
    output += listOf(
        "@SP",
        "AM=M-1",
        "D=M",
        "A=A-1",
        "D=M-D",
        "@TRUE_EQ$eq_counter",
        "D;JEQ",
        "@SP",
        "A=M-1",
        "M=0",
        "@END_EQ$eq_counter",
        "0;JMP",
        "(TRUE_EQ$eq_counter)",
        "@SP",
        "A=M-1",
        "M=-1",
        "(END_EQ$eq_counter)"
    )
    eq_counter += 1
}

var gt_counter = 0
fun gt() {
    output += listOf(
        "@SP",
        "AM=M-1",
        "D=M",
        "A=A-1",
        "D=M-D",
        "@TRUE_GT$gt_counter",
        "D;JGT",
        "@SP",
        "A=M-1",
        "M=0",
        "@END_GT$gt_counter",
        "0;JMP",
        "(TRUE_GT$gt_counter)",
        "@SP",
        "A=M-1",
        "M=-1",
        "(END_GT$gt_counter)"
    )
    gt_counter += 1
}

var lt_counter = 0
fun lt() {
    output += listOf(
        "@SP",
        "AM=M-1",
        "D=M",
        "A=A-1",
        "D=M-D",
        "@TRUE_LT$lt_counter",
        "D;JLT",
        "@SP",
        "A=M-1",
        "M=0",
        "@END_LT$lt_counter",
        "0;JMP",
        "(TRUE_LT$lt_counter)",
        "@SP",
        "A=M-1",
        "M=-1",
        "(END_LT$lt_counter)"
    )
    lt_counter += 1
}

fun and() {
    output += listOf(
        "@SP",
        "AM=M-1",
        "D=M",
        "A=A-1",
        "M=M&D"
    )
}
fun or() {
    output += listOf(
        "@SP",
        "AM=M-1",
        "D=M",
        "A=A-1",
        "M=M|D"
    )
}
fun not() {
    output += listOf(
        "@SP",
        "A=M-1",
        "M=!M"
    )
}



fun push(tokens: List<String>) {
    val i = tokens[2].toInt()
    when (tokens[1]) {
        "constant" -> push_constant(i)
        "temp" -> push_temp(i)
        "local" -> push_segment("LCL", i)
        "argument" -> push_segment("ARG", i)
        "this" -> push_segment("THIS", i)
        "that" -> push_segment("THAT", i)
        "pointer" -> push_pointer(i)
        "static" -> push_static(i)
        else -> output.add("ERROR TRYING TO PROCESS ${tokens[1]}")
    }
}

fun push_constant(i: Int) {
    output += listOf(
        "@$i",
        "D=A",
        "@SP",
        "A=M",
        "M=D",
        "@SP",
        "M=M+1"

    )
}
fun push_temp(i: Int) {
    output += listOf(
        "@${i + 5}",
        "D=M",
        "@SP",
        "A=M",
        "M=D",
        "@SP",
        "M=M+1"
    )
}
fun push_segment(segment: String, i: Int) {
    output += listOf(
        "@$segment",
        "D=M",
        "@$i",
        "D=D+A",
        "A=D",
        "D=M",
        "@SP",
        "A=M",
        "M=D",
        "@SP",
        "M=M+1"
    )
}
fun push_pointer(i: Int) {
    val addr = if (i == 0) "THIS" else "THAT"
    output += listOf(
        "@$addr",
        "D=M",
        "@SP",
        "A=M",
        "M=D",
        "@SP",
        "M=M+1",
    )
}
fun push_static(i: Int) {
    val temp = "$current_filename.$i"
    output += listOf(
        "@$temp",
        "D=M",
        "@SP",
        "A=M",
        "M=D",
        "@SP",
        "M=M+1"
    )
}


fun pop(tokens: List<String>) {
    val i = tokens[2].toInt()
    when (tokens[1]) {
        "temp" -> pop_temp(i)
        "local" -> pop_segment("LCL", i)
        "argument" -> pop_segment("ARG", i)
        "this" -> pop_segment("THIS", i)
        "that" -> pop_segment("THAT", i)
        "pointer" -> pop_pointer(i)
        "static" -> pop_static(i)
        else -> output.add("ERROR TRYING TO PROCESS ${tokens[1]}")
    }
}

fun pop_temp(i: Int) {
    output += listOf(
        "@SP",
        "AM=M-1",
        "D=M",
        "@${i + 5}",
        "M=D",
    )
}
fun pop_segment(segment: String, i: Int) {
    output += listOf(
        "@$segment",
        "D=M",
        "@$i",
        "D=D+A",
        "@R13",
        "M=D",
        "@SP",
        "AM=M-1",
        "D=M",
        "@R13",
        "A=M",
        "M=D",
    )
}
fun pop_pointer(i: Int) {
    val addr = if (i == 0) "THIS" else "THAT"
    output += listOf(
        "@SP",
        "AM=M-1",
        "D=M",
        "@$addr",
        "M=D"
    )
}
fun pop_static(i: Int) {
    val temp = "$current_filename.$i"
    output += listOf(
        "@SP",
        "AM=M-1",
        "D=M",
        "@$temp",
        "M=D"
    )
}

