package sift.cli

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.objectweb.asm.tree.ClassNode
import sift.core.api.SystemModel
import sift.core.api.resolveSystemModel
import sift.core.asm.resolveClassNodes
import sift.core.template.SystemModelTemplate

class TemplateOptions : OptionGroup(name = "Template options") {
    val template: SystemModelTemplate? by option("-t", "--template",
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

    val classNodes: List<ClassNode>? by option("-f", "--class-dir", "--classes",
            metavar = "PATH|URI|MAVEN_COORD",
            help = "Provide class input as a directory, JAR file, URI (pointing to a JAR), or Maven coordinate.")
        .convert { resolveClassNodes(it) }

    val diff: SystemModel? by option("-d", "--diff",
            metavar = "FILE_JSON|URI|MAVEN_COORD",
            help = "Compare the system model from '-f' with another, specified as a JSON file (previously saved System Model), " +
                   "class input as a directory, JAR file, URI (pointing to a JAR), or Maven coordinate.")
        .convert { resolveSystemModel(it, template) }

    val profile: Boolean by option("--profile",
            help = "Print execution times and input/output for the executed template.")
        .flag()

    val dumpSystemModel: Boolean by option("-X", "--dump-system-model",
            help = "Print all entities along with their properties and metadata.")
        .flag()
}