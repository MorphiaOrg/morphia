package dev.morphia.mapping.codec.kotlin

import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@Suppress("UNCHECKED_CAST")
class ReadWritePropertyCodec<T>(private val codec: Codec<Any>, private val type: Class<T>) : Codec<Any> {
    private lateinit var property: KProperty<T>
    override fun getEncoderClass(): Class<Any> = ReadWriteProperty::class.java as Class<Any>
    override fun encode(writer: BsonWriter, value: Any, context: EncoderContext) {
        context.encodeWithChildContext(codec, writer, value)
    }

    override fun decode(reader: BsonReader, context: DecoderContext): Any? {
        return codec.decode(reader, context)
    }
}
