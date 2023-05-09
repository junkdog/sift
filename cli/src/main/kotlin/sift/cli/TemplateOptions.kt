package sift.cli

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.*
import sift.core.template.SystemModelTemplate
import java.net.URI

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

    val mavenRepositories: List<URI> by option("-m", "--maven-repository",
            help = "Additional maven repositories to use for downloading artifacts. Maven central " +
                   "(https://repo1.maven.org/maven2/) and local user repositories are always included.")
        .convert { URI(it) }
        .multiple()

    val classNodes: String? by option("-f", "--class-dir", "--classes",
            metavar = "PATH|URI|MAVEN_COORD",
            help = "Provide class input as a directory, JAR file, URI (pointing to a JAR), or Maven coordinate.")

    val diff: String? by option("-d", "--diff",
            metavar = "FILE_JSON|URI|MAVEN_COORD",
            help = "Compare the system model from '-f' with another, specified as a JSON file (previously saved System Model), " +
                   "class input as a directory, JAR file, URI (pointing to a JAR), or Maven coordinate.")

    val profile: Boolean by option("--profile",
            help = "Print execution times and input/output for the executed template.")
        .flag()

    val dumpSystemModel: Boolean by option("-X", "--dump-system-model",
            help = "Print all entities along with their properties and metadata.")
        .flag()
}