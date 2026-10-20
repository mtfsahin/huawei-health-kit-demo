import java.net.URI
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun local(key: String, default: String = ""): String = localProperties.getProperty(key, default)

fun buildConfigString(value: String): String = "\"$value\""

val deepLink = URI(local("healthkit.deepLink", "healthkitdemo://oauth/callback"))

android {
    namespace = "com.example.healthkitdemo"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.healthkitdemo"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "HEALTH_KIT_CLIENT_ID", buildConfigString(local("healthkit.clientId")))
        buildConfigField("String", "HEALTH_KIT_DEEP_LINK", buildConfigString(deepLink.toString()))
        buildConfigField("String", "HEALTH_KIT_SCHEME_SECRET", buildConfigString(local("healthkit.schemeSecret")))
        buildConfigField("String", "HEALTH_KIT_API_HOST", buildConfigString(local("healthkit.apiHost")))
        buildConfigField("String", "HEALTH_KIT_AUTH_METHOD", buildConfigString(local("healthkit.authMethod", "WEB_OAUTH")))
        buildConfigField("String", "HEALTH_KIT_HISTORY_WINDOW", buildConfigString(local("healthkit.historyWindow", "NONE")))
        buildConfigField("String", "BACKEND_BASE_URL", buildConfigString(local("backend.baseUrl")))

        manifestPlaceholders["huaweiAppId"] = local("healthkit.clientId")
        manifestPlaceholders["deepLinkScheme"] = deepLink.scheme
        manifestPlaceholders["deepLinkHost"] = deepLink.host
    }

    signingConfigs {
        if (keystoreProperties.containsKey("storeFile")) {
            create("registered") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        val registered = signingConfigs.findByName("registered")
        debug {
            if (registered != null) signingConfig = registered
        }
        release {
            if (registered != null) signingConfig = registered
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.fragment)
    implementation(libs.huawei.healthlite)
    implementation(libs.huawei.push)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
