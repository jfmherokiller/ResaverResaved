expect class PlatformByteBuffer {
    fun position(): Int
    fun slice(): PlatformByteBuffer
    fun makeLe()
    fun limit(startingOffset: Int)
    fun position(startingOffset: Int)
    fun capacity(): Int
    fun limit(): Int
    fun getFloat(): Float
    fun getByte():Byte
    fun putFloat(fl: Float)
    fun flip()
    fun getInt():Int
    fun getLong():Long
    fun hasRemaining(): Boolean
    fun getShort(): Short
}