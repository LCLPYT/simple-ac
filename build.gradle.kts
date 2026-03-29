import net.fabricmc.loom.task.RemapJarTask
import work.lclpnet.build.task.GithubDeploymentTask
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.gradle.build.tools)
}

val props: Properties = buildUtils.loadProperties("publish.properties")  // will be empty, if the file is missing
val env: Map<String, String> = System.getenv()

version = "${project.property("mod_version")!!}+${libs.versions.minecraft.get()}"
group = project.property("maven_group")!!

val modId = project.property("mod_id")!!.toString()

base {
    archivesName.set(modId)
}

val javaVersion = libs.versions.java.get().toInt()

repositories {
    maven {
        url = uri("https://repo.lclpnet.work/repository/internal")
    }
}

loom {
    splitEnvironmentSourceSets()

    mods {
        register(modId) {
            sourceSet(sourceSets.getByName("main"))
            sourceSet(sourceSets.getByName("client"))
        }
    }
}

dependencies {
    minecraft(libs.minecraft)

    mappings(loom.officialMojangMappings())

    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.api)
    modImplementation(libs.fabric.language.kotlin)

    modImplementation(libs.kibu)

    testImplementation(libs.fabric.loader.junit)
    testImplementation(libs.kotlin.test)
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    inputs.properties(
        "version" to project.version,
        "loader_version" to libs.versions.fabric.loader.get(),
        "minecraft_compat" to project.property("minecraft_compat")!!,
        "java_version" to javaVersion,
        "fabric_language_kotlin" to libs.versions.fabric.language.kotlin.get(),
    )

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "loader_version" to libs.versions.fabric.loader.get(),
            "minecraft_compat" to project.property("minecraft_compat")!!,
            "java_version" to javaVersion,
            "fabric_language_kotlin" to libs.versions.fabric.language.kotlin.get(),
        )
    }

    filesMatching("$modId.mixins.json") {
        expand(
            "java_version" to javaVersion,
        )
    }
}

tasks.named<ProcessResources>("processClientResources") {
    inputs.properties(
        "java_version" to javaVersion,
    )

    filesMatching("$modId.client.mixins.json") {
        expand(
            "java_version" to javaVersion,
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(javaVersion)
}

java {
    withSourcesJar()

    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
}

kotlin {
    jvmToolchain(javaVersion)
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${base.archivesName.get()}" }
    }
}


tasks.register<GithubDeploymentTask>("github") {
    val artifactTask = tasks.getByName<RemapJarTask>("remapJar")

    dependsOn(artifactTask)

    config {
        token = env["GITHUB_TOKEN"]
        repository = env["GITHUB_REPOSITORY"]
    }

    release {
        title = "[${libs.versions.minecraft}] ${project.name} ${project.version}"
        tag = project.version.toString()
    }

    assets.add(artifactTask.archiveFile.get())
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = base.archivesName.get()

            from(components["java"])

            pom {
                name.set("simple-ac")
                description.set("Simple anti-cheat for Minecraft servers ")
            }
        }
    }

    // automatically use DEPLOY_URL, DEPLOY_USER and DEPLOY_PASSWORD environment variables
    // or mavenHost, mavenUser and mavenPassword from props
    buildUtils.setupPublishRepository(repositories, props)
}