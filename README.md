# ktor-workshop-2025

This project was created using the [Ktor Project Generator](https://start.ktor.io).

Here are some useful links to get you started:

- [Ktor Documentation](https://ktor.io/docs/home.html)
- [Ktor GitHub page](https://github.com/ktorio/ktor)
- The [Ktor Slack chat](https://slack-chats.kotlinlang.org/c/ktor).

The solution is available on the `solution` branch, to run the project:
```shell
docker-compose up -d
./gradlew run
```

Visit [`http://localhost:8080`](http://localhost:8080) from any browser. Log in using:
username: ktor_user
password: ktor_password

If you have Ollama running with `llama3` then it'll automatically connect, otherwise it'll use some stubbed impl.
You can change the model in `backend/src/main/resources/application.yaml`.

The slides are also available on the `solution` branch.

## Branches

- main: Initial state. Generated using [start.ktor.io](https://start.ktor.io/settings?name=kotlinconf&website=jetbrains.org&artifact=org.jetbrains.kotlinconf&kotlinVersion=2.1.10&ktorVersion=3.1.2&buildSystem=GRADLE_KTS&buildSystemArgs.version_catalog=true&engine=NETTY&configurationIn=YAML&addSampleCode=true&plugins=routing).
- branch01: First tests—empty
- branch02: First tests—implementation
- branch03: CRUD implementation
- branch04: Adding structure and DI
- branch05: Database access with Exposed - basics
- branch06: Database access with Exposed - adding relations
- branch07: Database access with Exposed - adding entities
- (branch08: Integration testing with TestContainers)
- branch09: Authentication with OAUth2, sessions and JWT
- branch10: WebSocket & SSE with KotlinX Serialization
- branch 11: Finished workshop project

## Building & Running

To build or run the project, use one of the following tasks:

| Task                          | Description                                                          |
| -------------------------------|---------------------------------------------------------------------- |
| `./gradlew test`              | Run the tests                                                        |
| `./gradlew build`             | Build everything                                                     |
| `buildFatJar`                 | Build an executable JAR of the server with all dependencies included |
| `buildImage`                  | Build the docker image to use with the fat JAR                       |
| `publishImageToLocalRegistry` | Publish the docker image locally                                     |
| `run`                         | Run the server                                                       |
| `runDocker`                   | Run using the local docker image                                     |

If the server starts successfully, you'll see the following output:

```
2024-12-04 14:32:45.584 [main] INFO  Application - Application started in 0.303 seconds.
2024-12-04 14:32:45.682 [main] INFO  Application - Responding at http://0.0.0.0:8080
```
