expect class PlatformByteBuffer {
    fun position(): Int
    fun slice(): PlatformByteBuffer
    fun makeLe()
    fun limit(startingOffset: Int)
    fun position(startingOffset: Int)
}