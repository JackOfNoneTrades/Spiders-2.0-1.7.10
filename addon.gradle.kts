import net.darkhax.curseforgegradle.TaskPublishCurseForge

tasks.withType<TaskPublishCurseForge>().configureEach {
    uploadArtifacts.forEach { it.addEnvironment("Client", "Server") }
}

// Runs the dedicated-server integration fixture from test sources; never included in the release jar.
if (providers.gradleProperty("spiderIntegration").isPresent) {
    val testSources = extensions.getByType<org.gradle.api.tasks.SourceSetContainer>().getByName("test")
    tasks.named<JavaExec>("runServer") {
        dependsOn(tasks.named("testClasses"))
        classpath(testSources.output)
        systemProperty("spiderstpo.integration", "true")
        doFirst {
            java.io.File((this as JavaExec).workingDir, "spiders-integration-result.txt").delete()
        }
        doLast {
            val result = java.io.File((this as JavaExec).workingDir, "spiders-integration-result.txt")
            check(result.isFile && result.readText().startsWith("PASS:")) {
                "Spider integration checks failed; see the server log and spiders-integration-result.txt"
            }
        }
    }
}
