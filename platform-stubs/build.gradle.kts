// Compile-only stubs for Android system APIs that are absent from the public SDK.
// They are never packaged: the real classes come from the platform at runtime.
plugins {
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
