package sift.core.api

import sift.core.entity.Entity

sealed interface EntityResolution

internal class Instantiations(val type: Entity.Type) : EntityResolution
internal class Invocations(val type: Entity.Type) : EntityResolution
internal class FieldAccess(val type: Entity.Type) : EntityResolution
