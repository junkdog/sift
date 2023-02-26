package sift.cli

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import java.nio.file.Path
import kotlin.io.path.exists

class TemplateOptions : OptionGroup(name = "Template options") {
    val template by option("-t", "--template",
            metavar = "TEMPLATE",
            help = "The template producing the system model.",
            completionCandidates = CompletionCandidates.Fixed(templateNames().toSet()))
        .convert { templates()[it]?.invoke() ?: fail("'$it' is not a valid template") }


    val listTemplates: Boolean by option("-l", "--list-templates",
            help = "Print all installed templates.")
        .flag()

    val listEntityTypes: Boolean by option("-T", "--list-entity-types",
            help = "Lists entity types defined by template.")
        .flag()

    val path: Path? by option("-f", "--class-dir",
            metavar = "PATH",
            help = "Path to directory structure containing classes or path to .jar",
            completionCandidates = CompletionCandidates.Path)
        .path(mustExist = true)
        .help("Jar or root directory with classes to analyze.")
        .convert { p -> p.resolve("target/classes").takeIf(Path::exists) ?: p }

    val profile: Boolean by option("--profile",
            help = "Print execution times and input/output for the executed template.")
        .flag()

    val dumpSystemModel: Boolean by option("-X", "--dump-system-model",
            help = "Print all entities along with their properties and metadata.")
        .flag()
}