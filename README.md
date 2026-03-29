# fabric-kotlin-kickstarter
A kickstarter for a Fabric mod using Kotlin.

## Adjusting names and values to match your mod
### gradle.properties
Set `maven_group` and `mod_id` to values for your mod.

The field `mod_version` determines the version of your mod. 
The resulting version will automatically include the Minecraft version you are using: `<mod_version>+<minecraft version>`

### Package names
Modify the following package names to your liking:
- `src/main/kotlin/com/example`
- `src/main/java/com/example`
- `src/client/kotlin/com/example`
- `src/client/java/com/example`

### Rename folders
- rename folder `src/main/resources/assets/testmod`, insert your mod id instead

### fabric.mod.json
Adjust the following fields to your liking:
- `modid`
- `name` 
- `description`
- `authors`
- `contact/*`
- `icon`, `entrypoints/{main,client,fabric-datagen}`
- `mixins` (adjust the mod id in `testmod.mixins.json` and `testmod.client.mixins.json`)

### Mixins definition file names
Rename the mixin json files to use your mod id:
- `src/main/resources/testmod.mixins.json`
- `src/client/resources/testmod.client.mixins.json`

Also adjust the `package` property in both files to match your mod package name.

### Rename classes
Rename the following classes to match your mod:
- `src/main/kotlin/com/example/ExampleMod`
- `src/main/kotlin/com/example/ExampleModDataGenerator`
- `src/client/kotlin/com/example/ExampleModClient`

### build.gradle.kts
- Adjust `publishing.publications.mavenJava.pom.{name,description}`

### settings.gradle
- change rootProject.name to match your mod


## Adjust for your use case

### Server-side only mod
If you don't need the client side, do the following:
In `build.gradle.kts`, delete the loom block and the source-set split:
```diff
-loom {
-    splitEnvironmentSourceSets()
-
-    mods {
-        register(modId) {
-            sourceSet(sourceSets.getByName("main"))
-            sourceSet(sourceSets.getByName("client"))
-        }
-    }
-}
```

Also delete the processClientResources configuration:
```diff
-tasks.named<ProcessResources>("processClientResources") {
-    inputs.properties(
-        "java_version" to javaVersion,
-    )
-
-    filesMatching("$modId.client.mixins.json") {
-        expand(
-            "java_version" to javaVersion,
-        )
-    }
-}
```

Then delete the `src/client` directory.

Adjust `fabric.mod.json`:
```diff
  "entrypoints": {
    "main": [
      "com.example.ExampleMod"
    ],
-   "client": [
-     "com.example.ExampleModClient"
-   ],
    "fabric-datagen": [
      "com.example.ExampleModDataGenerator"
    ]
  },
  "mixins": [
+   "testmod.mixins.json"
-   "testmod.mixins.json",
-   {
-     "config": "testmod.client.mixins.json",
-     "environment": "client"
-   }
  ],
```

If you do testing, add this property in `build.gradle.kts`:
```diff
test {
    useJUnitPlatform()
+   systemProperty("fabric.side", "server")
}
```

### Remove data generation if needed
If you don't use data generation, you might as well remove it.

Adjust `build.gradle.kts`:
```diff
-fabricApi {
-    configureDataGeneration()
-}
```

Adjust `fabric.mod.json`:
```diff
  "entrypoints": {
    "main": [
      "com.example.ExampleMod"
+   ]
-   ],
-   "fabric-datagen": [
-     "com.example.ExampleModDataGenerator"
-   ]
  },
```

Then delete `kotlin/com/example/ExampleModDataGenerator.kt`


## Running the project
If you don't see any run configurations in IntelliJ IDEA, try reopening the project.

If the run configurations have a red error sign on them:
- close the project
- remove the `.idea/` and `.gradle/` folders from CLI or your file manager
- reopen the project, wait for Gradle sync to finish
- reopen the project one more time


## Publishing
You can publish your mod in two ways:
- locally from CLI
- via CI (GitHub, GitLab, Jenkins etc.)

### Locally using the CLI
If you want to publish your mod from CLI, you can create a `publish.properties` file in the project root.
The contents should look like this:
```properties
mavenHost=https://your.maven.repo.here
mavenUser=user
mavenPassword=password
```
When the file is not present, or doesn't contain these entries, the filesystem will be used as fallback (in the `repo/` directory).

### GitHub Actions
If you are using GitHub Actions to publish your mod, you can define Actions secrets to authenticate.
Just define `DEPLOY_URL`, `DEPLOY_USER` and `DEPLOY_PASSWORD` as Action secrets on your repository.
Don't forget to pass them as environment variables in your action definition.
