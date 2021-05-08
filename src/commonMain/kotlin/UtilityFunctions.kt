class UtilityFunctions {

    companion object {
        /**
         * Compares two `byte` values numerically treating the values
         * as unsigned.
         *
         * @param  x the first `byte` to compare
         * @param  y the second `byte` to compare
         * @return the value `0` if `x == y`; a value less
         * than `0` if `x < y` as unsigned values; and
         * a value greater than `0` if `x > y` as
         * unsigned values
         * @since 9
         */
        fun compareUnsigned(x: Byte, y: Byte): Int {
            return toUnsignedInt(x) - toUnsignedInt(y)
        }

        /**
         * Converts the argument to an `int` by an unsigned
         * conversion.  In an unsigned conversion to an `int`, the
         * high-order 24 bits of the `int` are zero and the
         * low-order 8 bits are equal to the bits of the `byte` argument.
         *
         * Consequently, zero and positive `byte` values are mapped
         * to a numerically equal `int` value and negative `byte` values are mapped to an `int` value equal to the
         * input plus 2<sup>8</sup>.
         *
         * @param  x the value to convert to an unsigned `int`
         * @return the argument converted to `int` by an unsigned
         * conversion
         * @since 1.8
         */
        fun toUnsignedInt(x: Byte): Int {
            return x.toInt() and 0xff
        }

        /**
         * Converts the argument to a `long` by an unsigned
         * conversion.  In an unsigned conversion to a `long`, the
         * high-order 56 bits of the `long` are zero and the
         * low-order 8 bits are equal to the bits of the `byte` argument.
         *
         * Consequently, zero and positive `byte` values are mapped
         * to a numerically equal `long` value and negative `byte` values are mapped to a `long` value equal to the
         * input plus 2<sup>8</sup>.
         *
         * @param  x the value to convert to an unsigned `long`
         * @return the argument converted to `long` by an unsigned
         * conversion
         * @since 1.8
         */
        fun toUnsignedLong(x: Byte): Long {
            return x.toLong() and 0xffL
        }

        /**
         * Converts the argument to an `int` by an unsigned
         * conversion.  In an unsigned conversion to an `int`, the
         * high-order 16 bits of the `int` are zero and the
         * low-order 16 bits are equal to the bits of the `short` argument.
         *
         * Consequently, zero and positive `short` values are mapped
         * to a numerically equal `int` value and negative `short` values are mapped to an `int` value equal to the
         * input plus 2<sup>16</sup>.
         *
         * @param  x the value to convert to an unsigned `int`
         * @return the argument converted to `int` by an unsigned
         * conversion
         * @since 1.8
         */
        fun toUnsignedInt(x: Short): Int {
            return x.toInt() and 0xffff
        }

        /**
         * Compares two `long` values numerically treating the values
         * as unsigned.
         *
         * @param  x the first `long` to compare
         * @param  y the second `long` to compare
         * @return the value `0` if `x == y`; a value less
         * than `0` if `x < y` as unsigned values; and
         * a value greater than `0` if `x > y` as
         * unsigned values
         * @since 1.8
         */
        fun compareUnsigned(x: Long, y: Long): Int {
            return compare(x + Long.MIN_VALUE, y + Long.MIN_VALUE)
        }

        /**
         * Compares two `long` values numerically.
         * The value returned is identical to what would be returned by:
         * <pre>
         * Long.valueOf(x).compareTo(Long.valueOf(y))
        </pre> *
         *
         * @param  x the first `long` to compare
         * @param  y the second `long` to compare
         * @return the value `0` if `x == y`;
         * a value less than `0` if `x < y`; and
         * a value greater than `0` if `x > y`
         * @since 1.7
         */
        fun compare(x: Long, y: Long): Int {
            return if (x < y) -1 else if (x == y) 0 else 1
        }
    }
}