package sift.cli

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.restrictTo
import sift.core.entity.Entity

class EntityTreeOptions : OptionGroup(name = "Entity tree/graph options") {
    val maxDepth: Int? by option("-L", "--max-depth",
            help = "Max display depth of the tree.")
        .int()
        .restrictTo(min = 0)

    val filter: List<Regex> by option("-F", "--filter",
            metavar = "REGEX",
            help = "Filter nodes by label. (repeatable)")
        .convert { Regex(it) }
        .multiple()

    val filterContext: List<Regex> by option("-S", "--filter-context",
            metavar = "REGEX",
            help = "Filter nodes by label, while also including sibling nodes." +
                " (repeatable)")
        .convert { Regex(it) }
        .multiple()

    val exclude: List<Regex> by option("-e", "--exclude",
            metavar = "REGEX",
            help = "Exclude nodes when label matches REGEX. (repeatable)")
        .convert { Regex(it) }
        .multiple()

    val excludeTypes: List<Entity.Type> by option("-E", "--exclude-type",
            metavar = "ENTITY_TYPE",
            help = "Exclude entity types from tree. (repeatable)")
        .convert { Entity.Type(it) }
        .multiple()

    val treeRoot: List<Entity.Type> by option("-r", "--tree-root",
            metavar = "ENTITY_TYPE",
            help = "Tree built around requested entity type. (repeatable)")
        .convert { Entity.Type(it) }
        .multiple()
}