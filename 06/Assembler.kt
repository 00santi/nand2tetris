import java.io.File

val DEST_TABLE = mapOf(
    null to "000",
    "" to "000",
    "M" to "001",
    "D" to "010",
    "MD" to "011",
    "A" to "100",
    "AM" to "101",
    "AD" to "110",
    "AMD" to "111"
)
val COMP_TABLE = mapOf(
    "0" to "0101010",
    "1" to "0111111",
    "-1" to "0111010",
    "D" to "0001100",
    "A" to "0110000",
    "M" to "1110000",
    "D+1" to "0011111",
    "A+1" to "0110111",
    "M+1" to "1110111",
    "D-1" to "0001110",
    "A-1" to "0110010",
    "M-1" to "1110010",
    "D+A" to "0000010",
    "D+M" to "1000010",
    "D-A" to "0010011",
    "D-M" to "1010011",
    "A-D" to "0000111",
    "M-D" to "1000111",
    "D&A" to "0000000",
    "D&M" to "1000000",
    "D|A" to "0010101",
    "D|M" to "1010101",
    "!D" to "0001101",
    "!A" to "0110001",
    "!M" to "1110001",
    "-D" to "0001111",
    "-A" to "0110011",
    "-M" to "1110011"
)
val JUMP_TABLE = mapOf(
    null to "000",
    "" to "000",
    "JGT" to "001",
    "JEQ" to "010",
    "JGE" to "011",
    "JLT" to "100",
    "JNE" to "101",
    "JLE" to "110",
    "JMP" to "111"
)
val PREDEFINED_SYMBOLS = mutableMapOf(
    "SP" to 0,
    "LCL" to 1,
    "ARG" to 2,
    "THIS" to 3,
    "THAT" to 4,
    "R0" to 0,
    "R1" to 1,
    "R2" to 2,
    "R3" to 3,
    "R4" to 4,
    "R5" to 5,
    "R6" to 6,
    "R7" to 7,
    "R8" to 8,
    "R9" to 9,
    "R10" to 10,
    "R11" to 11,
    "R12" to 12,
    "R13" to 13,
    "R14" to 14,
    "R15" to 15,
    "SCREEN" to 16384,
    "KBD" to 24576
)

fun main(args: Array<String>) {
    for (filename in args) {
        if (!filename.endsWith(".asm")) {
            println("Skipping $filename: not an .asm file")
            continue
        }
        println("\n-----$filename-----")
        val lines = File(filename).readLines().map { cleanLine(it) }.filter { it.isNotEmpty() }
        firstpass(lines)
        assemble(lines, filename.removeSuffix(".asm") + ".hack")
    }
}

var nextVariableAddress = 16
val CURRENT_SYMBOLS: MutableMap<String, Int> = mutableMapOf()
fun firstpass(lines: List<String>) {
    nextVariableAddress = 16
    CURRENT_SYMBOLS.clear()
    var address = 0
    for (line in lines) when {
        line.startsWith('(') && line.endsWith(')') -> {
            val label = line.substring(1, line.length - 1).trim()
            CURRENT_SYMBOLS[label] = address
        }
        else -> address += 1
    }
}

fun assemble(lines: List<String>, output: String) {
    val outputLines: MutableList<String> = mutableListOf()
    for (line in lines) {
        val binary = when {
            line.startsWith('(') && line.endsWith(')') -> continue
            line.startsWith('@') -> parse_A_instruction(line.substring(1))
            else -> parse_C_instruction(line)
        }
        println(binary)
        outputLines.add(binary)
    }
    File(output).writeText(outputLines.joinToString("\n"))
}

fun cleanLine(s: String): String {
    val index = s.indexOf("//")
    return if (index != -1) { s.substring(0, index).trim() } else { s.trim() }
}

fun parse_A_instruction(line: String): String {
    val i = when {
        line.first().isDigit() -> line.toInt()
        PREDEFINED_SYMBOLS.contains(line) -> PREDEFINED_SYMBOLS[line]!!
        CURRENT_SYMBOLS.contains(line) -> CURRENT_SYMBOLS[line]!!
        else -> {
            val temp = nextVariableAddress++
            CURRENT_SYMBOLS[line] = temp
            temp
        }
    }
    return Integer.toBinaryString(i).padStart(16, '0')
}

fun parse_C_instruction(line: String): String {
    val equals = line.indexOf('=')
    val semicolon = line.indexOf(';')
    val dest = if (equals != -1) line.substring(0, equals) else null
    val compStart = if (equals != -1) equals + 1 else 0
    val compEnd = if (semicolon != -1) semicolon else line.length
    val comp = line.substring(compStart, compEnd)
    val jump = if (semicolon != -1) line.substring(semicolon + 1) else null

    val destBits = DEST_TABLE[dest] ?: "000"
    val compBits = COMP_TABLE[comp] ?: error("Unknown comp: $comp")
    val jumpBits = JUMP_TABLE[jump] ?: "000"

    return "111$compBits$destBits$jumpBits"
}