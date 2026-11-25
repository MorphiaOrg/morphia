package dev.morphia.audits.published

import dev.morphia.audits.semantics.BsonSnapshot
import org.bson.BsonType
import org.bson.types.Decimal128

/**
 * Pinned defaults modeled by normalization; execution must independently attest its
 * runtime/options.
 */
object PublishedReadProfile {
    const val ID =
        "mongosh:bd8133d2fb80bcb5951542c9b0480608dc5346cf/js-bson:7348ff59c70c4cbe39d39ba6dc3f2291569e6848/read-results:1"
}

/** A constraint justified by published text, never a guessed concrete BSON result. */
sealed interface PublishedValue {
    val kind: PublishedKind
}

enum class PublishedKind {
    DOCUMENT,
    ARRAY,
    EXACT_SCALAR,
    WHOLE_NUMBER,
    TEXT,
    DOUBLE_NAN,
    DECIMAL128_TEXT,
}

data class PublishedField(val name: String, val value: PublishedValue)

data class PublishedDocument(val fields: List<PublishedField>) : PublishedValue {
    override val kind = PublishedKind.DOCUMENT
    val keyOrder = "ECMASCRIPT_OWN_PROPERTIES"
}

data class PublishedArray(val items: List<PublishedValue>) : PublishedValue {
    override val kind = PublishedKind.ARRAY
}

data class PublishedExact(val value: BsonSnapshot) : PublishedValue {
    override val kind = PublishedKind.EXACT_SCALAR

    init {
        require(
            value.bsonType !in
                setOf(BsonType.DOCUMENT, BsonType.ARRAY, BsonType.JAVASCRIPT_WITH_SCOPE)
        )
    }
}

/** Default mongosh promotes Int32 and Double to a JS number, but keeps Long wrapped. */
data class PublishedWholeNumber(val value: Int) : PublishedValue {
    override val kind = PublishedKind.WHOLE_NUMBER
    val possibleBsonTypes = listOf(BsonType.INT32, BsonType.DOUBLE)
}

/** The pinned decoder promotes both BSON string and legacy BSON symbol to JS text. */
data class PublishedText(val value: String) : PublishedValue {
    override val kind = PublishedKind.TEXT
    val possibleBsonTypes = listOf(BsonType.STRING, BsonType.SYMBOL)
}

data object PublishedDoubleNaN : PublishedValue {
    override val kind = PublishedKind.DOUBLE_NAN
}

/** Printed Decimal128 retains its text/quantum, not an unobservable BID NaN payload. */
data class PublishedDecimal128(val value: String) : PublishedValue {
    override val kind = PublishedKind.DECIMAL128_TEXT

    init {
        require(value.length <= 1024 && Decimal128.parse(value).toString() == value)
    }
}

/** Ordinary JS own-property enumeration; numeric index keys precede ordinary string keys. */
object JavaScriptKeyOrder {
    fun index(key: String): Long? {
        if (
            key.isEmpty() ||
                key.length > 10 ||
                key.any { it !in '0'..'9' } ||
                key.length > 1 && key[0] == '0'
        )
            return null
        return key.toLongOrNull()?.takeIf { it <= 4_294_967_294L }
    }

    fun order(keys: List<String>): List<String> =
        keys.filter { index(it) != null }.sortedBy { index(it) } + keys.filter { index(it) == null }
}
