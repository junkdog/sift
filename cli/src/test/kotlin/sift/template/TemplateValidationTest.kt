package sift.template;

import org.junit.jupiter.api.Nested
import sift.core.entity.Entity
import sift.template.projects.*


class TemplateValidationTest {
    @Nested
    inner class BaeldungAxon : TemplateValidator(
        template = template("spring-axon"),
        jar = resource("/baeldung-axon-3e58e24219.jar"),
        root = Entity.Type("controller"),
        expectedTree = BAELDUNG_AXON,
        expectedProfile = BAELDUNG_AXON_PROFILE,
        expectedStatistics = BAELDUNG_AXON_STATS,
    )

    @Nested
    inner class SiftSelf : TemplateValidator(
        template = template("sift"),
        jar = resource("/sift-core-0.14.0.jar"),
        root = Entity.Type("scope"),
        expectedTree = SIFT,
        expectedProfile = SIFT_PROFILE,
        expectedStatistics = SIFT_STATS,
    )
}