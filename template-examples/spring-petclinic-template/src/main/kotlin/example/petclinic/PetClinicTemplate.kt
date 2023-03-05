package example.petclinic

import org.objectweb.asm.Type
import sift.core.entity.Entity
import sift.core.dsl.template
import sift.core.graphviz.Dot
import sift.core.graphviz.Shape
import sift.core.template.SystemModelTemplate
import sift.core.terminal.Gruvbox.green1
import sift.core.terminal.Style.Companion.plain
import sift.template.dsl.graphviz
import sift.template.springboot.SpringBootTemplate
import sift.template.springcrud.SpringCrudTemplate

// in the interest of brevity and organization
typealias E = PetClinicTemplate.EntityTypes
typealias A = PetClinicTemplate.Annotations

typealias SBE = SpringBootTemplate.EntityType
typealias SBA = SpringBootTemplate.Annotation

typealias SCE = SpringCrudTemplate.EntityTypes
typealias SCA = SpringCrudTemplate.Annotations
typealias SCT = SpringCrudTemplate.AsmTypes

@Suppress("unused")
class PetClinicTemplate : SystemModelTemplate {
    // inhering templates
    val springBoot = SpringBootTemplate()
    val springCrud = SpringCrudTemplate()

    override val entityTypes: Iterable<Entity.Type> =
        springBoot.entityTypes + springCrud.entityTypes + listOf(E.modelAttribute)

    override val defaultType: Entity.Type = springBoot.defaultType

    // Annotations, AsmTypes and EntityTypes are just for organization
    object Annotations {
        private val String.type
            get() = Type.getType("L${replace('.', '/')};")!!


        // spring MVC
        val modelAttribute = "org.springframework.web.bind.annotation.ModelAttribute".type
    }

    object EntityTypes {
        private val String.type
            get() = Entity.Type(this)

        val modelAttribute = "model-attribute".type
    }

    /** `sift --template petclinic ...` */
    override val name: String
        get() = "petclinic"


    override fun template() = template {
        include(springBoot.template()) // entities: controllers, endpoints
        include(springCrud.template()) // entities: repositories

        // register @ModelAttribute methods
        classes {
            methods {
                annotatedBy(A.modelAttribute )
                // register @ModelAttribute entities as their function name
                entity(E.modelAttribute, label("\uD83D\uDE40 \${name}"), //
                    property("name", readName()))  // <- for \${name} in label
            }
        }


        // entities with registered relationships are rendered together.
        scope("wire entities") {

            // ... (advanced use case - aka, i wasn't sure it'd work)
            // A @ModelAttribute function adds attributes to the model object before
            // an endpoint method is invoked. To connect @ModelAttribute functions
            // with the endpoint methods that use their attributes, sift first tries
            // to find a direct relationship between them based on shared elements.
            // If a direct relationship exists for any entity, we use it for all
            // entities. Since no such association exists, sift falls back on the
            // indirect relationship established by the fact that they share the same
            // controller class.
            elementsOf(SBE.endpoint) { e ->
                // endpoints associated with all their @ModelAttribute functions
                e["model-attributes"] = E.modelAttribute
            }


            // wire all endpoints to repository methods via invocations
            SBE.endpoint["invokes"] = SCE.crudMethod.invocations

            // same as above, but for the model attributes
            E.modelAttribute["invokes"] = SCE.crudMethod.invocations
        }

        // for `--render`: updates entities with additional graphviz properties
        // SBE.endpoint is already configured by its template.
        scope("graphviz configuration") {
            graphviz(E.modelAttribute,
                rank = 1,                   // endpoint is registered with rank 0
                type = Dot.node
            )

            // to make it a bit nicer looking, we'll render the crud operations
            // as labels for the edges to the repository.
            graphviz(SCE.crudMethod,
                identifyAs = SCE.crudRepository,
                edgeLabel = { readName() }, // invoked repo method name
            )

            // style it more database-y
            graphviz(SCE.crudRepository,
                rank = 2,
                type = Dot.node,
                shape = Shape.cylinder,
//                label = replace("Repository", "")
            )

            elementsOf(SCE.crudMethod) {
                // prints crud methods when running with --debug
                log("crud methods")
            }
        }
    }

    override fun theme() = springBoot.theme() + springCrud.theme() + mapOf(
        E.modelAttribute to plain(green1)
    )
}
