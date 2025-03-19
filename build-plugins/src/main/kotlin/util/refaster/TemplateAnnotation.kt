package util.refaster

enum class TemplateAnnotation {
    AFTER {
        override fun errorProne() = "com.google.errorprone.refaster.annotation.AfterTemplate"

        override fun shortName() = "RefasterAfterTemplate"
    },
    BEFORE {
        override fun errorProne() = "com.google.errorprone.refaster.annotation.BeforeTemplate"

        override fun shortName() = "RefasterBeforeTemplate"
    };

    abstract fun errorProne(): String

    abstract fun shortName(): String
}
