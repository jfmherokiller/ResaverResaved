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
            return (x + Long.MIN_VALUE).compareTo(y + Long.MIN_VALUE)
        }

        /**
         * Parses the string argument as an unsigned integer in the radix
         * specified by the second argument.  An unsigned integer maps the
         * values usually associated with negative numbers to positive
         * numbers larger than `MAX_VALUE`.
         *
         * The characters in the string must all be digits of the
         * specified radix (as determined by whether [ ][java.lang.Character.digit] returns a nonnegative
         * value), except that the first character may be an ASCII plus
         * sign `'+'` (`'\u005Cu002B'`). The resulting
         * integer value is returned.
         *
         *
         * An exception of type `NumberFormatException` is
         * thrown if any of the following situations occurs:
         *
         *  * The first argument is `null` or is a string of
         * length zero.
         *
         *  * The radix is either smaller than
         * [java.lang.Character.MIN_RADIX] or
         * larger than [java.lang.Character.MAX_RADIX].
         *
         *  * Any character of the string is not a digit of the specified
         * radix, except that the first character may be a plus sign
         * `'+'` (`'\u005Cu002B'`) provided that the
         * string is longer than length 1.
         *
         *  * The value represented by the string is larger than the
         * largest unsigned `int`, 2<sup>32</sup>-1.
         *
         * @param      s   the `String` containing the unsigned integer
         * representation to be parsed
         * @param      radix   the radix to be used while parsing `s`.
         * @return     the integer represented by the string argument in the
         * specified radix.
         * @throws     NumberFormatException if the `String`
         * does not contain a parsable `int`.
         * @since 1.8
         */
        @Throws(NumberFormatException::class)
        fun parseUnsignedInt(s: String?, radix: Int): Int {
            if (s == null) {
                throw NumberFormatException("null")
            }
            val len = s.length
            return if (len > 0) {
                val firstChar = s[0]
                if (firstChar == '-') {
                    throw NumberFormatException("Illegal leading minus sign on unsigned string $s.")
                } else {
                    if (len <= 5 ||  // Integer.MAX_VALUE in Character.MAX_RADIX is 6 digits
                        radix == 10 && len <= 9
                    ) { // Integer.MAX_VALUE in base 10 is 10 digits
                        s.toInt(radix)
                    } else {
                        val ell = s.toLong(radix)
                        if (ell and -0x100000000L == 0L) {
                            ell.toInt()
                        } else {
                            throw NumberFormatException("String value $s exceeds range of unsigned int.")
                        }
                    }
                }
            } else {
                throw NumberFormatException()
            }
        }

        /**
         * Compares two `int` values numerically treating the values
         * as unsigned.
         *
         * @param  x the first `int` to compare
         * @param  y the second `int` to compare
         * @return the value `0` if `x == y`; a value less
         * than `0` if `x < y` as unsigned values; and
         * a value greater than `0` if `x > y` as
         * unsigned values
         * @since 1.8
         */
        fun compareUnsigned(x: Int, y: Int): Int {
            return (x + Int.MIN_VALUE).compareTo(y + Int.MIN_VALUE)
        }
    }
}