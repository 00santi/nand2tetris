import java.io.File

fun main(args: Array<String>) {
    if(args.isEmpty()) { return }
    val files = get_files(File(args[0]))
    for(f in files) {
        val lines = clean(f.readLines())
        val tokens = tokenize(lines)

        val Txml = make_Txml(tokens)
        outputTxml(Txml, f.nameWithoutExtension)

        val parser = Parser(tokens)
        parser.output(f.nameWithoutExtension)
    }
}

fun get_files(path: File): List<File> {
    val files = mutableListOf<File>()
    if(path.isDirectory) {
        for (f in path.listFiles { _, name -> name.endsWith(".jack") } ?: emptyArray() ) { files += f }
    } else { files.add(path) }
    return files
}

fun clean(lines: List<String>): List<String> {
    val result = mutableListOf<String>()
    for (line in lines) {
        var l = line.trim()
        if (l.startsWith("/") || l.startsWith("*")) { continue }
        if (l.contains("//")) { l = l.substring(0, l.indexOf("//")) }
        l = l.trim()
        if (l.isNotEmpty()) { result += l }
    }
    return result
}