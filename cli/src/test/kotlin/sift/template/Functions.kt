package sift.template

import sift.cli.templates
import java.net.URL

fun template(name: String) = templates()[name]!!().template()

fun resource(path: String): URL {
    return TemplateValidationTest::class.java.getResource(path)!!
}