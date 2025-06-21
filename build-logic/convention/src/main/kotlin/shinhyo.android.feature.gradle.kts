import io.github.shinhyo.buildlogic.configureKoverAndroid
import io.github.shinhyo.buildlogic.findLibrary

with(pluginManager) {
    apply("shinhyo.android.library.compose")
    apply("org.jetbrains.kotlin.plugin.serialization")
}

configureKoverAndroid()

dependencies {
    add("implementation", findLibrary("kotlinx.serialization.json"))
    add("implementation", findLibrary("coil.kt.compose"))
    add("implementation", findLibrary("androidx.compose.material3"))
    add("implementation", findLibrary("androidx.compose.material.iconsExtended"))
    add("implementation", findLibrary("androidx.compose.animation"))
    add("implementation", findLibrary("haze"))
    add("implementation", findLibrary("androidx.navigation.compose"))
    add("implementation", findLibrary("androidx.hilt.navigation.compose"))
    add("implementation", findLibrary("androidx.lifecycle.runtimeCompose"))
    add("implementation", findLibrary("androidx.lifecycle.viewModelCompose"))
}
