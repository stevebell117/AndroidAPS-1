plugins {
    id("com.android.library")
    id("kotlin-android")
    id("android-module-dependencies")
}

android {
    namespace = "app.aaps.core.ui"
    defaultConfig {
        minSdk = Versions.wearMinSdk
    }
}

dependencies {
    api(Libs.AndroidX.core)
    api(Libs.AndroidX.appCompat)
    api(Libs.AndroidX.preference)
    api(Libs.AndroidX.gridLayout)


    api(Libs.Google.Android.material)

    api(Libs.Dagger.android)
    api(Libs.Dagger.androidSupport)
}