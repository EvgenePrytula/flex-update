import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    `maven-publish`
    id("com.vanniktech.maven.publish") version "0.31.0"
}

android {
    namespace = "com.madappgang.flexupdate.core"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.madappgang.flexupdate.core"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    buildTypes {
        debug { }
        release { }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.madappgang.flexupdate"
            artifactId = "core"
            version = "0.1"

            afterEvaluate {
                from(components["release"])
            }

            pom {
                scm {
                    connection.set("scm:git:git://github.com/")
                }
            }
        }
    }

    repositories {
        mavenLocal()

        maven {
            name = "buildDir"
            url = uri(rootProject.layout.buildDirectory.dir("maven-repo"))
        }
    }

    mavenPublishing {
        publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
        signAllPublications()
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)

    // In-app update
    implementation(libs.google.play.update)
    implementation(libs.google.play.update.ktx)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}