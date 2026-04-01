# sauce.colegio.core-service

Demo project for Spring Boot - Core Service

## Información del Proyecto

- **Versión:** 1.1.0
- **Java Version:** 25
- **Spring Boot Version:** 4.0.4

## Tecnologías

- Spring Boot 4.0.4
- Spring Data JPA
- Spring HATEOAS
- Spring Web
- Spring Validation
- MySQL Connector 9.6.0
- Lombok
- SpringDoc OpenAPI 3.0.2
- Log4j2

## Configuración

El servicio está configurado para ejecutarse en el puerto `8081` y conectarse a una base de datos MySQL.

### Variables de Entorno

| Variable | Descripción | Valor por defecto |
|----------|-------------|-------------------|
| `SERVER` | Servidor de base de datos | 192.168.201.132:3306 |
| `DATABASE` | Nombre de la base de datos | escuela |
| `USER` | Usuario de base de datos | root |
| `PASSWORD` | Contraseña de base de datos | root |

## Docker

El proyecto incluye un Dockerfile multi-stage para construir una imagen Docker optimizada.

```bash
docker build -t sauce.colegio.core-service:1.1.0 .
docker run -p 8081:8081 sauce.colegio.core-service:1.1.0
```

## API Documentation

SpringDoc OpenAPI está integrado. Accede a la documentación en: `http://localhost:8081/swagger-ui.html`

## Build

```bash
mvn clean package
```

## Changelog

Ver [CHANGELOG.md](./CHANGELOG.md)