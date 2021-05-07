package ess

import java.nio.ByteBuffer

fun interface ElementReader<T : Element?> {
    fun read(input: ByteBuffer?): T
}