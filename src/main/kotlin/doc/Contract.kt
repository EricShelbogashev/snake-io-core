package doc

@Repeatable
@Target(
    allowedTargets = [
        AnnotationTarget.CLASS,
        AnnotationTarget.ANNOTATION_CLASS,
        AnnotationTarget.FUNCTION,
        AnnotationTarget.TYPE
    ]
)
annotation class Contract(val requirement: String)