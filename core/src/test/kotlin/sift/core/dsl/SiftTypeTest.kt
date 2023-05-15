package sift.core.dsl

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SiftTypeTest {
    val types = listOf(
        "java.lang.String".type,
        "java.lang.Integer".type,
        "java.lang.Long".type,
        "java.lang.Boolean".type,
        "java.lang.Object".type,
        "java.lang.Number".type,
        "java.lang.reflect.Array".type,
        "java.lang.reflect.Field".type,

        "java.util.Map".type,
        "java.util.List".type,
        "java.util.HashMap".type,
        "java.util.LinkedHashMap".type,
        "java.util.HashMap<java.lang.Integer, java.lang.String>".type,
        "java.util.HashMap<java.lang.Integer, java.lang.Integer>".type,
    )

    @Test
    fun `match non-generic java lang types`() {
        val javaLang = Regex("""java\.lang\.[\w.]+$""").type // \w does not match <
        assertThat(types.filter(javaLang::matches))
            .containsExactlyInAnyOrder(
                "java.lang.String".type,
                "java.lang.Integer".type,
                "java.lang.Long".type,
                "java.lang.Boolean".type,
                "java.lang.Object".type,
                "java.lang.Number".type,
                "java.lang.reflect.Array".type,
                "java.lang.reflect.Field".type,
            )
    }

    @Test
    fun `match concrete map types`() {
        val maps = Regex("[^.]Map").type
        assertThat(types.filter(maps::matches))
            .containsExactlyInAnyOrder(
                "java.util.HashMap".type,
                "java.util.LinkedHashMap".type,
                "java.util.HashMap<java.lang.Integer, java.lang.String>".type,
                "java.util.HashMap<java.lang.Integer, java.lang.Integer>".type,
            )
    }

    @Test
    fun `match maps with integer keys`() {
        val mapOfInts = Regex("Map<[\\w.]+Integer").type
        assertThat(types.filter(mapOfInts::matches))
            .containsExactlyInAnyOrder(
                "java.util.HashMap<java.lang.Integer, java.lang.String>".type,
                "java.util.HashMap<java.lang.Integer, java.lang.Integer>".type,
            )
    }

    @Test
    fun `match types operator contains`() {
        assertThat(Regex("Field").type in types).isTrue
        assertThat(Regex("Field2").type in types).isFalse
    }
}

