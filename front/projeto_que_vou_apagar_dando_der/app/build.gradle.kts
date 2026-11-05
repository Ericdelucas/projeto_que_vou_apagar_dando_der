plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 3
        versionName = "1.2"
        val apiUrl = providers.gradleProperty("skippecasApiUrl").orElse("https://skippecas-api.onrender.com/api/").get()
        require(apiUrl.endsWith("/api/")) { "skippecasApiUrl deve terminar em /api/" }
        require(apiUrl == "http://localhost:3000/api/" || apiUrl.startsWith("https://")) {
            "Use HTTPS para a API publicada."
        }
        buildConfigField("String", "API_BASE_URL", "\"${apiUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    testImplementation(libs.junit)
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}

// Falha a compilação se o código Kotlin/Java introduzir warnings.
kotlin {
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}
tasks.withType<JavaCompile>().configureEach {
    // O AGP gera comentário Javadoc antes do package em BuildConfig.java.
    // Essa categoria do JDK acusa o arquivo gerado; os demais warnings seguem fatais.
    options.compilerArgs.addAll(listOf("-Xlint:all,-dangling-doc-comments", "-Werror"))
}
