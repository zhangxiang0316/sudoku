# The app uses no reflection over its own classes, so the default R8 rules are enough.
# Keep the Kotlin metadata that Compose relies on for its generated code.
-keepattributes *Annotation*, InnerClasses, Signature
-dontwarn org.jetbrains.annotations.**
