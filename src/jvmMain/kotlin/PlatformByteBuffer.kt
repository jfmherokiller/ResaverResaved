import net.jpountz.lz4.LZ4Factory
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

actual class PlatformByteBuffer {
    var mybuffer:ByteBuffer = ByteBuffer.allocate(0)
    var IsLittleEndien:Boolean = false

    constructor(capacity: Int, IsLittleEndien: Boolean) {
        mybuffer = ByteBuffer.allocate(capacity)
        if(IsLittleEndien) {
            this.IsLittleEndien = true
            mybuffer.order(ByteOrder.LITTLE_ENDIAN)
        }
    }
    fun getInt():Int {
        return mybuffer.int
    }
    fun getInt(index:Int):Int {
        return mybuffer.getInt(index)
    }
    fun getLong():Long {
        return mybuffer.long
    }

    fun putInt(magic: Int) {
        mybuffer.putInt(magic)
    }

    fun putLong(compilationTime: Long) {
        mybuffer.putLong(compilationTime)
    }

    fun putShort(toShort: Short):PlatformByteBuffer {
        mybuffer.putShort(toShort)
        return this
    }


    fun flip() {
        (mybuffer as Buffer).flip()
    }

    fun put(bytes: ByteArray?): PlatformByteBuffer {
        mybuffer.put(bytes)
        return this
    }

    fun put(bytes: Byte):PlatformByteBuffer {
        mybuffer.put(bytes)
        return this
    }
    fun writeFileChannel(CHANNEL: FileChannel) {
        CHANNEL.write(mybuffer)
    }
    fun readFileChannel(CHANNEL: FileChannel): Int {
        return CHANNEL.read(mybuffer)
    }
    fun readFileChannel(CHANNEL: FileChannel,offset:Long): Int {
        return CHANNEL.read(mybuffer,offset)
    }

    fun put(bytes: ByteArray, i: Int, i1: Int) {
        mybuffer.put(bytes,i,i1)
    }

    fun putFloat(fl: Float) {
        mybuffer.putFloat(fl)
    }
    fun getByte():Byte {
        return mybuffer.get()
    }

    fun getFloat(): Float {
        return mybuffer.float
    }

    fun getShort(): Short {
        return mybuffer.short
    }

    operator fun get(bytes: ByteArray) {
        mybuffer[bytes]
    }
    fun makeLe() {
        mybuffer.order(ByteOrder.LITTLE_ENDIAN)
    }

    fun position(): Int {
        return mybuffer.position()
    }
    fun position(newpos:Int) {
        mybuffer = (mybuffer as Buffer).position(newpos) as ByteBuffer
    }

    fun limit(): Int {
        return mybuffer.limit()
    }
    fun limit(startingOffset: Int) {
        mybuffer = (mybuffer as Buffer).limit(startingOffset) as ByteBuffer
    }
    fun hasRemaining(): Boolean {
        return mybuffer.hasRemaining()
    }
    fun array(): ByteArray {
        return mybuffer.array()
    }

    fun compress(uncompressedSize:Int): PlatformByteBuffer {
        val LZ4FACTORY = LZ4Factory.fastestInstance()
        val LZ4COMP = LZ4FACTORY.fastCompressor()
        val COMPRESSED = allocate(LZ4COMP.maxCompressedLength(uncompressedSize))
        LZ4COMP.compress(mybuffer,COMPRESSED.mybuffer)
        COMPRESSED.flip()
        return COMPRESSED
    }
    fun decompress(uncompressedSize: Int):PlatformByteBuffer {
        val uncompressed = allocate(uncompressedSize)
        val LZ4FACTORY = LZ4Factory.fastestInstance()
        val LZ4DECOMP = LZ4FACTORY.fastDecompressor()
        LZ4DECOMP.decompress(mybuffer, uncompressed.mybuffer)
        uncompressed.flip()
        return uncompressed
    }

    fun put(buffer: PlatformByteBuffer) {
        mybuffer.put(buffer.mybuffer)
    }

    fun compact() {
        mybuffer.compact()
    }
    fun slice():PlatformByteBuffer {
        return PlatformByteBuffer(mybuffer.slice())
    }

    fun capacity(): Int {
       return mybuffer.capacity()
    }

    fun duplicate(): PlatformByteBuffer {
        return PlatformByteBuffer(mybuffer.duplicate())
    }

    companion object {
        fun allocate(capacity: Int): PlatformByteBuffer {
            return PlatformByteBuffer(capacity, false)
        }
        fun allocateLe(capacity: Int): PlatformByteBuffer {
            return PlatformByteBuffer(capacity,true)
        }

        fun wrap(compressedBytes: ByteArray, i: Int, toInt: Int): PlatformByteBuffer {
            return PlatformByteBuffer(ByteBuffer.wrap(compressedBytes, i, toInt))
        }
        fun wrap(compressedBytes: ByteArray): PlatformByteBuffer {
            return PlatformByteBuffer(ByteBuffer.wrap(compressedBytes))
        }

        fun allocateDirect(toInt: Int): PlatformByteBuffer {
            return PlatformByteBuffer(ByteBuffer.allocateDirect(toInt))
        }

    }

    constructor(capacity: ByteBuffer) {
        mybuffer = capacity
    }
}
