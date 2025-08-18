import java.io.File

class SymbolTable {
    val class_table = mutableMapOf<String, Triple<String, String, Int>>() // varname : kind , type , index
    var field_counter = 0
    var static_counter = 0

    val subroutine_table = mutableMapOf<String, Triple<String, String, Int>>()
    var arg_counter = 0
    var var_counter = 0

    val vmlines = mutableListOf<String>()

    fun def(kind: String, type: String, name: String) {
        when (kind) {
            "field" -> { class_table[name] = Triple(kind, type, field_counter++) }
            "static" -> { class_table[name] = Triple(kind, type, static_counter++) }
            "arg" -> { subroutine_table[name] = Triple(kind, type, arg_counter++) }
            "var" -> { subroutine_table[name] = Triple(kind, type, var_counter++) }
            else -> { throw Exception("error trying to define entry: ($kind, $type, $name) ") }
        }
    }

    fun varcount(kind: String): Int {
        return when(kind.first()) {
            'f' -> field_counter;'s' -> static_counter
            'a' -> arg_counter; 'v' -> var_counter
            else -> throw Exception("error trying to get count of kind $kind")
        }
    }

    fun has(name: String) = class_table.containsKey(name) || subroutine_table.containsKey(name)

    fun getkind(name: String): String {
        return when {
            class_table.contains(name) -> class_table[name]!!.first
            subroutine_table.contains(name) -> subroutine_table[name]!!.first
            else -> throw Exception("error getting var from table: $name")
        }
    }

    fun gettype(name: String): String {
        return if (class_table.contains(name)) { class_table[name]!!.second }
        else { subroutine_table[name]!!.second }
    }

    fun getsegment(name: String): String {
        return when(getkind(name)) {
            "var" -> "local"
            "arg" -> "argument"
            "field" -> "this"
            "static" -> "static"
            else -> throw Exception("error getting segment of kind ${getkind(name)}")
        }
    }

    fun getindex(name: String): Int {
        return if (class_table.contains(name)) { class_table[name]!!.third }
        else { subroutine_table[name]!!.third }
    }

    fun start_subroutine() {
        arg_counter = 0
        var_counter = 0
        subroutine_table.clear()
    }

    fun outputVM(fname: String) {
        val f = "${fname}.vm"
        File(f).writeText(vmlines.joinToString("\n"))
    }
}