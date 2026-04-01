# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/0),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-04-01

### Added
- New GitHub Actions workflow: `generate-docs.yml` for automated documentation generation
- GitHub Pages deployment with interactive documentation
- Mermaid diagrams: request-flow.mmd and gateway-status.mmd
- Wiki updated as documentation portal
- SonarCloud analysis integration in CI pipeline

### Changed
- Updated GitHub Actions workflow `maven.yml` with SonarCloud analysis and Docker build
- Updated Dockerfile to multi-stage build with JDK 25 and security (non-root user)
- Changed default database server from 10.147.20.25 to 192.168.201.132

### Removed
- Removed livereload configuration from application.yml

---

## [1.0.0] - 2026-04-01

### Changed
- Upgrade Spring Boot from 3.3.1 to 4.0.4
- Upgrade Java from 21 to 25
- Upgrade MySQL Connector from 9.0.0 to 9.6.0
- Upgrade SpringDoc OpenAPI from 2.6.0 to 3.0.2
- Remove maven-enforcer-plugin
- Replace logback with Log4j2
- Add maven-compiler-plugin custom configuration

### Added
- Multi-stage Dockerfile for optimized image builds
- Security improvements (non-root user in Docker)
- curl installed in final Docker image

### Fixed
- Docker build configuration now compiles the project inside container
- Final JAR renamed to sauce.colegio.core-service.jar

### Removed
- livereload configuration from application.yml
- Docker executable configuration

---

## [0.0.1-SNAPSHOT] - Initial Release
- Initial project setup with Spring Boot