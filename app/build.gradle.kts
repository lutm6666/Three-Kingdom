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
        versionCode = 23
        versionName = "2.3.0"
    }
}
