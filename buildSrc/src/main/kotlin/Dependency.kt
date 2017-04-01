package com.lansent.gradle

import org.gradle.api.artifacts.dsl.DependencyHandler

/**
 *
 * @author <a href="mailto:hyysguyang@gmail.com">Young Gu</a>
 */
class Dependency(internal var dependencyHandler: DependencyHandler) {
    val springBootVersion = "1.4.0.RELEASE"
    val springWeb = dependencyHandler.create("org.springframework.boot:spring-boot-starter-web:" + springBootVersion)
    val springwebsocket = dependencyHandler.create("org.springframework:spring-websocket:4.2.7.RELEASE")
    val kurentoClient = dependencyHandler.create("org.kurento:kurento-client:6.6.0")
//    val kurentoUtilsjs = dependencyHandler.create("org.kurento:kurento--utils-js:6.6.0")
//    val jquery = dependencyHandler.create("org.webjars.bower:jquery:1.12.3")
//    val bootstrap = dependencyHandler.create("org.webjars.bower:bootstrap:3.3.6")
//    val locator = dependencyHandler.create("org.webjars:webjars-locator:0.32")
//    val console = dependencyHandler.create("org.webjars.bower:demo-console:1.5.1")
//    val adapter = dependencyHandler.create("org.webjars.bower:adapter.js:0.2.9")
//    val lightbox = dependencyHandler.create("org.webjars.bower:ekko-lightbox:4.0.2")


    fun compile() = listOf(springWeb, springwebsocket, kurentoClient/*, kurentoUtilsjs, jquery, bootstrap, locator, console, adapter, lightbox*/)

    fun testCompile() = listOf(springWeb)


}
