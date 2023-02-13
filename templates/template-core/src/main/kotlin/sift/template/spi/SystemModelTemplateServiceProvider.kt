package sift.template.spi

import sift.core.template.SystemModelTemplate

interface SystemModelTemplateServiceProvider {
    fun create(): SystemModelTemplate
}