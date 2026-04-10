# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

DMS (Docker Management System) is a Kubernetes-like container orchestration system written in Groovy/Java. It manages Docker containers and host machine processes across distributed worker nodes, with a web UI for administration. Requires JDK 21+ and Gradle 8+.

## Build Commands

Each subproject has its own `settings.gradle` and must be built from its own directory.

```bash
# Initialize submodules (required first time)
git submodule update --init

# Build agent (must be done before server)
cd dms_agent && gradle tar

# Build server (includes copying agent tar, www, plugins, dependencies)
cd dms && gradle buildToRun

# Run server
cd dms/build/libs && java -cp . -jar dms_server-1.2.jar

# Run tests (Spock framework, JUnit Platform)
cd dms && gradle test
cd dms_common && gradle test          # also generates JaCoCo coverage report
cd dms_agent && gradle test
```

## Architecture

Three Gradle subprojects plus three git submodule dependencies:

### Subprojects
- **`dms/`** - Server: web UI, REST controllers, orchestration (Guardian scheduler), plugin system, Redis manager, DNS server. Entry point: `src/RunServer.groovy`. Ports 5010/5011.
- **`dms_agent/`** - Agent: runs on worker nodes, manages Docker containers (via docker-java SDK) and host machine processes, reports metrics/heartbeat to server. Entry point: `src/RunAgent.groovy`. Builds as fat jar → `agentV2.tar.gz`.
- **`dms_common/`** - Shared models, SSH deployment utilities (`CmdExecutor`), container/node transfer objects.

### Git Submodules (in `segment_common/`, `segmentd/`, `segmentweb/`)
- **segment_common** - Core utilities library
- **segmentd** - Database abstraction layer (the `D` class for DB operations)
- **segmentweb** - Web framework (`RouteServer`, Jetty-based HTTP routing)

### Key Server Components
- **Controllers** (`dms/src/ctrl/`) - REST endpoints for apps, containers, clusters, nodes, jobs, plugins, Redis management
- **Guardian** (`dms/src/server/Guardian`) - Main scheduler loop for orchestration (health checks, scaling, container lifecycle)
- **Plugin system** (`dms/src/plugin/`, `dms/plugins/`) - Extensible via Groovy classes implementing `Plugin` interface. Built-in plugins for MySQL, Redis, PostgreSQL, Prometheus, Grafana, Traefik, etc.
- **Redis Manager** (`dms/src/rm/`) - Full Redis cluster/sentinel management with backup, failover, scaling, data transfer

### AI Agent Guides
- For legacy admin frontend conversion work, read `doc/web_pages_code_conversion.md`
- For backend web/API conversion work, read `doc/web_api_code_conversion.md`

## Feature Implement Steps

- Use two AI roles for feature delivery: one implementer and one reviewer.
- Implement in this order: DDL first, then web API, then web pages.
- Each stage must be reviewed before moving to the next stage.
- Record every review round in `doc/feat/<feature_name>/review_round{N}.md`.
- Kafka manager review files must use `doc/feat/kafka_manager/review_round{N}.md`.
- DDL tip for new features: update both `dms/init_h2.sql` and `dms/ddl_update.sql`; if old deployments may need repair or schema validation after `ddl_update.sql`, add or update `support/DDLPostChecker.groovy` and keep `RunServer.groovy` fail-fast on post-check errors.

### Key Agent Components
- **Controllers** (`dms_agent/src/agent/ctrl/`) - Container CRUD, image pull, log viewing, file ops, script execution
- **ContainerCreate** (`dms_agent/src/agent/`) - Container provisioning with mount file generation

## Conventions

- Source is in `src/` directories (not `src/main/groovy`), tests in `test/`
- Plugins live in `dms/plugins/` with resources in `dms/plugins_resources/`
- Web UI is vanilla JavaScript in `dms/www/`
- Configuration via `conf.properties` files
- Commit messages follow `type: description` format (feat, fix, build, doc, style, refactor, test, perf, layout)
- JSON handling uses FastJSON (`com.alibaba.fastjson`)
- SSH operations use JSch

## Groovy Code Style (see `dms/test/GroovyStyleRefer.groovy`)

- No semicolons
- Use `def` for variable declarations, not `var` or explicit types
- Use Groovy literal syntax for collections: `[1, 2, 3]`, `[a: 1, b: 2]`
- Classes and methods are public by default — omit `public`/`private` modifiers
- Omit parentheses on closure-taking methods: `list.each { println it }`
- Omit `return` when the last expression is the return value
- Always declare parameter types on method/function definitions
- Use Groovy collection methods (`collect`, `each`, `find`) instead of Java streams
- Use Groovy regex `~/pattern/` instead of `Pattern.compile()`
- Use Groovy truth for null/empty checks: `if (str)`, `if (list)`, `if (map)`
- Use property access instead of getters/setters: `person.name` not `person.getName()`
- Use `?:` (Elvis operator) and `?.` (safe navigation) instead of verbose null checks
- Use `==` instead of `.equals()`
- Use GStrings (`"""..."""`) for multi-line/interpolated strings, not `+` concatenation
- Use operator overloads: `<=>` for compareTo, `<<` for left shift/append
- Prefer Groovy built-in methods: `'abc' * 100`, `'abc'.padLeft(16, ' ')`
