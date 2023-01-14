package sift.instrumenter.spi

import sift.core.instrumenter.InstrumenterService

interface InstrumenterServiceProvider {
    fun create(): InstrumenterService
}