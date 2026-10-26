# CLAUDE.md — OdontoGest

Sistema de escritorio para consultorios odontológicos con odontograma interactivo.
Proyecto del curso de Ingeniería de Software, destinado a registro ante INDECOPI como soporte lógico.

**La especificación completa está en `docs/PLAN.md`. Léela entera antes de escribir código.**

## Stack (no cambiar sin consultar)
- Java 21, Maven, Spring Boot 3.3.4 **sin servidor web** (`spring.main.web-application-type=none`).
- JavaFX 21 (FXML + CSS) con AtlantaFX (tema Primer) e Ikonli (Feather).
- SQLite (`sqlite-jdbc`) + Spring Data JPA/Hibernate con `hibernate-community-dialects` (SQLiteDialect).
- Esquema definido a mano en `src/main/resources/db/schema.sql` (`spring.jpa.hibernate.ddl-auto=none`).
- OpenPDF (PDF), Apache POI (Excel), Gson (JSON de APIs), `spring-boot-starter-mail` (correo), `spring-security-crypto` (BCrypt).
- Integración Spring + JavaFX igual que el repo de referencia del docente (`noetipo/crud-persona-spring`):
  `Launcher` → `JavaFxApplication.init()` levanta el contexto Spring → `FXMLLoader.setControllerFactory(context::getBean)`.

## Comandos
- Ejecutar en desarrollo: `mvn javafx:run` (o `mvn spring-boot:run`).
- Pruebas: `mvn test` (incluye JaCoCo: reporte en `target/site/jacoco/index.html`; falla si `dominio` < 70 % de instrucciones)
- JAR ejecutable: `mvn clean package` → `target/odontogest.jar`
- Distribución Windows sin Java instalado: `mvn clean package -Pempaquetar` (ejecutar en Windows) → `target/dist/OdontoGest/`

## Reglas de trabajo para Claude Code
1. Implementa **una fase a la vez** según la sección 7 de `docs/PLAN.md`. Al terminar cada fase: `mvn test` en verde, resumen de lo hecho y **detente a esperar confirmación** antes de pasar a la siguiente.
2. No cambies el esquema de BD, nombres de paquetes ni dependencias del plan sin explicar el motivo y pedir aprobación.
3. Si algo del plan es técnicamente inviable, dilo y propone la alternativa; no improvises en silencio.

## Convenciones de código (obligatorias)
- Nombres de clases, métodos y variables **en español**, como el repo del docente (`PacienteServiceASEJ`, `registrarHallazgoASEJ`; ver el sufijo `ASEJ` más abajo).
- Capas: `ui` → `servicio` → `persistencia`. Los controladores **nunca** usan repositorios ni entidades JPA: reciben y envían `record` DTO de `servicio.dto`. Hay un test ArchUnit que lo verifica.
- El paquete `dominio` es Java puro: **prohibido** importar `javafx.*`, `org.springframework.*`, `jakarta.persistence.*`. Ahí viven los algoritmos (odontograma, índices, cronogramas, agenda) y sus tests unitarios.
- **Nada de BD, archivos ni red en el JavaFX Application Thread.** Todo pasa por `TareaUi.ejecutar(...)`. La UI se actualiza solo en los callbacks de éxito/error.
- Servicios con `@Transactional(readOnly = true)` a nivel de clase y `@Transactional` en métodos que escriben (patrón del docente).
- Dinero: `long` en céntimos en BD (`*_cent`) y `BigDecimal` en Java mediante `DineroConverter`. Nunca `double`.
- Fechas: `TEXT` ISO-8601 en BD (`yyyy-MM-dd`, `yyyy-MM-dd'T'HH:mm:ss`) mediante `AttributeConverter` con `autoApply = true`.
- Errores de negocio: lanzar `NegocioException` con mensaje para el usuario; `TareaUi` lo muestra con `Notificador.advertencia`.
- Los hallazgos del odontograma son **inmutables** una vez cerrado el odontograma; las correcciones se hacen con anulación (tabla `hallazgo_anulacion`). No escribir métodos `update` para `hallazgo`.
- **Sin comentarios en el código fuente** (ni Javadoc ni cabeceras de autoría; tampoco en FXML, CSS, SQL, propiedades ni `pom.xml`). Reemplaza la regla anterior de cabeceras; decisión de los autores para el registro ante INDECOPI. No agregar comentarios nuevos.
- **Sufijo `ASEJ`** (iniciales de los autores) en todo identificador que se declare en el proyecto, pruebas incluidas: clases, interfaces, enums, records, métodos, campos, parámetros y variables locales (`PacienteServiceASEJ`, `registrarHallazgoASEJ`, `pacienteIdASEJ`). Las pruebas terminan en `TestASEJ` (el `pom.xml` configura Surefire con `**/*TestASEJ.java`). Los archivos `.java` se llaman igual que su clase.
  - Excepciones obligadas, que **no** llevan sufijo: constantes de enum (se guardan por nombre en la base), `main` e `initialize()` de los controladores FXML, métodos que sobrescriben o implementan librerías (`run`, `start`, `toString`, `updateItem`, `convertToDatabaseColumn`…), `log` de Lombok (es `logASEJ` por `lombok.config`) y los parámetros genéricos `<T>`.
  - Spring Data: en los métodos derivados el sufijo va después de cada propiedad (`findByPacienteASEJIdASEJOrderByFechaASEJDesc`). Los atributos JPA terminan en `ASEJ`; `EstrategiaNombresASEJ` lo quita para que las columnas de la base de datos no cambien. En `@Query`, los nombres de entidad, atributos y parámetros llevan el sufijo; las clases completas dentro del JPQL también.
  - FXML: `fx:id`, `fx:controller`, `onAction="#…"` y los atributos de componentes propios (`tituloASEJ`) deben coincidir con los nombres con sufijo.

## Datos de la aplicación
- Carpeta de datos: `${user.home}/OdontoGest/` (sobrescribible con `-Dodontogest.home=...`). Ahí van `odontogest.db`, `adjuntos/`, `respaldos/`, `logs/`, `clave.key`.
- Nunca escribir en la carpeta de instalación.
- Usuarios demo (seeder): `admin / Admin123*`, `odontologo / Odonto123*`, `recepcion / Recep123*`.
