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
        versionCode = 27
        versionName = "2.5.1"
        testInstrumentationRunner = "com.openai.threekingdoms.test.BattleRegression"
    }
}
