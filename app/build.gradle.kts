plugins {
    id("com.android.application")
}

android {
    namespace = "com.openai.threekingdoms"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.openai.threekingdoms"
        minSdk = 23
        targetSdk = 37
        versionCode = 24
        versionName = "2.4.0"
    }
}
