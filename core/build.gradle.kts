import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    `maven-publish`
    id("com.vanniktech.maven.publish") version "0.31.0"
}

val libVersion = "0.1.1"

android {
    namespace = "com.madappgang.flexupdate.core"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.madappgang.flexupdate.core"
        minSdk = 24
        targetSdk = 35
        versionCode = generateVersionCode(libVersion)
        versionName = libVersion

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
            groupId = "io.github.evgeneprytula"
            artifactId = "flex-update"
            version = libVersion

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("FlexUpdate")
                description.set("A library to manage the In-app update integration in Android")
                url.set("https://github.com/EvgenePrytula/flex-update")

                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }

                developers {
                    developer {
                        id.set("eugeneprytula")
                        name.set("Eugene Prytula")
                        email.set("ep@madappgang.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/EvgenePrytula/flex-update")
                    developerConnection.set("scm:git:ssh://github.com/EvgenePrytula/flex-update")
                    url.set("http://github.com/EvgenePrytula/flex-update")
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

fun generateVersionCode(versionName: String): Int {
    val parts = versionName.split(".").map { it.toIntOrNull() ?: 0 }

    val major = parts.getOrElse(0) { 0 }
    val minor = parts.getOrElse(1) { 0 }
    val patch = parts.getOrElse(2) { 0 }

    return major * 10000 + minor * 100 + patch
}