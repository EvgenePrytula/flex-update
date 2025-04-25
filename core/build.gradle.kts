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
        create("fullRelease")
    }

    publishing {
        singleVariant("fullRelease") {
            withSourcesJar()
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("fullRelease") {
            groupId = "io.github.evgeneprytula"
            artifactId = "flex-update"
            version = "0.1.2"

            afterEvaluate {
                from(components["fullRelease"])
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

    api(libs.google.play.update)
    api(libs.google.play.update.ktx)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}