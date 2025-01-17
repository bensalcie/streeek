plugins {
    id("bizilabs.convention.feature")
}

android {
    namespace = "com.bizilabs.streeek.feature.updater"
}

dependencies {
    implementation(libs.updater)
}
