package com.lansent.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.compile.JavaCompile

/**
 *
 * @author <a href="mailto:hyysguyang@gmail.com">Young Gu</a>
 */
class Build(val root: Project) {

    val isProd = "prod".equals(root.properties["profile"]?.toString(), true)

    fun configAllProject(): Unit {
        root.allprojects.forEach {
            with(it) {
                group = "com.lifecosys.demo"
                version = "1.0-SNAPSHOTS"
                repositories.mavenLocal()
                repositories.mavenCentral()
            }
        }

    }

    fun configWeb(): Unit {
        val web = root.project(":launcher")
        with(web) {
            val javaPluginConvention = getConvention().getPlugin(JavaPluginConvention::class.java)


            val mainSourceSet = javaPluginConvention.sourceSets.getByName("main")

            with(javaPluginConvention) {
                setSourceCompatibility(1.8)
                setTargetCompatibility(1.8)
                with(sourceSets.getByName("main").resources) {
                    srcDirs("src/main/resources")
                    exclude("webapp/js", "webapp/css/*.css")
                }
            }

            tasks.withType(JavaCompile::class.java) {
                it.options.compilerArgs.add("-parameters")
            }


            fun processDependency() {
                val dependency = Dependency(dependencies)
                dependency.compile().forEach { dependencies.add("compile", it) }
                dependency.testCompile().forEach { dependencies.add("testCompile", it) }
                val lifecosysTesting = dependencies.add("testCompile", "com.lifecosys:lifecosys-testing:0.2") as ModuleDependency
                lifecosysTesting.exclude(mapOf("group" to "org.scalatest", "module" to "scalatest_2.11"))
            }

            processDependency()

        }

    }

}

