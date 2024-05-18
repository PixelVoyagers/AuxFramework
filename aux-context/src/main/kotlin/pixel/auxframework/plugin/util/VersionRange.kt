package pixel.auxframework.plugin.util

import com.google.common.collect.Range
import java.nio.CharBuffer


class VersionRange(val range: Range<Version>) {

    constructor(rangeString: String) : this(Parser.parse(rangeString))

    @OptIn(ExperimentalUnsignedTypes::class)
    class Version(val version: ULongArray) : Comparable<Version> {
        override fun compareTo(other: Version): Int {
            val version1 = version
            val version2 = other.version
            val length = minOf(version1.size, version2.size)
            for (i in 0 until length) {
                val comparison = version1[i].compareTo(version2[i])
                if (comparison != 0) {
                    return comparison
                }
            }
            return version1.size.compareTo(version2.size)
        }
    }

    object Parser {

        @OptIn(ExperimentalUnsignedTypes::class)
        fun toVersion(versionString: String) =
            Version(versionString.split(".").map(String::toLong).toLongArray().toULongArray())

        fun parse(rangeString: String): Range<Version> {
            if (rangeString == "*") return Range.all()
            val buffer = CharBuffer.wrap(rangeString)
            skipWhitespace(buffer)
            val lowerBound = buffer.get()
            skipWhitespace(buffer)
            val lowerVersion = parseVersion(buffer)
            skipWhitespace(buffer)
            expect(buffer, ',')
            skipWhitespace(buffer)
            val upperVersion = parseVersion(buffer)
            skipWhitespace(buffer)
            val upperBound = buffer.get()
            return if (lowerVersion == "" && upperVersion == "" && lowerBound == '(' && upperBound == ')') Range.all()
            else if (lowerVersion.isEmpty() && lowerBound == '(') when (upperBound) {
                ')' -> Range.lessThan(toVersion(upperVersion))
                ']' -> Range.atMost(toVersion(upperVersion))
                else -> throw IllegalArgumentException("Invalid bound type")
            } else if (upperVersion.isEmpty() && upperBound == ')') when (lowerBound) {
                '(' -> Range.greaterThan(toVersion(lowerVersion))
                '[' -> Range.atLeast(toVersion(lowerVersion))
                else -> throw IllegalArgumentException("Invalid bound type")
            } else when (lowerBound) {
                '(' -> when (upperBound) {
                    ')' -> Range.open(toVersion(lowerVersion), toVersion(upperVersion))
                    ']' -> Range.openClosed(toVersion(lowerVersion), toVersion(upperVersion))
                    else -> throw IllegalArgumentException("Invalid bound type")
                }

                '[' -> when (upperBound) {
                    ')' -> Range.closedOpen(toVersion(lowerVersion), toVersion(upperVersion))
                    ']' -> Range.closed(toVersion(lowerVersion), toVersion(upperVersion))
                    else -> throw IllegalArgumentException("Invalid bound type")
                }

                else -> throw IllegalArgumentException("Invalid bound type")
            }
        }

        private fun parseVersion(buffer: CharBuffer): String {
            val builder = StringBuilder()
            while (buffer.hasRemaining()) {
                val c = buffer.get()
                if (c == ',' || c == ')' || c == ']' || Character.isWhitespace(c)) {
                    buffer.position(buffer.position() - 1)
                    break
                }
                builder.append(c)
            }
            return builder.toString()
        }

        private fun skipWhitespace(buffer: CharBuffer) {
            while (buffer.hasRemaining() && buffer.get(buffer.position()).isWhitespace()) {
                buffer.get()
            }
        }

        @Suppress("SameParameterValue")
        private fun expect(buffer: CharBuffer, vararg expected: Char) {
            if (!buffer.hasRemaining() || !expected.contains(buffer.get())) {
                throw IllegalArgumentException(
                    "Expected one of ${expected.contentToString()}, but found: ${
                        buffer.get(
                            buffer.position()
                        )
                    }"
                )
            }
        }
    }

    fun test(input: String) = range.test(Parser.toVersion(input))

}

