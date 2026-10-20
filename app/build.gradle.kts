import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.io.File
import java.net.URL
import java.security.MessageDigest

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.baqiyat.azkar"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.baqiyat.azkar"
    minSdk = 24
    targetSdk = 36

    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  val debugKeystore = file("${rootDir}/debug.keystore")

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    if (debugKeystore.exists()) {
      create("debugConfig") {
        storeFile = debugKeystore
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      if (debugKeystore.exists()) {
        signingConfig = signingConfigs.getByName("debugConfig")
      }
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  sourceSets.getByName("main").assets.srcDir(layout.projectDirectory.dir("local-ai-model"))
  androidResources { noCompress += "litertlm" }

  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

val localAiModel = layout.projectDirectory.file("local-ai-model/Qwen2_0.5B_Instruct.litertlm")
val localAiModelUrl =
  "https://huggingface.co/litert-community/Qwen2-0.5B-Instruct/resolve/main/Qwen2_0.5B_Instruct.litertlm?download=true"
val localAiModelSha256 = "0f01cc004b8eb62b92ba6be85ed05a248ba0d2f78af94c4949b313eccfb4c157"

val downloadLocalAiModel by tasks.registering {
  notCompatibleWithConfigurationCache("Downloads and verifies the bundled model using build-script scoped logic")
  outputs.file(localAiModel)
  outputs.upToDateWhen { false }
  doLast {
    val target = localAiModel.asFile
    target.parentFile.mkdirs()

    fun sha256(file: File): String {
      val digest = MessageDigest.getInstance("SHA-256")
      file.inputStream().buffered().use { input ->
        val buffer = ByteArray(1024 * 1024)
        while (true) {
          val count = input.read(buffer)
          if (count < 0) break
          digest.update(buffer, 0, count)
        }
      }
      return digest.digest().joinToString("") { "%02x".format(it) }
    }

    if (target.isFile && sha256(target) == localAiModelSha256) {
      logger.lifecycle("Verified cached local AI model.")
    } else {
      val temporary = File(target.parentFile, target.name + ".download")
      temporary.delete()
      logger.lifecycle("Downloading the bundled on-device model (~647 MB); this is only needed when the model is not cached.")
      val connection = URL(localAiModelUrl).openConnection().apply {
        connectTimeout = 30_000
        readTimeout = 600_000
        setRequestProperty("User-Agent", "Baqiyat-Android-Build")
      }
      connection.getInputStream().buffered().use { input ->
        temporary.outputStream().buffered().use { output -> input.copyTo(output) }
      }
      val actualSha = sha256(temporary)
      if (actualSha != localAiModelSha256) {
        temporary.delete()
        throw GradleException("Local AI model checksum mismatch: expected $localAiModelSha256, got $actualSha")
      }
      if (target.exists() && !target.delete()) throw GradleException("Could not replace invalid local AI model file")
      if (!temporary.renameTo(target)) throw GradleException("Could not install downloaded local AI model")
      logger.lifecycle("Downloaded and verified local AI model.")
    }
  }
}

tasks.named("preBuild").configure { dependsOn(downloadLocalAiModel) }
tasks.configureEach {
  if (name.startsWith("merge") && name.endsWith("Assets")) {
    dependsOn(downloadLocalAiModel)
  }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation("androidx.compose.ui:ui-text-google-fonts")
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.firebase.firestore)
  implementation("com.google.ai.edge.litertlm:litertlm-android:0.18.0")
  implementation(libs.firebase.auth)
  implementation(libs.firebase.messaging)
  implementation(libs.okhttp)
  implementation("com.google.firebase:firebase-functions")
  implementation("com.google.firebase:firebase-storage")
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)
  implementation(libs.googleid)
  debugImplementation(libs.firebase.appcheck.debug)
  releaseImplementation(libs.firebase.appcheck.playintegrity)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.play.services)
  implementation(libs.kotlinx.coroutines.core)
  implementation("androidx.work:work-runtime-ktx:2.9.0")
  implementation("androidx.glance:glance-appwidget:1.1.1")
  implementation("androidx.glance:glance-material3:1.1.1")
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
}
