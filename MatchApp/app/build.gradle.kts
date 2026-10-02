plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
}

import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

fun nonBlankEnv(name: String): String? =
    System.getenv(name)?.trim()?.takeIf { it.isNotEmpty() }

fun releaseSecret(envName: String, propertyName: String): String? =
    nonBlankEnv(envName) ?: keystoreProps.getProperty(propertyName)?.trim()?.takeIf { it.isNotEmpty() }

val productionApplicationId = nonBlankEnv("MATREE_APPLICATION_ID")?.also { value ->
    require(value != "com.match.app") {
        "MATREE_APPLICATION_ID must be the final production package, not com.match.app"
    }
    require(value.matches(Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*){2,}"))) {
        "MATREE_APPLICATION_ID must be a valid reverse-DNS Android application id"
    }
} ?: "com.match.app"

val releaseVersionCode = nonBlankEnv("MATREE_VERSION_CODE")?.let { raw ->
    raw.toIntOrNull()?.takeIf { it > 0 }
        ?: error("MATREE_VERSION_CODE must be a positive integer")
} ?: 1
val releaseVersionName = nonBlankEnv("MATREE_VERSION_NAME")?.also { value ->
    require(value.length <= 50 && value.matches(Regex("[0-9A-Za-z][0-9A-Za-z._-]*"))) {
        "MATREE_VERSION_NAME must be 1-50 safe version characters"
    }
} ?: "1.0.0"

val releaseKeystorePath = releaseSecret("MATREE_KEYSTORE_PATH", "storeFile")
val releaseKeystorePassword = releaseSecret("MATREE_KEYSTORE_PASSWORD", "storePassword")
val releaseKeyAlias = releaseSecret("MATREE_KEY_ALIAS", "keyAlias")
val releaseKeyPassword = releaseSecret("MATREE_KEY_PASSWORD", "keyPassword")
val releaseSigningInputs = listOf(
    releaseKeystorePath,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword
)
val releaseSigningConfigured = releaseSigningInputs.all { !it.isNullOrBlank() }
require(releaseSigningInputs.none { !it.isNullOrBlank() } || releaseSigningConfigured) {
    "Release signing is partially configured. Supply all MATREE_KEYSTORE_* inputs or a complete keystore.properties."
}

android {
    namespace = "com.match.app"
    compileSdk = 36

    defaultConfig {
        applicationId = productionApplicationId
        minSdk = 24
        targetSdk = 36
        versionCode = releaseVersionCode
        versionName = releaseVersionName

        vectorDrawables { useSupportLibrary = true }
        testInstrumentationRunner = "com.match.app.HiltTestRunner"
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = rootProject.file(requireNotNull(releaseKeystorePath))
                storePassword = requireNotNull(releaseKeystorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "META-INF/LICENSE*",
            "META-INF/NOTICE*"
        )
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.generateKotlin", "true")
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)

    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.biometric)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.coil.compose)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.config)
    implementation(libs.firebase.functions)
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)

    implementation(libs.google.play.services.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.identity.googleid)

    implementation(libs.play.review.ktx)
    implementation(libs.play.app.update.ktx)
    implementation(libs.play.billing.ktx)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.androidx.paging.compose)

    implementation(libs.kotlinx.serialization.json)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.android.compiler)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
