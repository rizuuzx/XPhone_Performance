plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.xphone.performance"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.xphone.performance"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // Bật R8: thu gọn code/resource không dùng, tối ưu bytecode => app nhẹ và chạy nhanh hơn.
            // Đo hiệu năng nên đo trên bản release, vì bản debug chạy chậm hơn đáng kể.
            optimization {
                enable = true
            }
            // Ký bằng debug key để cài được bản release lên máy ảo / máy thật khi đo đạc
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.recyclerview)
    // LeakCanary: chỉ có trong bản debug, tự động phát hiện memory leak và hiện thông báo
    debugImplementation(libs.leakcanary)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}