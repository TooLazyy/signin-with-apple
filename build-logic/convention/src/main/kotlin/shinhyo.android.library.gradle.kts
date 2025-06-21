import io.github.shinhyo.buildlogic.configureHiltAndroid
import io.github.shinhyo.buildlogic.configureKotlinAndroid
import io.github.shinhyo.buildlogic.findLibrary

with(pluginManager) {
    apply("com.android.library")
    apply("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    add("implementation", findLibrary("timber"))
}

configureKotlinAndroid()
configureHiltAndroid()
