package sift.core.entity

sealed interface LabelFormatter : Entity.LabelFormatter {

    class FromPattern(val pattern: String) : LabelFormatter {

        override fun format(entity: Entity, service: EntityService): String {
            return replaceRegex
                .replace(pattern) { match ->
                    val token = match.groups[1]?.value ?: error("unable to find token: $match")

                    val split = token.split(":")
                    val (key, defaultValue) = when (split.size) {
                        2 -> split
                        1 -> listOf(split.first(), "\${${split.first()}}")
                        else -> error("':' can only occur once and is not escapable atm")
                    }

                    entity[key]?.first()?.toString() ?: defaultValue
                }
        }

        companion object {
            // matches text within ${var}
            val replaceRegex = Regex("\\\$\\{([^}]+)\\}")
        }
    }

    object FromElement : LabelFormatter {
        override fun format(entity: Entity, service: EntityService): String {
            return service[entity].simpleName
        }
    }
}