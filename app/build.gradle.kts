plugins { id("com.android.application") }
android {
 namespace = "org.aicollective.nexus"
 compileSdk = 35
 defaultConfig { applicationId = "org.aicollective.nexus"; minSdk = 26; targetSdk = 35; versionCode = 6; versionName = "2.0" }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
