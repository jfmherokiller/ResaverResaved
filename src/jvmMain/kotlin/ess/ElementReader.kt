package ess

import PlatformByteBuffer

fun interface ElementReader<T : Element?> {
    fun read(input: PlatformByteBuffer?): T
}