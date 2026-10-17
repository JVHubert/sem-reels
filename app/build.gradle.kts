plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// No GitHub Actions cada build ganha um número maior, para instalar por cima da versão antiga.
val numeroBuild = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "io.github.jvhubert.semreels"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.jvhubert.semreels"
        minSdk = 26
        targetSdk = 35
        versionCode = numeroBuild
        versionName = "0.2.$numeroBuild"
    }

    signingConfigs {
        create("release") {
            // A chave fica fora do repositório; o GitHub Actions recebe pelos secrets.
            System.getenv("CHAVE_ARQUIVO")?.let { arquivo ->
                storeFile = file(arquivo)
                storeType = "pkcs12"
                storePassword = System.getenv("CHAVE_SENHA")
                keyAlias = System.getenv("CHAVE_ALIAS")
                keyPassword = System.getenv("CHAVE_SENHA")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    testImplementation("junit:junit:4.13.2")
}
