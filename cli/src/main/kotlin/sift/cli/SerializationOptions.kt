package sift.cli

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import java.io.File

class SerializationOptions : OptionGroup(name = "Serialization options") {
    val save: File? by option("-s", "--save",
            metavar = "FILE_JSON",
            help = "Save the resulting system model as json.",
            completionCandidates = CompletionCandidates.Path
    ).file(canBeDir = false)

    val load: File? by option("--load",
            metavar = "FILE_JSON",
            help = "Load a previously saved system model.",
            completionCandidates = CompletionCandidates.Path
    ).file(canBeDir = false, mustExist = true, mustBeReadable = true)

    val diff: File? by option("-d", "--diff",
            metavar = "FILE_JSON",
            help = "Compare against a previously saved system model.",
            completionCandidates = CompletionCandidates.Path
    ).file(canBeDir = false, mustExist = true, mustBeReadable = true)
}