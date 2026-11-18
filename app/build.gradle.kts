plugins { id("com.android.application") }
android {
 namespace = "org.aicollective.nexus"
 compileSdk = 35
 defaultConfig { applicationId = "org.aicollective.nexus"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "1.5" }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
