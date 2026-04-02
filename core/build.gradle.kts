import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.com.android.library)
    alias(libs.plugins.kotlin.android)
    `maven-publish`
    alias(libs.plugins.com.vanniktech.maven.publish)
}

android {
    namespace = "com.madappgang.flexupdate.core"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

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
}

publishing {
    publications {
        create<MavenPublication>("release") {
            afterEvaluate {
                from(components["release"])
                tasks.withType<PublishToMavenRepository> {
                    dependsOn(tasks.withType<Sign>())
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
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("app.cash.turbine:turbine:1.2.0")
    testImplementation(libs.robolectric)
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("com.google.android.play:app-update:2.1.0")
    testImplementation("com.google.android.play:app-update-ktx:2.1.0")
    testImplementation("com.google.android.play:integrity:1.4.0")
    testImplementation("com.google.android.gms:play-services-basement:18.5.0")
    testImplementation("com.google.android.gms:play-services-tasks:18.2.0")

    api(libs.google.play.update)
    api(libs.google.play.update.ktx)
    implementation(libs.kotlinx.coroutines.play.services)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation("app.cash.turbine:turbine:1.2.0")
    androidTestImplementation("com.google.android.play:app-update:2.1.0")
    androidTestImplementation("com.google.android.play:app-update-ktx:2.1.0")
    androidTestImplementation("com.google.android.play:integrity:1.4.0")
    androidTestImplementation("com.google.android.gms:play-services-basement:18.5.0")
    androidTestImplementation("com.google.android.gms:play-services-tasks:18.2.0")
}