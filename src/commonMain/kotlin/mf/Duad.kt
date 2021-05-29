package mf

/**
 *
 * @author Mark Fairchild
 * @param <T>
</T> */
class Duad<T>(var A: T, var B: T) {

    override fun toString(): String {
        return "($A, $B)"
    }

    override fun hashCode(): Int {
        var hash = 5
        hash = 97 * hash + A.hashCode()
        hash = 97 * hash + B.hashCode()
        return hash
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null) {
            return false
        }
        if (this::class != other::class) {
            return false
        }
        val other2 = other as Duad<*>
        return A == other2.A && B == other2.B
    }

    companion object {

        fun <T> make(a: T, b: T): Duad<T> {
            return Duad(a, b)
        }
    }
}