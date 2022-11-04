package sift.instrumenter.spi

import sift.instrumenter.InstrumenterService

interface InstrumenterServiceProvider {
    fun create(): InstrumenterService
}