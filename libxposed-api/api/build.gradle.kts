plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "io.github.libxposed.api"
    compileSdk = 36
    defaultConfig { minSdk = 27 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    compileOnly("androidx.annotation:annotation:1.9.1")
}
