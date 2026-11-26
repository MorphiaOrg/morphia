package dev.morphia.audits.semantics

import java.nio.ByteBuffer
import java.util.Base64
import org.bson.*
import org.bson.codecs.BsonDocumentCodec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.io.BasicOutputBuffer
import org.bson.types.ObjectId

/**
 * Immutable, type/order-preserving snapshot produced from a validated in-process BSON value. There
 * is deliberately no encoded-input factory: artifacts must be regenerated from their source before
 * use. value() decodes only bytes produced by this class and returns a fresh tree.
 */
class BsonSnapshot
private constructor(val base64: String, val bsonType: BsonType, val byteLength: Int) {
    fun value(): BsonValue = decode(Base64.getDecoder().decode(base64))

    override fun equals(other: Any?): Boolean = other is BsonSnapshot && base64 == other.base64

    override fun hashCode(): Int = base64.hashCode()

    override fun toString(): String = "BsonSnapshot($bsonType, $byteLength bytes)"

    companion object {
        fun of(value: BsonValue, maximumBytes: Int = 1_048_576): BsonSnapshot {
            require(maximumBytes in 32..16_777_216)
            val validation = BsonComparison.compare(value, value)
            require(validation.state == BsonComparisonState.EQUAL) {
                validation.problem ?: "Invalid BSON snapshot value"
            }
            val encoded =
                LimitedOutput(maximumBytes).use { output ->
                    BsonBinaryWriter(output).use { writer ->
                        BsonDocumentCodec()
                            .encode(
                                writer,
                                BsonDocument("value", value),
                                EncoderContext.builder().build(),
                            )
                        output.toByteArray()
                    }
                }
            // Catch UTF-8 replacement and representation loss at the serialization boundary.
            val roundTrip = BsonComparison.compare(value, decode(encoded))
            require(roundTrip.state == BsonComparisonState.EQUAL) {
                "BSON value does not survive lossless encoding: ${roundTrip.difference}"
            }
            return BsonSnapshot(
                Base64.getEncoder().encodeToString(encoded),
                value.bsonType,
                encoded.size,
            )
        }

        private fun decode(encoded: ByteArray): BsonValue =
            BsonBinaryReader(ByteBuffer.wrap(encoded)).use { reader ->
                BsonDocumentCodec()
                    .decode(reader, DecoderContext.builder().build())
                    .getValue("value")
            }
    }

    private class LimitedOutput(private val maximum: Int) : BasicOutputBuffer() {
        private fun reserve(length: Int) {
            if (length < 0 || position.toLong() + length > maximum) throw BsonSnapshotLimit(maximum)
        }

        override fun writeByte(value: Int) {
            reserve(1)
            super.writeByte(value)
        }

        override fun writeBytes(bytes: ByteArray, offset: Int, length: Int) {
            reserve(length)
            super.writeBytes(bytes, offset, length)
        }

        override fun write(bytes: ByteArray) {
            reserve(bytes.size)
            super.write(bytes)
        }

        override fun writeInt32(value: Int) {
            reserve(4)
            super.writeInt32(value)
        }

        override fun writeInt64(value: Long) {
            reserve(8)
            super.writeInt64(value)
        }

        override fun writeObjectId(value: ObjectId) {
            reserve(12)
            super.writeObjectId(value)
        }
    }
}

class BsonSnapshotLimit(maximum: Int) : RuntimeException("BSON snapshot exceeds $maximum bytes")
