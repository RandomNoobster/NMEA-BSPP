plugins {
    id("com.android.library")
}

android {
    namespace = "com.geomatikk.bspp.plugin"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        targetSdk = 34
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    // Add any dependencies needed for your logic here
}
