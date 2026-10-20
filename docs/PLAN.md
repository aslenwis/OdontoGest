# OdontoGest — Plan técnico de arquitectura y desarrollo

> Documento de especificación para implementar con Claude Code.
> Producto: **OdontoGest**, sistema de escritorio para consultorios odontológicos con odontograma interactivo.
> Contexto: proyecto del curso de Ingeniería de Software; se registrará ante INDECOPI como soporte lógico, por lo que **debe ejecutarse localmente en la PC del evaluador sin instalar nada adicional**.

## 0. Contexto y decisiones de diseño

### 0.1 Base tomada del repositorio del docente (`noetipo/crud-persona-spring`)
Se mantiene:
- Spring Boot 3.3.4 sin web + JavaFX 21; `Launcher` (clase sin `extends Application`) → `JavaFxApplication` que crea el `ConfigurableApplicationContext` en `init()` y usa `loader.setControllerFactory(context::getBean)`.
- Spring Data JPA + `hibernate-community-dialects` (`SQLiteDialect`) + `sqlite-jdbc`, pool Hikari de **1 conexión**.
- AtlantaFX (`PrimerLight`/`PrimerDark`), Ikonli Feather, OpenPDF, Apache POI, Lombok, Bean Validation.
- Patrones de UI: `MainController` con menú lateral y caché de vistas, interfaz `Refrescable`, `Notificador` (toasts, diálogo de confirmación, panel lateral con `ModalPane`), `Paginador`, `Autocompletar`, `Exportador` con `HostServices`.
- Seeders idempotentes orquestados por `DatabaseSeeder implements ApplicationRunner` con `@ConditionalOnProperty(app.seeder.enabled)`.
- Servicios con `@Transactional(readOnly = true)` de clase y `NegocioException` para reglas de negocio.

### 0.2 Cambios respecto al repo del docente (y por qué)
| Repo docente | OdontoGest | Motivo |
|---|---|---|
| `ddl-auto=update` | `ddl-auto=none` + `db/schema.sql` | Se exige un DDL explícito; Hibernate no genera `CHECK`, índices parciales ni reglas de SQLite. Esquema reproducible para INDECOPI. |
| `jdbc:sqlite:persona.db` (ruta relativa) | `${odontogest.home}/odontogest.db` | Al hacer doble clic en el `.exe` la carpeta de trabajo varía y la carpeta de instalación puede ser de solo lectura. |
| `BigDecimal` con `precision/scale` | Céntimos `INTEGER` + `DineroConverter` | SQLite no tiene `DECIMAL`; con afinidad `NUMERIC` puede guardar `REAL` y perder centavos. |
| Entidades usadas por controladores | DTO `record` hacia la UI | Evita `LazyInitializationException` al acceder a relaciones fuera de la transacción desde el hilo de JavaFX. |
| Solo fat JAR | Fat JAR + `jpackage` (app-image con JRE embebido) | El evaluador puede no tener Java 21 instalado. |
| — | Paquete `dominio` puro + ArchUnit | El núcleo original (odontograma, índices) queda aislado, probado y defendible en el registro. |

### 0.3 Marco normativo que guía el dominio
- **NTS N° 188-MINSA/DGIESP-2022, Norma Técnica de Salud para el Uso del Odontograma** (RM N° 559-2022/MINSA), que deroga la NTS N° 150-MINSA/2019. Reglas que el software implementa:
  - Numeración de piezas con sistema dígito dos (FDI).
  - Color **azul** = hallazgo en buen estado / no patológico; **rojo** = mal estado, temporal o patológico.
  - Los hallazgos registrados **no se alteran** (sin enmendaduras): en OdontoGest los hallazgos son inmutables tras el cierre y se corrigen por anulación trazable.
  - En el odontograma solo se registran **hallazgos observados**, no procedimientos planificados: por eso el plan de tratamiento es un módulo separado.
- **Ley N° 29733** (protección de datos personales): consentimiento del paciente; los datos de salud son sensibles.
- ⚠️ **Tarea del estudiante:** el catálogo de hallazgos de la §3.4 es representativo. Antes de la Fase 3, contrastar símbolos, siglas y colores con el Anexo de la NTS 188 publicado por el MINSA y ajustar las filas del seeder. El código no debe tener colores ni símbolos “quemados”: todo sale del catálogo `hallazgo_tipo`.

### 0.4 Elementos originales del software (argumento para INDECOPI)
1. **Odontograma basado en eventos**: el estado de la boca es una proyección (fold) de hallazgos inmutables con capas de exclusión; permite reconstruir el odontograma en cualquier fecha y comparar inicial vs. actual.
2. **Detección geométrica de superficie dental** por clic, con orientación mesial/distal y vestibular/lingual dependiente del cuadrante FDI.
3. **Cadena de integridad SHA-256** entre odontogramas cerrados: detecta manipulación posterior de la BD (valor médico-legal).
4. **Cálculo automático de índices epidemiológicos CPO-D y ceo-d** desde el odontograma, con clasificación de severidad OMS.
5. **Motor de sugerencia de tratamiento**: reglas configurables hallazgo → procedimiento, por número de superficies y tipo radicular de la pieza; presupuesto y cronograma de cuotas con reparto exacto de céntimos y aplicación de pagos FIFO.
6. **Búsqueda del siguiente hueco libre** en la agenda por barrido de intervalos.

---

## 1. Especificación de Requisitos (ERS resumido)

Prioridad: **M** = obligatorio (MVP), **S** = deseable, **C** = si sobra tiempo.

### 1.1 Requisitos funcionales

#### SEG — Seguridad y usuarios
| ID | Requisito | P |
|---|---|---|
| RF-SEG-01 | Iniciar sesión con usuario y contraseña (BCrypt). Bloqueo de 5 min tras 5 intentos fallidos consecutivos. | M |
| RF-SEG-02 | Roles `ADMIN`, `ODONTOLOGO`, `RECEPCION`; el menú y las acciones se habilitan según la matriz de permisos (§5.6). | M |
| RF-SEG-03 | CRUD de usuarios (solo ADMIN); un usuario `ODONTOLOGO` se vincula a un registro de `odontologo`. | M |
| RF-SEG-04 | Cambiar la propia contraseña; cerrar sesión. | M |
| RF-SEG-05 | Registrar en `auditoria` inicio de sesión, creación/anulación de hallazgos, cierre de odontograma, pagos y anulaciones, cambios de configuración. | S |

#### PAC — Pacientes e historia clínica
| ID | Requisito | P |
|---|---|---|
| RF-PAC-01 | CRUD de pacientes con tipo y número de documento únicos; número de historia clínica correlativo `HC-000001`. | M |
| RF-PAC-02 | Búsqueda paginada por nombre, documento o N° de historia. | M |
| RF-PAC-03 | Registrar apoderado cuando el paciente es menor de 18 años (validación por fecha de nacimiento). | M |
| RF-PAC-04 | Anamnesis: antecedentes, alergias, medicación, enfermedades sistémicas (checklist), hábitos, presión arterial. Las alergias se muestran como alerta roja en la cabecera de la ficha. | M |
| RF-PAC-05 | Consentimiento de tratamiento de datos personales y consentimiento informado de tratamiento, imprimibles en PDF. | M |
| RF-PAC-06 | Autocompletar nombres consultando DNI en una API externa configurable (opcional; si falla, ingreso manual). | S |
| RF-PAC-07 | Adjuntar radiografías, fotos y documentos al paciente (copiados a la carpeta de datos, con SHA-256). | S |

#### ATE — Atenciones (evolución clínica)
| ID | Requisito | P |
|---|---|---|
| RF-ATE-01 | Abrir una atención (desde una cita o directa) con motivo de consulta, examen clínico, diagnóstico y observaciones. | M |
| RF-ATE-02 | Marcar en la atención los ítems del plan de tratamiento realizados. | M |
| RF-ATE-03 | Cerrar la atención; una atención cerrada es de solo lectura. | M |
| RF-ATE-04 | Ver la línea de tiempo de atenciones del paciente. | M |

#### ODO — Odontograma
| ID | Requisito | P |
|---|---|---|
| RF-ODO-01 | Mostrar odontograma de dentición permanente (11–48), decidua (51–85) o mixta, numeración FDI. | M |
| RF-ODO-02 | Registrar hallazgos por **superficie** (O/I, M, D, V, L/P) con clic sobre la superficie, por **pieza** o por **rango/entre piezas**. | M |
| RF-ODO-03 | Color de cada hallazgo según la regla del catálogo (azul/rojo, o según estado bueno/malo elegido). | M |
| RF-ODO-04 | Odontograma **inicial** único por paciente; odontogramas de **evolución** por atención. | M |
| RF-ODO-05 | Cerrar (firmar) un odontograma: queda inmutable y se calcula su hash de integridad encadenado. | M |
| RF-ODO-06 | Anular un hallazgo de un odontograma cerrado con motivo obligatorio; el hallazgo sigue visible en el historial como anulado. | M |
| RF-ODO-07 | Ver el estado de la boca en cualquier fecha (línea de tiempo) y comparar contra el inicial resaltando cambios. | M |
| RF-ODO-08 | Calcular y mostrar índices CPO-D y ceo-d con nivel de severidad OMS. | M |
| RF-ODO-09 | Campo de especificaciones y observaciones del odontograma. | M |
| RF-ODO-10 | Exportar la ficha odontológica (datos + imagen del odontograma + hallazgos + especificaciones + firma) a PDF. | M |
| RF-ODO-11 | Verificar la integridad de la cadena de odontogramas del paciente. | S |

#### PLN — Plan de tratamiento y presupuesto
| ID | Requisito | P |
|---|---|---|
| RF-PLN-01 | Generar automáticamente ítems sugeridos a partir de los hallazgos vigentes (motor de reglas). | M |
| RF-PLN-02 | Editar el plan: agregar/quitar ítems, cambiar procedimiento, precio, descuento y fase (1 urgente, 2 operatoria, 3 rehabilitación, 4 mantenimiento). | M |
| RF-PLN-03 | Totales: subtotal, descuento por ítem y global, total. | M |
| RF-PLN-04 | Estados del plan: BORRADOR → PROPUESTO → ACEPTADO → EN_CURSO → FINALIZADO / ANULADO. | M |
| RF-PLN-05 | Generar cronograma de cuotas (cuota inicial, N cuotas, frecuencia, fecha de primera cuota). | M |
| RF-PLN-06 | Exportar presupuesto en PDF. | M |

#### CAJ — Caja y pagos
| ID | Requisito | P |
|---|---|---|
| RF-CAJ-01 | Registrar pago (efectivo, tarjeta, Yape, Plin, transferencia) asignado automáticamente a cuotas pendientes en orden FIFO, con vista previa. | M |
| RF-CAJ-02 | Recibo interno en PDF con monto en letras y correlativo `R-00000001`. | M |
| RF-CAJ-03 | Anular pago con motivo (revierte aplicaciones). | M |
| RF-CAJ-04 | Estado de cuenta del paciente: cuotas, pagado, saldo, vencidas. | M |
| RF-CAJ-05 | Resumen de caja del día por medio de pago. | S |

#### AGE — Agenda y recordatorios
| ID | Requisito | P |
|---|---|---|
| RF-AGE-01 | Agenda semanal por odontólogo con bloques de 15 min dentro de su horario de atención. | M |
| RF-AGE-02 | Crear, mover, cancelar citas; estados PROGRAMADA, CONFIRMADA, ATENDIDA, CANCELADA, NO_ASISTIO. | M |
| RF-AGE-03 | Impedir solapamiento de citas del mismo odontólogo y del mismo paciente. | M |
| RF-AGE-04 | Sugerir el siguiente hueco libre para una duración dada. | M |
| RF-AGE-05 | Desde una cita, abrir la atención del paciente. | M |
| RF-AGE-06 | Recordatorio por correo N horas antes (si hay internet y SMTP configurado); registro del envío. | S |

#### CAT — Catálogos
| ID | Requisito | P |
|---|---|---|
| RF-CAT-01 | Tarifario de procedimientos (código, nombre, categoría, precio, forma de cobro, duración). | M |
| RF-CAT-02 | Catálogo de hallazgos (sigla, representación, símbolo, regla de color, grupo de exclusión, conteo CPO-D/ceo-d). | M |
| RF-CAT-03 | Reglas de sugerencia hallazgo → procedimiento. | M |
| RF-CAT-04 | Odontólogos (COP, especialidad, color en agenda) y horarios de atención. | M |

#### REP — Reportes y dashboard
| ID | Requisito | P |
|---|---|---|
| RF-REP-01 | Dashboard: citas de hoy, ingresos del mes, pacientes nuevos del mes, cuotas vencidas, ingresos de los últimos 6 meses. | M |
| RF-REP-02 | Reporte de ingresos por periodo y medio de pago (Excel). | M |
| RF-REP-03 | Producción por odontólogo (procedimientos realizados y monto). | S |
| RF-REP-04 | Morosidad: cuotas vencidas por paciente (Excel). | S |
| RF-REP-05 | Reporte epidemiológico: CPO-D/ceo-d promedio por grupo etario. | S |

#### CFG — Configuración y respaldo
| ID | Requisito | P |
|---|---|---|
| RF-CFG-01 | Datos de la clínica (nombre, RUC, dirección, teléfono, logo) usados en PDFs. | M |
| RF-CFG-02 | Parámetros de agenda y recordatorios. | M |
| RF-CFG-03 | Credenciales de integraciones (token API DNI, SMTP) guardadas cifradas en la tabla `configuracion`, con botón “Probar conexión”. | M |
| RF-CFG-04 | Crear respaldo de la BD (+ adjuntos) en ZIP; restaurar respaldo (se aplica al reiniciar). | M |
| RF-CFG-05 | Pantalla “Acerca de” con versión, autores y aviso de registro. | M |

### 1.2 Requisitos no funcionales

| ID | Categoría | Requisito |
|---|---|---|
| RNF-01 | Portabilidad (INDECOPI) | Se distribuye como **app-image de Windows x64** con JRE embebido (`jpackage`): se descomprime y se ejecuta `OdontoGest.exe` sin instalar Java ni motor de BD. También corre con `java -jar odontogest.jar` en un equipo con Java 21. |
| RNF-02 | Portabilidad | Al primer arranque crea `${user.home}/OdontoGest/` con la BD, aplica `schema.sql` y carga catálogos y datos demo. Ruta sobrescribible con `-Dodontogest.home`. |
| RNF-03 | Funcionamiento local | Todas las funciones M operan **sin internet**. Las integraciones (DNI, correo) degradan con un aviso no bloqueante. |
| RNF-04 | Rendimiento | Arranque ≤ 8 s en PC de 8 GB RAM; listados paginados ≤ 300 ms con 5 000 pacientes; redibujado del odontograma ≤ 16 ms (hover fluido). |
| RNF-05 | Concurrencia JavaFX | Ninguna operación de BD, E/S o red en el *JavaFX Application Thread*. Se usa `javafx.concurrent.Task` sobre un `ExecutorService` de hilos *daemon* (`TareaUi`). La UI muestra indicador de carga y deshabilita el control que dispara la tarea. |
| RNF-06 | Concurrencia BD | Hikari con `maximum-pool-size=1`, `journal_mode=WAL`, `busy_timeout=5000`, `foreign_keys=true`. Transacciones cortas en la capa de servicio. |
| RNF-07 | Integridad | FK activas, `CHECK` en dominios, índices únicos (incluido índice parcial para odontograma inicial). Hallazgos inmutables tras cierre. Cadena SHA-256. |
| RNF-08 | Seguridad | Contraseñas BCrypt (fuerza 10). Secretos AES-256-GCM con clave local `clave.key`. Control de permisos en servicio (no solo ocultando botones). |
| RNF-09 | Usabilidad | Interfaz en español; resolución mínima 1280×720; tema claro/oscuro; atajos (`Ctrl+N` nuevo, `Ctrl+F` buscar, `Esc` cancelar, `Enter` registrar hallazgo). |
| RNF-10 | Mantenibilidad | Arquitectura por capas verificada con ArchUnit; cobertura ≥ 70 % en `dominio`; sin lógica de negocio en controladores. |
| RNF-11 | Trazabilidad | Log rotativo en `${odontogest.home}/logs/odontogest.log`; tabla `auditoria`. |
| RNF-12 | Normativo | Odontograma alineado a NTS 188 (FDI, azul/rojo, sin enmiendas, separación hallazgo/plan). Consentimiento de datos personales (Ley 29733). |
| RNF-13 | Registro INDECOPI | Versión visible en “Acerca de” (desde `build-info`), cabecera de autoría en fuentes, README con credenciales demo, manual de usuario. |

---

## 2. Arquitectura y estructura de paquetes

### 2.1 Estilo arquitectónico
**Arquitectura en capas + MVC de JavaFX, con ViewModel para las pantallas complejas y patrón Repository/DAO.**

```
┌──────────────────────────────────────────────────────────────┐
│ ui (JavaFX)                                                  │
│  FXML (vista) ── Controlador (@Component) ── ViewModel*      │
│  *solo odontograma y agenda                                  │
└───────────────▲──────────────────────────┬───────────────────┘
       DTO record│                          │ llamadas vía TareaUi (hilo de fondo)
┌───────────────┴──────────────────────────▼───────────────────┐
│ servicio (@Service, @Transactional)  ── documento (PDF/XLSX) │
│                                       ── integracion (HTTP,  │
│                                          SMTP, cifrado)      │
└───────────────▲──────────────────────────┬───────────────────┘
                │ usa algoritmos puros      │ entidades JPA
┌───────────────┴─────────┐   ┌────────────▼───────────────────┐
│ dominio (Java puro)     │   │ persistencia                   │
│ odontograma, finanzas,  │   │  repositorio (Spring Data JPA) │
│ agenda, comun           │   │  dao (JdbcTemplate, reportes)  │
└─────────────────────────┘   │  conversor (AttributeConverter)│
                              └────────────┬───────────────────┘
                                           ▼
                                SQLite (odontogest.db)
```

Reglas de dependencia (verificadas en `ArquitecturaTest` con ArchUnit):
- `ui..` no depende de `persistencia..` ni de `modelo..` (entidades).
- `dominio..` no depende de `javafx..`, `org.springframework..`, `jakarta.persistence..` ni de ningún otro paquete del proyecto salvo `dominio..`.
- `persistencia..` no depende de `ui..` ni de `servicio..`.
- `servicio..` no depende de `ui..`.

**Repository vs DAO:** las operaciones CRUD y consultas por entidad usan interfaces Spring Data (`JpaRepository`, patrón Repository). Las consultas de agregación para reportes y dashboard usan clases DAO con `JdbcTemplate` y SQL nativo, que devuelven `record` directamente.

**Comunicación entre controladores:** eventos de Spring (`ApplicationEventPublisher` + `@EventListener`) y beans de estado compartido (`SesionUsuario`, `PacienteActivo`). Ver §6.3.

### 2.2 Árbol de directorios

```
odontogest/
├── pom.xml
├── CLAUDE.md
├── README.md                         # instalación, ejecución, credenciales demo
├── docs/
│   ├── PLAN.md                       # este documento
│   ├── manual-usuario.md             # se redacta en la Fase 9
│   └── indecopi/                     # descripción del software, capturas
├── packaging/
│   ├── odontogest.ico                # icono para jpackage (Windows)
│   └── odontogest.png
└── src/
    ├── main/
    │   ├── java/com/odontogest/
    │   │   ├── Launcher.java
    │   │   ├── OdontoGestApplication.java          # @SpringBootApplication
    │   │   ├── JavaFxApplication.java
    │   │   │
    │   │   ├── config/
    │   │   │   ├── DirectorioDatos.java            # resuelve/crea ${odontogest.home} (static, antes de Spring)
    │   │   │   ├── RestauracionPendiente.java      # aplica respaldo pendiente antes de abrir la BD
    │   │   │   ├── EjecutorConfig.java             # @Bean ExecutorService "ejecutorUi", ScheduledExecutorService
    │   │   │   ├── ClinicaProperties.java          # @ConfigurationProperties("app")
    │   │   │   └── SeguridadConfig.java            # @Bean PasswordEncoder (BCrypt)
    │   │   │
    │   │   ├── dominio/                            # JAVA PURO — sin Spring/JavaFX/JPA
    │   │   │   ├── odontograma/
    │   │   │   │   ├── PiezaDental.java            # record PiezaDental(int fdi) + validación y metadatos
    │   │   │   │   ├── Cuadrante.java              # enum 1..8
    │   │   │   │   ├── Arcada.java                 # enum SUPERIOR, INFERIOR
    │   │   │   │   ├── TipoPieza.java              # enum INCISIVO, CANINO, PREMOLAR, MOLAR
    │   │   │   │   ├── TipoRadicular.java          # enum UNIRRADICULAR, BIRRADICULAR, MULTIRRADICULAR
    │   │   │   │   ├── Denticion.java              # enum PERMANENTE, DECIDUA, MIXTA
    │   │   │   │   ├── Superficie.java             # enum O, M, D, V, L con bit
    │   │   │   │   ├── Superficies.java            # utilidades bitmask <-> EnumSet
    │   │   │   │   ├── Representacion.java         # enum SUPERFICIE, PIEZA, ENTRE_PIEZAS, RANGO_PIEZAS
    │   │   │   │   ├── ReglaColor.java             # enum POR_ESTADO, SIEMPRE_ROJO, SIEMPRE_AZUL
    │   │   │   │   ├── ColorHallazgo.java          # enum AZUL, ROJO
    │   │   │   │   ├── EstadoHallazgo.java         # enum BUENO, MALO
    │   │   │   │   ├── ConteoIndice.java           # enum C, P, O, E, NINGUNO
    │   │   │   │   ├── TipoHallazgoDef.java        # record con definición del catálogo
    │   │   │   │   ├── HallazgoEvento.java         # record inmutable (un hallazgo registrado)
    │   │   │   │   ├── ClaveCapa.java              # record (piezaInicio, piezaFin, superficie, capa)
    │   │   │   │   ├── ProyectorOdontograma.java   # fold de eventos -> EstadoOdontograma
    │   │   │   │   ├── EstadoOdontograma.java      # estado proyectado inmutable
    │   │   │   │   ├── EstadoPieza.java            # hallazgos vigentes de una pieza
    │   │   │   │   ├── DiferenciaOdontograma.java  # comparación entre dos estados
    │   │   │   │   ├── CalculadoraIndices.java     # CPO-D y ceo-d
    │   │   │   │   ├── ResultadoIndice.java        # record (c, p, o, total, examinadas, nivel)
    │   │   │   │   ├── NivelSeveridadOms.java      # enum MUY_BAJO..MUY_ALTO con umbrales
    │   │   │   │   └── CadenaIntegridad.java       # hash SHA-256 encadenado
    │   │   │   ├── tratamiento/
    │   │   │   │   ├── ReglaSugerenciaDef.java
    │   │   │   │   ├── ItemSugerido.java
    │   │   │   │   └── MotorSugerencias.java
    │   │   │   ├── finanzas/
    │   │   │   │   ├── Frecuencia.java             # enum SEMANAL, QUINCENAL, MENSUAL
    │   │   │   │   ├── CuotaProgramada.java        # record
    │   │   │   │   ├── GeneradorCronograma.java
    │   │   │   │   ├── SaldoCuota.java             # record (cuotaId, vencimiento, numero, saldoCent)
    │   │   │   │   ├── Aplicacion.java             # record (cuotaId, montoCent)
    │   │   │   │   ├── AsignadorPagosFifo.java
    │   │   │   │   └── CalculadoraTotales.java     # subtotal/descuentos/total en céntimos
    │   │   │   ├── agenda/
    │   │   │   │   ├── Intervalo.java              # record (LocalDateTime inicio, fin) con solapa()
    │   │   │   │   ├── BloqueHorario.java          # record (DayOfWeek, LocalTime, LocalTime)
    │   │   │   │   └── BuscadorHuecos.java
    │   │   │   └── comun/
    │   │   │       ├── NumeroALetras.java          # "CIENTO VEINTE CON 50/100 SOLES"
    │   │   │       └── Validaciones.java           # DNI, CE, email, teléfono
    │   │   │
    │   │   ├── modelo/                             # entidades JPA + enums persistidos
    │   │   │   ├── Configuracion.java  Correlativo.java  Usuario.java  Rol.java
    │   │   │   ├── Odontologo.java  HorarioAtencion.java
    │   │   │   ├── Paciente.java  TipoDocumento.java  Sexo.java  Anamnesis.java
    │   │   │   ├── Consentimiento.java  TipoConsentimiento.java  Adjunto.java  TipoAdjunto.java
    │   │   │   ├── Atencion.java  EstadoAtencion.java
    │   │   │   ├── Odontograma.java  TipoOdontograma.java  EstadoOdontogramaRegistro.java
    │   │   │   ├── HallazgoTipo.java  Hallazgo.java  HallazgoAnulacion.java
    │   │   │   ├── Procedimiento.java  CategoriaProcedimiento.java  FormaCobro.java  ReglaSugerencia.java
    │   │   │   ├── PlanTratamiento.java  EstadoPlan.java  PlanItem.java  EstadoPlanItem.java
    │   │   │   ├── Cuota.java  Pago.java  MedioPago.java  PagoAplicacion.java
    │   │   │   ├── Cita.java  EstadoCita.java
    │   │   │   └── NotificacionLog.java  Auditoria.java
    │   │   │
    │   │   ├── persistencia/
    │   │   │   ├── repositorio/                    # una interfaz JpaRepository por entidad raíz
    │   │   │   │   ├── PacienteRepository.java  OdontogramaRepository.java  HallazgoRepository.java
    │   │   │   │   ├── HallazgoTipoRepository.java  CitaRepository.java  PagoRepository.java
    │   │   │   │   └── ... (resto de entidades)
    │   │   │   ├── dao/
    │   │   │   │   ├── DashboardDao.java  ReporteDao.java  CorrelativoDao.java
    │   │   │   └── conversor/
    │   │   │       ├── DineroConverter.java        # BigDecimal <-> Long céntimos
    │   │   │       ├── LocalDateConverter.java     # autoApply, "yyyy-MM-dd"
    │   │   │       ├── LocalDateTimeConverter.java # autoApply, "yyyy-MM-dd'T'HH:mm:ss"
    │   │   │       ├── LocalTimeConverter.java     # autoApply, "HH:mm"
    │   │   │       └── SuperficiesConverter.java   # Set<Superficie> <-> int
    │   │   │
    │   │   ├── servicio/
    │   │   │   ├── AutenticacionService.java  UsuarioService.java
    │   │   │   ├── PacienteService.java  AnamnesisService.java  ConsentimientoService.java  AdjuntoService.java
    │   │   │   ├── AtencionService.java
    │   │   │   ├── OdontogramaService.java  CatalogoHallazgoService.java  IntegridadService.java
    │   │   │   ├── PlanTratamientoService.java  ProcedimientoService.java
    │   │   │   ├── PagoService.java
    │   │   │   ├── CitaService.java  OdontologoService.java
    │   │   │   ├── DashboardService.java  ReporteService.java
    │   │   │   ├── ConfiguracionService.java  RespaldoService.java  AuditoriaService.java
    │   │   │   ├── CorrelativoService.java
    │   │   │   └── dto/                            # records de entrada/salida para la UI
    │   │   │       ├── PacienteResumen.java  PacienteDetalle.java  PacienteForm.java
    │   │   │       ├── OdontogramaVista.java  HallazgoDto.java  NuevoHallazgo.java  HallazgoTipoDto.java
    │   │   │       ├── PlanDto.java  PlanItemDto.java  CronogramaForm.java  CuotaDto.java
    │   │   │       ├── PagoForm.java  PagoDto.java  EstadoCuentaDto.java
    │   │   │       ├── CitaDto.java  CitaForm.java  AgendaSemanaDto.java
    │   │   │       └── ...
    │   │   │
    │   │   ├── documento/
    │   │   │   ├── PlantillaPdf.java               # cabecera de clínica, pie, estilos OpenPDF
    │   │   │   ├── FichaOdontologicaPdf.java  PresupuestoPdf.java  ReciboPdf.java  ConsentimientoPdf.java
    │   │   │   └── ReporteExcel.java               # utilidades POI (estilos, autoajuste)
    │   │   │
    │   │   ├── integracion/
    │   │   │   ├── seguridad/CifradorSecretos.java
    │   │   │   ├── dni/ConsultaDniGateway.java  HttpConsultaDniGateway.java  DatosPersona.java
    │   │   │   ├── correo/CorreoGateway.java  SmtpCorreoGateway.java
    │   │   │   ├── RecordatorioScheduler.java
    │   │   │   └── ConectividadService.java        # ¿hay internet? (HEAD con timeout 2 s)
    │   │   │
    │   │   ├── seguridad/
    │   │   │   ├── SesionUsuario.java              # usuario actual (bean singleton)
    │   │   │   └── Permiso.java                    # enum con matriz rol -> permisos
    │   │   │
    │   │   ├── evento/                             # eventos de aplicación (records)
    │   │   │   ├── NavegarA.java  PacienteAbierto.java  CitaCambiada.java
    │   │   │   ├── PagoRegistrado.java  OdontogramaCerrado.java  SesionIniciada.java
    │   │   │
    │   │   ├── excepcion/
    │   │   │   ├── NegocioException.java  PermisoDenegadoException.java  IntegracionException.java
    │   │   │
    │   │   ├── seeder/
    │   │   │   ├── ConfiguracionSeeder.java        # SIEMPRE (claves por defecto)
    │   │   │   ├── CatalogoSeeder.java             # SIEMPRE (hallazgos, procedimientos, reglas)
    │   │   │   ├── DatabaseSeeder.java             # orquestador demo (condicional)
    │   │   │   ├── UsuarioSeeder.java  OdontologoSeeder.java  PacienteSeeder.java
    │   │   │   ├── ClinicaDemoSeeder.java          # atenciones, odontogramas, planes, pagos, citas
    │   │   │   └── GeneradorDatosDemo.java         # nombres peruanos, Random(2026)
    │   │   │
    │   │   └── ui/
    │   │       ├── controlador/
    │   │       │   ├── LoginController.java  MainController.java  DashboardController.java
    │   │       │   ├── AgendaController.java  CitaFormController.java
    │   │       │   ├── PacientesController.java  PacienteFichaController.java
    │   │       │   ├── DatosPacienteController.java  AnamnesisController.java
    │   │       │   ├── OdontogramaController.java  PlanTratamientoController.java
    │   │       │   ├── AtencionesController.java  PagosPacienteController.java  AdjuntosController.java
    │   │       │   ├── CajaController.java  ReportesController.java  CatalogosController.java
    │   │       │   ├── UsuariosController.java  ConfiguracionController.java  AcercaDeController.java
    │   │       ├── vistamodelo/
    │   │       │   ├── OdontogramaViewModel.java
    │   │       │   └── AgendaViewModel.java
    │   │       ├── componente/
    │   │       │   ├── OdontogramaCanvas.java      # Region con Canvas redimensionable
    │   │       │   ├── LayoutOdontograma.java      # geometría: pieza -> rectángulos
    │   │       │   ├── RenderizadorOdontograma.java
    │   │       │   ├── DetectorSuperficie.java     # hit-testing
    │   │       │   ├── PaletaOdontograma.java      # colores (azul/rojo/hover/diff) por tema
    │   │       │   ├── AgendaSemanal.java          # grilla de citas (Region personalizada)
    │   │       │   ├── TarjetaKpi.java
    │   │       │   ├── Paginador.java  Autocompletar.java
    │   │       └── soporte/
    │   │           ├── TareaUi.java  Notificador.java  Refrescable.java
    │   │           ├── CargadorVistas.java         # FXMLLoader + controllerFactory + caché
    │   │           ├── Formularios.java  Exportador.java  Atajos.java
    │   │
    │   └── resources/
    │       ├── application.properties
    │       ├── db/schema.sql
    │       ├── view/
    │       │   ├── login-view.fxml  main-view.fxml  dashboard-view.fxml
    │       │   ├── agenda-view.fxml  cita-form-view.fxml
    │       │   ├── pacientes-view.fxml  paciente-ficha-view.fxml
    │       │   ├── paciente-datos-tab.fxml  paciente-anamnesis-tab.fxml  paciente-odontograma-tab.fxml
    │       │   ├── paciente-plan-tab.fxml  paciente-atenciones-tab.fxml  paciente-pagos-tab.fxml
    │       │   ├── paciente-adjuntos-tab.fxml
    │       │   ├── caja-view.fxml  reportes-view.fxml  catalogos-view.fxml
    │       │   ├── usuarios-view.fxml  configuracion-view.fxml  acerca-de-view.fxml
    │       ├── css/app.css  css/odontograma.css  css/agenda.css
    │       ├── img/logo.png  img/icono.png
    │       └── plantillas/consentimiento-datos.txt  plantillas/consentimiento-tratamiento.txt
    └── test/java/com/odontogest/
        ├── ArquitecturaTest.java
        ├── dominio/odontograma/ PiezaDentalTest, ProyectorOdontogramaTest, CalculadoraIndicesTest,
        │                        CadenaIntegridadTest, DetectorSuperficieTest*
        ├── dominio/finanzas/ GeneradorCronogramaTest, AsignadorPagosFifoTest
        ├── dominio/agenda/ BuscadorHuecosTest
        ├── dominio/tratamiento/ MotorSugerenciasTest
        ├── persistencia/ ConversoresIntegracionTest      # guarda y lee fechas/dinero en SQLite real
        └── servicio/ OdontogramaServiceIT, PagoServiceIT, CitaServiceIT
```
\* `DetectorSuperficie` vive en `ui.componente` pero su lógica es pura (no usa nodos JavaFX, solo `double`), así que se prueba sin levantar JavaFX.

### 2.3 Arranque de la aplicación

```java
public final class Launcher {
    public static void main(String[] args) {
        Path home = DirectorioDatos.inicializar();          // crea carpetas y fija System property "odontogest.home"
        RestauracionPendiente.aplicarSiExiste(home);         // si hay odontogest.db.restaurar, lo intercambia
        Application.launch(JavaFxApplication.class, args);
    }
}

public class JavaFxApplication extends Application {
    private ConfigurableApplicationContext context;

    @Override public void init() {
        context = new SpringApplicationBuilder(OdontoGestApplication.class)
                .web(WebApplicationType.NONE).headless(false).run();
    }
    @Override public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        context.getBean(Exportador.class).setHostServices(getHostServices());
        context.getBean(CargadorVistas.class).mostrarLogin(stage);   // login → main-view
    }
    @Override public void stop() {
        context.getBean(RespaldoService.class).respaldoAutomaticoAlCerrar();  // si está habilitado
        context.close();
        Platform.exit();
    }
}
```
Mientras Spring arranca, mostrar un `Preloader` simple (opcional, C) o una ventana splash.

### 2.4 `application.properties`

```properties
spring.application.name=odontogest
spring.main.web-application-type=none
spring.main.banner-mode=off

# SQLite en la carpeta de datos del usuario
spring.datasource.url=jdbc:sqlite:${odontogest.home}/odontogest.db?foreign_keys=true&journal_mode=WAL&busy_timeout=5000
spring.datasource.driver-class-name=org.sqlite.JDBC
spring.datasource.hikari.maximum-pool-size=1
spring.datasource.hikari.connection-init-sql=PRAGMA foreign_keys=ON

# Esquema explícito
spring.sql.init.mode=always
spring.sql.init.schema-locations=classpath:db/schema.sql
spring.sql.init.encoding=UTF-8
spring.jpa.hibernate.ddl-auto=none
spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect
spring.jpa.open-in-view=false
spring.jpa.show-sql=false

# Logs
logging.file.name=${odontogest.home}/logs/odontogest.log
logging.logback.rollingpolicy.max-file-size=5MB
logging.logback.rollingpolicy.max-history=10
logging.level.root=WARN
logging.level.com.odontogest=INFO

# Datos de demostración (idempotente). Dejar en true en la versión que se entrega a INDECOPI.
app.seeder.enabled=true
app.seeder.semilla=2026
```
`schema.sql` usa solo `CREATE TABLE IF NOT EXISTS` / `CREATE INDEX IF NOT EXISTS` y **no contiene triggers** (el separador `;` de `spring.sql.init` rompería los bloques `BEGIN…END`).

---

## 3. Diseño de base de datos (SQLite)

### 3.1 Convenciones
- PK: `id INTEGER PRIMARY KEY` (alias de `rowid`; Hibernate `GenerationType.IDENTITY`).
- Fechas: `TEXT` ISO-8601. Fecha `yyyy-MM-dd`; fecha-hora `yyyy-MM-ddTHH:mm:ss`; hora `HH:mm`. Se ordenan y comparan bien como texto.
- Booleanos: `INTEGER` con `CHECK (x IN (0,1))`.
- Dinero: `INTEGER` en céntimos, columnas con sufijo `_cent`.
- Enums: `TEXT` con `CHECK (... IN (...))`, mapeados con `@Enumerated(EnumType.STRING)`.
- Superficies: `INTEGER` bitmask `O=1, M=2, D=4, V=8, L=16` (rango 0–31).

### 3.2 Diagrama entidad-relación (resumen)

```
usuario ─┐                       odontologo ── horario_atencion
         │ (registra)                │
paciente ─┬─ anamnesis (1:1)         │
          ├─ consentimiento (1:N)    │
          ├─ adjunto (1:N)           │
          ├─ cita (1:N) ─────────────┤
          ├─ atencion (1:N) ─────────┘
          │      └─ odontograma (1:N, uno INICIAL por paciente)
          │              └─ hallazgo (1:N) ── hallazgo_tipo (catálogo)
          │                     └─ hallazgo_anulacion (0..1)
          ├─ plan_tratamiento (1:N) ── plan_item (1:N) ── procedimiento (tarifario)
          │         └─ cuota (1:N) ── pago_aplicacion (N) ── pago
          └─ pago (1:N)
hallazgo_tipo ── regla_sugerencia ── procedimiento
configuracion, correlativo, auditoria, notificacion_log (independientes)
```

### 3.3 Script DDL — `src/main/resources/db/schema.sql`

```sql
-- =====================================================================
-- OdontoGest - Esquema SQLite v1
-- Ejecutado en cada arranque por spring.sql.init (idempotente).
-- =====================================================================

-- ------------------------------------------------------------ SISTEMA
CREATE TABLE IF NOT EXISTS configuracion (
    clave           TEXT PRIMARY KEY,
    valor           TEXT,
    tipo            TEXT NOT NULL DEFAULT 'TEXTO'
                    CHECK (tipo IN ('TEXTO','ENTERO','BOOLEANO','SECRETO','JSON')),
    grupo           TEXT NOT NULL,
    descripcion     TEXT,
    editable        INTEGER NOT NULL DEFAULT 1 CHECK (editable IN (0,1)),
    actualizado_en  TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS correlativo (
    serie   TEXT PRIMARY KEY,                      -- 'HC', 'RECIBO'
    ultimo  INTEGER NOT NULL DEFAULT 0 CHECK (ultimo >= 0)
);

-- ------------------------------------------------------------ PERSONAL
CREATE TABLE IF NOT EXISTS odontologo (
    id              INTEGER PRIMARY KEY,
    nombres         TEXT NOT NULL,
    apellidos       TEXT NOT NULL,
    cop             TEXT NOT NULL UNIQUE,          -- N° colegiatura (Colegio Odontológico del Perú)
    rne             TEXT,                          -- registro de especialidad
    especialidad    TEXT NOT NULL DEFAULT 'Odontología general',
    telefono        TEXT,
    email           TEXT,
    color_agenda    TEXT NOT NULL DEFAULT '#2563EB' CHECK (color_agenda GLOB '#[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]'),
    activo          INTEGER NOT NULL DEFAULT 1 CHECK (activo IN (0,1))
);

CREATE TABLE IF NOT EXISTS horario_atencion (
    id              INTEGER PRIMARY KEY,
    odontologo_id   INTEGER NOT NULL REFERENCES odontologo(id) ON DELETE CASCADE,
    dia_semana      INTEGER NOT NULL CHECK (dia_semana BETWEEN 1 AND 7),   -- 1 = lunes (ISO)
    hora_inicio     TEXT NOT NULL,
    hora_fin        TEXT NOT NULL,
    CHECK (hora_fin > hora_inicio)
);
CREATE INDEX IF NOT EXISTS ix_horario_odontologo ON horario_atencion(odontologo_id, dia_semana);

CREATE TABLE IF NOT EXISTS usuario (
    id                  INTEGER PRIMARY KEY,
    username            TEXT NOT NULL UNIQUE COLLATE NOCASE,
    password_hash       TEXT NOT NULL,
    nombre_completo     TEXT NOT NULL,
    rol                 TEXT NOT NULL CHECK (rol IN ('ADMIN','ODONTOLOGO','RECEPCION')),
    odontologo_id       INTEGER REFERENCES odontologo(id) ON DELETE SET NULL,
    activo              INTEGER NOT NULL DEFAULT 1 CHECK (activo IN (0,1)),
    intentos_fallidos   INTEGER NOT NULL DEFAULT 0,
    bloqueado_hasta     TEXT,
    ultimo_acceso       TEXT,
    creado_en           TEXT NOT NULL,
    CHECK (rol <> 'ODONTOLOGO' OR odontologo_id IS NOT NULL)
);

-- ------------------------------------------------------------ PACIENTES
CREATE TABLE IF NOT EXISTS paciente (
    id                          INTEGER PRIMARY KEY,
    numero_historia             TEXT NOT NULL UNIQUE,              -- HC-000001
    tipo_documento              TEXT NOT NULL CHECK (tipo_documento IN ('DNI','CE','PASAPORTE','SIN_DOCUMENTO')),
    numero_documento            TEXT,
    nombres                     TEXT NOT NULL,
    apellido_paterno            TEXT NOT NULL,
    apellido_materno            TEXT,
    fecha_nacimiento            TEXT NOT NULL,
    sexo                        TEXT NOT NULL CHECK (sexo IN ('M','F')),
    telefono                    TEXT,
    email                       TEXT,
    direccion                   TEXT,
    ocupacion                   TEXT,
    contacto_emergencia_nombre  TEXT,
    contacto_emergencia_telefono TEXT,
    apoderado_nombre            TEXT,
    apoderado_documento         TEXT,
    apoderado_parentesco        TEXT,
    acepta_recordatorios        INTEGER NOT NULL DEFAULT 1 CHECK (acepta_recordatorios IN (0,1)),
    activo                      INTEGER NOT NULL DEFAULT 1 CHECK (activo IN (0,1)),
    creado_en                   TEXT NOT NULL,
    actualizado_en              TEXT NOT NULL,
    CHECK (tipo_documento = 'SIN_DOCUMENTO' OR numero_documento IS NOT NULL),
    CHECK (tipo_documento <> 'DNI' OR (length(numero_documento) = 8 AND numero_documento NOT GLOB '*[^0-9]*'))
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_paciente_documento
    ON paciente(tipo_documento, numero_documento) WHERE numero_documento IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_paciente_apellidos ON paciente(apellido_paterno, apellido_materno, nombres);

CREATE TABLE IF NOT EXISTS anamnesis (
    id                      INTEGER PRIMARY KEY,
    paciente_id             INTEGER NOT NULL UNIQUE REFERENCES paciente(id) ON DELETE CASCADE,
    antecedentes_medicos    TEXT,
    alergias                TEXT,
    medicacion_actual       TEXT,
    enfermedades_json       TEXT NOT NULL DEFAULT '{}',   -- {"diabetes":true,"hipertension":false,...}
    habitos                 TEXT,
    fuma                    INTEGER NOT NULL DEFAULT 0 CHECK (fuma IN (0,1)),
    gestante                INTEGER NOT NULL DEFAULT 0 CHECK (gestante IN (0,1)),
    presion_arterial        TEXT,                          -- "120/80"
    observaciones           TEXT,
    actualizado_por         INTEGER REFERENCES usuario(id),
    actualizado_en          TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS consentimiento (
    id              INTEGER PRIMARY KEY,
    paciente_id     INTEGER NOT NULL REFERENCES paciente(id) ON DELETE CASCADE,
    tipo            TEXT NOT NULL CHECK (tipo IN ('DATOS_PERSONALES','TRATAMIENTO')),
    plan_id         INTEGER REFERENCES plan_tratamiento(id) ON DELETE SET NULL,
    aceptado_en     TEXT NOT NULL,
    firmado_por     TEXT NOT NULL,                 -- paciente o apoderado
    registrado_por  INTEGER NOT NULL REFERENCES usuario(id)
);
CREATE INDEX IF NOT EXISTS ix_consentimiento_paciente ON consentimiento(paciente_id, tipo);

CREATE TABLE IF NOT EXISTS adjunto (
    id              INTEGER PRIMARY KEY,
    paciente_id     INTEGER NOT NULL REFERENCES paciente(id) ON DELETE CASCADE,
    atencion_id     INTEGER REFERENCES atencion(id) ON DELETE SET NULL,
    tipo            TEXT NOT NULL CHECK (tipo IN ('RADIOGRAFIA','FOTO','DOCUMENTO')),
    nombre_original TEXT NOT NULL,
    ruta_relativa   TEXT NOT NULL UNIQUE,          -- adjuntos/<pacienteId>/<uuid>.<ext>
    sha256          TEXT NOT NULL,
    tamano_bytes    INTEGER NOT NULL,
    descripcion     TEXT,
    creado_en       TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_adjunto_paciente ON adjunto(paciente_id);

-- ------------------------------------------------------------ ATENCIÓN CLÍNICA
CREATE TABLE IF NOT EXISTS atencion (
    id                  INTEGER PRIMARY KEY,
    paciente_id         INTEGER NOT NULL REFERENCES paciente(id),
    odontologo_id       INTEGER NOT NULL REFERENCES odontologo(id),
    cita_id             INTEGER UNIQUE REFERENCES cita(id) ON DELETE SET NULL,
    fecha               TEXT NOT NULL,
    motivo_consulta     TEXT NOT NULL,
    examen_clinico      TEXT,
    diagnostico         TEXT,
    observaciones       TEXT,
    estado              TEXT NOT NULL DEFAULT 'ABIERTA' CHECK (estado IN ('ABIERTA','CERRADA')),
    cerrada_en          TEXT
);
CREATE INDEX IF NOT EXISTS ix_atencion_paciente ON atencion(paciente_id, fecha);

-- ------------------------------------------------------------ ODONTOGRAMA
CREATE TABLE IF NOT EXISTS odontograma (
    id                  INTEGER PRIMARY KEY,
    paciente_id         INTEGER NOT NULL REFERENCES paciente(id),
    atencion_id         INTEGER NOT NULL REFERENCES atencion(id),
    tipo                TEXT NOT NULL CHECK (tipo IN ('INICIAL','EVOLUCION')),
    denticion           TEXT NOT NULL CHECK (denticion IN ('PERMANENTE','DECIDUA','MIXTA')),
    especificaciones    TEXT,
    observaciones       TEXT,
    estado              TEXT NOT NULL DEFAULT 'BORRADOR' CHECK (estado IN ('BORRADOR','CERRADO')),
    creado_en           TEXT NOT NULL,
    cerrado_en          TEXT,
    cerrado_por         INTEGER REFERENCES usuario(id),
    hash_anterior       TEXT,                      -- hash del odontograma cerrado previo ('GENESIS' si es el primero)
    hash_cierre         TEXT,                      -- SHA-256 hex
    CHECK (estado = 'BORRADOR' OR (cerrado_en IS NOT NULL AND hash_cierre IS NOT NULL))
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_odontograma_inicial ON odontograma(paciente_id) WHERE tipo = 'INICIAL';
CREATE UNIQUE INDEX IF NOT EXISTS ux_odontograma_borrador ON odontograma(paciente_id) WHERE estado = 'BORRADOR';
CREATE INDEX IF NOT EXISTS ix_odontograma_paciente ON odontograma(paciente_id, creado_en);

CREATE TABLE IF NOT EXISTS hallazgo_tipo (
    id                  INTEGER PRIMARY KEY,
    codigo              TEXT NOT NULL UNIQUE,
    nombre              TEXT NOT NULL,
    sigla               TEXT,                      -- se dibuja en el recuadro de la pieza
    siglas_opciones     TEXT,                      -- JSON ["AM","R","IV"] si el usuario debe elegir
    representacion      TEXT NOT NULL CHECK (representacion IN ('SUPERFICIE','PIEZA','ENTRE_PIEZAS','RANGO_PIEZAS')),
    simbolo             TEXT NOT NULL CHECK (simbolo IN (
                            'RELLENO','CONTORNO_SUPERFICIE','ASPA','CIRCUNFERENCIA','LINEA_RAIZ',
                            'FLECHA_ARRIBA','FLECHA_ABAJO','FLECHA_GIRO','LINEA_FRACTURA',
                            'PARENTESIS','PUENTE','LLAVE','SOLO_SIGLA','RAIZ_TRIANGULO','ZIGZAG')),
    regla_color         TEXT NOT NULL CHECK (regla_color IN ('POR_ESTADO','SIEMPRE_ROJO','SIEMPRE_AZUL')),
    grupo_exclusion     TEXT NOT NULL,             -- capa: un nuevo hallazgo de la misma capa reemplaza al anterior
    oculta_superficies  INTEGER NOT NULL DEFAULT 0 CHECK (oculta_superficies IN (0,1)),  -- p. ej. pieza ausente
    conteo_cpod         TEXT NOT NULL DEFAULT 'NINGUNO' CHECK (conteo_cpod IN ('C','P','O','NINGUNO')),
    conteo_ceod         TEXT NOT NULL DEFAULT 'NINGUNO' CHECK (conteo_ceod IN ('C','E','O','NINGUNO')),
    activo              INTEGER NOT NULL DEFAULT 1 CHECK (activo IN (0,1)),
    orden               INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS hallazgo (
    id                  INTEGER PRIMARY KEY,
    odontograma_id      INTEGER NOT NULL REFERENCES odontograma(id) ON DELETE CASCADE,
    hallazgo_tipo_id    INTEGER NOT NULL REFERENCES hallazgo_tipo(id),
    pieza_fdi           INTEGER NOT NULL,
    pieza_fdi_fin       INTEGER,                   -- solo ENTRE_PIEZAS / RANGO_PIEZAS
    superficies         INTEGER NOT NULL DEFAULT 0 CHECK (superficies BETWEEN 0 AND 31),
    estado              TEXT CHECK (estado IN ('BUENO','MALO')),
    sigla               TEXT,                      -- sigla elegida (si el tipo tiene opciones)
    nota                TEXT,
    registrado_por      INTEGER NOT NULL REFERENCES usuario(id),
    registrado_en       TEXT NOT NULL,
    CHECK ((pieza_fdi / 10 BETWEEN 1 AND 4 AND pieza_fdi % 10 BETWEEN 1 AND 8)
        OR (pieza_fdi / 10 BETWEEN 5 AND 8 AND pieza_fdi % 10 BETWEEN 1 AND 5)),
    CHECK (pieza_fdi_fin IS NULL
        OR (pieza_fdi_fin / 10 BETWEEN 1 AND 4 AND pieza_fdi_fin % 10 BETWEEN 1 AND 8)
        OR (pieza_fdi_fin / 10 BETWEEN 5 AND 8 AND pieza_fdi_fin % 10 BETWEEN 1 AND 5))
);
CREATE INDEX IF NOT EXISTS ix_hallazgo_odontograma ON hallazgo(odontograma_id);
CREATE INDEX IF NOT EXISTS ix_hallazgo_pieza ON hallazgo(pieza_fdi);

CREATE TABLE IF NOT EXISTS hallazgo_anulacion (
    id              INTEGER PRIMARY KEY,
    hallazgo_id     INTEGER NOT NULL UNIQUE REFERENCES hallazgo(id),
    motivo          TEXT NOT NULL CHECK (length(trim(motivo)) >= 10),
    anulado_por     INTEGER NOT NULL REFERENCES usuario(id),
    anulado_en      TEXT NOT NULL
);

-- ------------------------------------------------------------ TARIFARIO Y PLAN
CREATE TABLE IF NOT EXISTS procedimiento (
    id              INTEGER PRIMARY KEY,
    codigo          TEXT NOT NULL UNIQUE,
    nombre          TEXT NOT NULL,
    categoria       TEXT NOT NULL CHECK (categoria IN ('DIAGNOSTICO','PREVENTIVO','OPERATORIA','ENDODONCIA',
                        'PERIODONCIA','CIRUGIA','PROTESIS','IMPLANTOLOGIA','ORTODONCIA','ESTETICA')),
    precio_cent     INTEGER NOT NULL CHECK (precio_cent >= 0),
    forma_cobro     TEXT NOT NULL CHECK (forma_cobro IN ('PIEZA','SUPERFICIE','ARCADA','BOCA','SESION')),
    duracion_min    INTEGER NOT NULL DEFAULT 30 CHECK (duracion_min > 0),
    activo          INTEGER NOT NULL DEFAULT 1 CHECK (activo IN (0,1))
);

CREATE TABLE IF NOT EXISTS regla_sugerencia (
    id                  INTEGER PRIMARY KEY,
    hallazgo_tipo_id    INTEGER REFERENCES hallazgo_tipo(id) ON DELETE CASCADE,  -- NULL = regla general (toda la boca)
    estado              TEXT CHECK (estado IN ('BUENO','MALO')),                 -- NULL = cualquiera
    tipo_radicular      TEXT CHECK (tipo_radicular IN ('UNIRRADICULAR','BIRRADICULAR','MULTIRRADICULAR')),
    min_superficies     INTEGER NOT NULL DEFAULT 0,
    max_superficies     INTEGER NOT NULL DEFAULT 5,
    procedimiento_id    INTEGER NOT NULL REFERENCES procedimiento(id),
    fase                INTEGER NOT NULL CHECK (fase BETWEEN 1 AND 4),
    prioridad           INTEGER NOT NULL DEFAULT 100,                            -- menor = se evalúa primero
    activo              INTEGER NOT NULL DEFAULT 1 CHECK (activo IN (0,1)),
    CHECK (min_superficies <= max_superficies)
);

CREATE TABLE IF NOT EXISTS plan_tratamiento (
    id                  INTEGER PRIMARY KEY,
    paciente_id         INTEGER NOT NULL REFERENCES paciente(id),
    odontologo_id       INTEGER NOT NULL REFERENCES odontologo(id),
    odontograma_id      INTEGER REFERENCES odontograma(id),   -- odontograma que originó las sugerencias
    fecha               TEXT NOT NULL,
    estado              TEXT NOT NULL DEFAULT 'BORRADOR'
                        CHECK (estado IN ('BORRADOR','PROPUESTO','ACEPTADO','EN_CURSO','FINALIZADO','ANULADO')),
    subtotal_cent       INTEGER NOT NULL DEFAULT 0 CHECK (subtotal_cent >= 0),
    descuento_cent      INTEGER NOT NULL DEFAULT 0 CHECK (descuento_cent >= 0),
    total_cent          INTEGER NOT NULL DEFAULT 0 CHECK (total_cent >= 0),
    observaciones       TEXT,
    aceptado_en         TEXT,
    CHECK (total_cent = subtotal_cent - descuento_cent)
);
CREATE INDEX IF NOT EXISTS ix_plan_paciente ON plan_tratamiento(paciente_id, fecha);

CREATE TABLE IF NOT EXISTS plan_item (
    id                  INTEGER PRIMARY KEY,
    plan_id             INTEGER NOT NULL REFERENCES plan_tratamiento(id) ON DELETE CASCADE,
    procedimiento_id    INTEGER NOT NULL REFERENCES procedimiento(id),
    hallazgo_id         INTEGER REFERENCES hallazgo(id),
    pieza_fdi           INTEGER,
    superficies         INTEGER NOT NULL DEFAULT 0 CHECK (superficies BETWEEN 0 AND 31),
    fase                INTEGER NOT NULL CHECK (fase BETWEEN 1 AND 4),
    cantidad            INTEGER NOT NULL DEFAULT 1 CHECK (cantidad > 0),
    precio_unit_cent    INTEGER NOT NULL CHECK (precio_unit_cent >= 0),
    descuento_cent      INTEGER NOT NULL DEFAULT 0 CHECK (descuento_cent >= 0),
    total_cent          INTEGER NOT NULL CHECK (total_cent >= 0),
    estado              TEXT NOT NULL DEFAULT 'PENDIENTE'
                        CHECK (estado IN ('PENDIENTE','EN_PROCESO','REALIZADO','CANCELADO')),
    atencion_id         INTEGER REFERENCES atencion(id),      -- dónde se realizó
    realizado_en        TEXT,
    orden               INTEGER NOT NULL DEFAULT 0,
    CHECK (pieza_fdi IS NULL
        OR (pieza_fdi / 10 BETWEEN 1 AND 4 AND pieza_fdi % 10 BETWEEN 1 AND 8)
        OR (pieza_fdi / 10 BETWEEN 5 AND 8 AND pieza_fdi % 10 BETWEEN 1 AND 5)),
    CHECK (total_cent = cantidad * precio_unit_cent - descuento_cent)
);
CREATE INDEX IF NOT EXISTS ix_plan_item_plan ON plan_item(plan_id, fase, orden);

-- ------------------------------------------------------------ CAJA
CREATE TABLE IF NOT EXISTS cuota (
    id              INTEGER PRIMARY KEY,
    plan_id         INTEGER NOT NULL REFERENCES plan_tratamiento(id) ON DELETE CASCADE,
    numero          INTEGER NOT NULL CHECK (numero >= 0),            -- 0 = cuota inicial
    vencimiento     TEXT NOT NULL,
    monto_cent      INTEGER NOT NULL CHECK (monto_cent > 0),
    pagado_cent     INTEGER NOT NULL DEFAULT 0 CHECK (pagado_cent >= 0),
    UNIQUE (plan_id, numero),
    CHECK (pagado_cent <= monto_cent)
);
CREATE INDEX IF NOT EXISTS ix_cuota_vencimiento ON cuota(vencimiento) WHERE pagado_cent < monto_cent;

CREATE TABLE IF NOT EXISTS pago (
    id                  INTEGER PRIMARY KEY,
    numero_recibo       TEXT NOT NULL UNIQUE,                        -- R-00000001
    paciente_id         INTEGER NOT NULL REFERENCES paciente(id),
    plan_id             INTEGER REFERENCES plan_tratamiento(id),
    fecha               TEXT NOT NULL,
    monto_cent          INTEGER NOT NULL CHECK (monto_cent > 0),
    medio               TEXT NOT NULL CHECK (medio IN ('EFECTIVO','TARJETA','YAPE','PLIN','TRANSFERENCIA')),
    referencia          TEXT,
    registrado_por      INTEGER NOT NULL REFERENCES usuario(id),
    anulado             INTEGER NOT NULL DEFAULT 0 CHECK (anulado IN (0,1)),
    motivo_anulacion    TEXT,
    anulado_en          TEXT,
    CHECK (anulado = 0 OR (motivo_anulacion IS NOT NULL AND anulado_en IS NOT NULL))
);
CREATE INDEX IF NOT EXISTS ix_pago_fecha ON pago(fecha);
CREATE INDEX IF NOT EXISTS ix_pago_paciente ON pago(paciente_id);

CREATE TABLE IF NOT EXISTS pago_aplicacion (
    id          INTEGER PRIMARY KEY,
    pago_id     INTEGER NOT NULL REFERENCES pago(id) ON DELETE CASCADE,
    cuota_id    INTEGER NOT NULL REFERENCES cuota(id),
    monto_cent  INTEGER NOT NULL CHECK (monto_cent > 0),
    UNIQUE (pago_id, cuota_id)
);

-- ------------------------------------------------------------ AGENDA
CREATE TABLE IF NOT EXISTS cita (
    id                      INTEGER PRIMARY KEY,
    paciente_id             INTEGER NOT NULL REFERENCES paciente(id),
    odontologo_id           INTEGER NOT NULL REFERENCES odontologo(id),
    inicio                  TEXT NOT NULL,
    fin                     TEXT NOT NULL,
    motivo                  TEXT NOT NULL,
    estado                  TEXT NOT NULL DEFAULT 'PROGRAMADA'
                            CHECK (estado IN ('PROGRAMADA','CONFIRMADA','ATENDIDA','CANCELADA','NO_ASISTIO')),
    plan_item_id            INTEGER REFERENCES plan_item(id) ON DELETE SET NULL,
    notas                   TEXT,
    recordatorio_enviado_en TEXT,
    creado_por              INTEGER NOT NULL REFERENCES usuario(id),
    creado_en               TEXT NOT NULL,
    CHECK (fin > inicio)
);
CREATE INDEX IF NOT EXISTS ix_cita_odontologo_inicio ON cita(odontologo_id, inicio);
CREATE INDEX IF NOT EXISTS ix_cita_paciente_inicio ON cita(paciente_id, inicio);
CREATE INDEX IF NOT EXISTS ix_cita_recordatorio ON cita(inicio)
    WHERE recordatorio_enviado_en IS NULL AND estado IN ('PROGRAMADA','CONFIRMADA');

CREATE TABLE IF NOT EXISTS notificacion_log (
    id          INTEGER PRIMARY KEY,
    cita_id     INTEGER REFERENCES cita(id) ON DELETE SET NULL,
    canal       TEXT NOT NULL CHECK (canal IN ('EMAIL')),
    destino     TEXT NOT NULL,
    estado      TEXT NOT NULL CHECK (estado IN ('ENVIADO','FALLIDO','OMITIDO')),
    detalle     TEXT,
    fecha       TEXT NOT NULL
);

-- ------------------------------------------------------------ AUDITORÍA
CREATE TABLE IF NOT EXISTS auditoria (
    id          INTEGER PRIMARY KEY,
    usuario_id  INTEGER REFERENCES usuario(id),
    accion      TEXT NOT NULL,              -- LOGIN, HALLAZGO_CREAR, HALLAZGO_ANULAR, ODONTOGRAMA_CERRAR, PAGO_REGISTRAR...
    entidad     TEXT,
    entidad_id  INTEGER,
    detalle     TEXT,                       -- JSON (Gson)
    fecha       TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_auditoria_fecha ON auditoria(fecha);
CREATE INDEX IF NOT EXISTS ix_auditoria_entidad ON auditoria(entidad, entidad_id);
```

**Notas para la implementación:**
- SQLite permite declarar FK hacia tablas definidas más adelante en el script (por ejemplo `consentimiento → plan_tratamiento`, `atencion → cita`); la verificación ocurre al insertar. No reordenar por eso.
- `ux_odontograma_borrador` garantiza **un solo borrador abierto por paciente**.
- `ConversoresIntegracionTest` (Fase 0) debe insertar y leer una entidad con `LocalDate`, `LocalDateTime`, `LocalTime`, dinero y superficies contra un SQLite temporal, y verificar el texto exacto guardado. Si Hibernate no aplica los `AttributeConverter` con `autoApply` a los tipos `java.time`, anotar cada atributo con `@Convert(converter = ...)`.

### 3.4 Tabla de configuración (claves por defecto)

`ConfiguracionSeeder` inserta solo las claves que **no existen** (`INSERT OR IGNORE`), así nunca pisa valores del usuario.

| clave | tipo | grupo | valor por defecto |
|---|---|---|---|
| `esquema.version` | ENTERO | sistema | `1` (no editable) |
| `clinica.nombre` | TEXTO | clinica | `Consultorio Odontológico Demo` |
| `clinica.ruc` | TEXTO | clinica | `20000000001` (ficticio) |
| `clinica.direccion` / `clinica.telefono` / `clinica.email` | TEXTO | clinica | demo |
| `clinica.logo_ruta` | TEXTO | clinica | vacío (usa `img/logo.png`) |
| `agenda.duracion_cita_min` | ENTERO | agenda | `30` |
| `agenda.intervalo_min` | ENTERO | agenda | `15` |
| `agenda.dias_busqueda_hueco` | ENTERO | agenda | `30` |
| `recordatorio.habilitado` | BOOLEANO | recordatorio | `false` |
| `recordatorio.horas_antes` | ENTERO | recordatorio | `24` |
| `email.smtp.host` / `email.smtp.puerto` / `email.smtp.usuario` / `email.remitente` | TEXTO/ENTERO | email | `smtp.gmail.com` / `587` / vacío / vacío |
| `email.smtp.password` | **SECRETO** | email | vacío |
| `email.smtp.starttls` | BOOLEANO | email | `true` |
| `api.dni.habilitado` | BOOLEANO | api_dni | `false` |
| `api.dni.url_plantilla` | TEXTO | api_dni | vacío — p. ej. `https://<proveedor>/v1/dni/{numero}` |
| `api.dni.cabecera_auth` | TEXTO | api_dni | `Authorization` |
| `api.dni.prefijo_token` | TEXTO | api_dni | `Bearer ` |
| `api.dni.token` | **SECRETO** | api_dni | vacío (**API Key dinámica**) |
| `api.dni.mapeo_json` | JSON | api_dni | `{"nombres":"nombres","apellidoPaterno":"apellidoPaterno","apellidoMaterno":"apellidoMaterno"}` |
| `api.timeout_seg` | ENTERO | api_dni | `6` |
| `respaldo.automatico_al_cerrar` | BOOLEANO | respaldo | `true` |
| `respaldo.mantener` | ENTERO | respaldo | `10` (respaldos automáticos a conservar) |
| `odontograma.excluir_terceros_molares_cpod` | BOOLEANO | odontograma | `true` |
| `ui.tema` | TEXTO | ui | `CLARO` |

El proveedor de la API de DNI **no se fija en código**: URL, cabecera, token y nombres de campos JSON son configurables. Antes de la Fase 7, el estudiante elige un proveedor, revisa su documentación vigente y llena estas claves.

`ConfiguracionService`:
```java
String texto(String clave);
int entero(String clave);
boolean booleano(String clave);
Optional<String> secreto(String clave);              // descifra con CifradorSecretos
<T> T json(String clave, Class<T> tipo);             // Gson
@Transactional void guardar(Map<String, String> valores);   // cifra los de tipo SECRETO; audita sin registrar el valor
```
Caché en memoria (`ConcurrentHashMap`) invalidada al guardar.

`CifradorSecretos`: AES/GCM/NoPadding, clave de 256 bits generada con `SecureRandom` en el primer arranque y guardada en `${odontogest.home}/clave.key`. Formato del valor: `enc:v1:` + Base64(IV de 12 bytes ‖ texto cifrado + tag). En la UI los secretos se muestran como `••••••` y solo se reemplazan si el usuario escribe uno nuevo. **Limitación honesta (para el informe):** la clave vive en el mismo equipo; protege contra quien copie solo la BD, no contra quien tenga acceso a toda la carpeta.

### 3.5 Seeders (`DatabaseSeeder` y catálogos)

Orden y condición:

| # | Clase | Condición | Contenido | Idempotencia |
|---|---|---|---|---|
| 1 | `ConfiguracionSeeder` | siempre (`@Order(1)`, `ApplicationRunner`) | claves §3.4, correlativos `HC` y `RECIBO` en 0 | `INSERT OR IGNORE` |
| 2 | `CatalogoSeeder` | siempre (`@Order(2)`) | hallazgos, procedimientos, reglas | busca por `codigo`; inserta si falta |
| 3 | `DatabaseSeeder` | `app.seeder.enabled=true` (`@Order(3)`) | orquesta 4–7 | si `usuario` tiene filas, no hace nada |
| 4 | `UsuarioSeeder` | — | `admin/Admin123*`, `odontologo/Odonto123*` (vinculado al odontólogo 1), `recepcion/Recep123*` | — |
| 5 | `OdontologoSeeder` | — | 3 odontólogos (general, endodoncista, ortodoncista), horarios L–V 09:00–13:00 y 15:00–19:00, S 09:00–13:00 | — |
| 6 | `PacienteSeeder` | — | 60 pacientes con nombres peruanos, DNI ficticios, edades 4–80 (10 menores con apoderado), anamnesis, consentimiento de datos | — |
| 7 | `ClinicaDemoSeeder` | — | para 40 pacientes: atención + odontograma INICIAL cerrado con 3–12 hallazgos coherentes con la edad (decidua < 6 años, mixta 6–12, permanente > 12); para 15 de ellos un odontograma de EVOLUCIÓN que reemplaza caries por restauraciones; planes generados con el motor de sugerencias; cronogramas; pagos aplicados FIFO; 120 citas entre hoy −30 y hoy +30 días sin solapamientos | — |

Reglas del seeder demo:
- Usa **los mismos servicios** que la UI (`OdontogramaService.registrarHallazgo`, `PagoService.registrar`, `CitaService.programar`), no SQL directo, para que los datos demo cumplan todas las validaciones y generen hashes válidos.
- `Random` con semilla `app.seeder.semilla` → datos reproducibles.
- Todos los DNI empiezan con `9` y se marcan en la UI con la etiqueta “Paciente demo” (columna derivada: `numero_historia` ≤ `HC-000060` creado por el seeder; guardar en `configuracion` la clave `seeder.ultimo_hc_demo`).
- El seeder de la versión INDECOPI se deja activo para que el evaluador vea el sistema con datos.

#### Catálogo inicial de hallazgos (⚠️ validar contra el Anexo de la NTS 188)
| codigo | nombre | sigla | representación | símbolo | regla color | grupo exclusión | CPO-D | ceo-d |
|---|---|---|---|---|---|---|---|---|
| CARIES | Lesión de caries dental | — | SUPERFICIE | RELLENO | SIEMPRE_ROJO | SUPERFICIE | C | C |
| RESTAURACION_DEFINITIVA | Restauración definitiva | AM/R/IV/IM/IE | SUPERFICIE | RELLENO | POR_ESTADO | SUPERFICIE | O | O |
| RESTAURACION_TEMPORAL | Restauración temporal | — | SUPERFICIE | CONTORNO_SUPERFICIE | SIEMPRE_ROJO | SUPERFICIE | O | O |
| SELLANTE | Sellante | S | SUPERFICIE | SOLO_SIGLA | POR_ESTADO | SUPERFICIE_SELLANTE | NINGUNO | NINGUNO |
| PIEZA_AUSENTE | Pieza dentaria ausente | — | PIEZA | ASPA | SIEMPRE_AZUL | ESTADO_PIEZA (oculta superficies) | P | E |
| REMANENTE_RADICULAR | Remanente radicular | RR | PIEZA | RAIZ_TRIANGULO | SIEMPRE_ROJO | ESTADO_PIEZA | C | E |
| IMPLANTE | Implante dental | IMP | PIEZA | SOLO_SIGLA | POR_ESTADO | ESTADO_PIEZA | P | NINGUNO |
| PIEZA_EN_ERUPCION | Pieza dentaria en erupción | — | PIEZA | ZIGZAG | SIEMPRE_AZUL | ESTADO_PIEZA | NINGUNO | NINGUNO |
| CORONA_DEFINITIVA | Corona definitiva | CM/CF/CMC/CV/CLM | PIEZA | CIRCUNFERENCIA | POR_ESTADO | CORONA | O | O |
| CORONA_TEMPORAL | Corona temporal | — | PIEZA | CIRCUNFERENCIA | SIEMPRE_ROJO | CORONA | O | O |
| TRATAMIENTO_PULPAR | Tratamiento pulpar | TC/PC/PP | PIEZA | LINEA_RAIZ | POR_ESTADO | PULPAR | NINGUNO | NINGUNO |
| FRACTURA | Fractura | — | PIEZA | LINEA_FRACTURA | SIEMPRE_ROJO | FRACTURA | NINGUNO | NINGUNO |
| PIEZA_EXTRUIDA | Pieza extruida | — | PIEZA | FLECHA_ARRIBA | SIEMPRE_ROJO | POSICION_VERTICAL | NINGUNO | NINGUNO |
| PIEZA_INTRUIDA | Pieza intruida | — | PIEZA | FLECHA_ABAJO | SIEMPRE_ROJO | POSICION_VERTICAL | NINGUNO | NINGUNO |
| GIROVERSION | Giroversión | — | PIEZA | FLECHA_GIRO | SIEMPRE_ROJO | GIRO | NINGUNO | NINGUNO |
| MOVILIDAD | Movilidad patológica | M1/M2/M3 | PIEZA | SOLO_SIGLA | SIEMPRE_ROJO | MOVILIDAD | NINGUNO | NINGUNO |
| DIASTEMA | Diastema | — | ENTRE_PIEZAS | PARENTESIS | SIEMPRE_ROJO | DIASTEMA | NINGUNO | NINGUNO |
| PROTESIS_FIJA | Prótesis fija | — | RANGO_PIEZAS | PUENTE | POR_ESTADO | PROTESIS_FIJA | NINGUNO | NINGUNO |
| PROTESIS_REMOVIBLE | Prótesis removible | — | RANGO_PIEZAS | LLAVE | POR_ESTADO | PROTESIS_REMOVIBLE | NINGUNO | NINGUNO |
| ORTODONCIA_FIJA | Aparato ortodóntico fijo | — | RANGO_PIEZAS | LLAVE | POR_ESTADO | ORTODONCIA | NINGUNO | NINGUNO |

#### Tarifario demo (precios ficticios en soles)
`CONS` Consulta y diagnóstico 50.00 (BOCA) · `RX-PA` Radiografía periapical 25.00 (PIEZA) · `PROF` Profilaxis 80.00 (BOCA) · `DEST` Destartraje 120.00 (BOCA) · `FLUOR` Fluorización 60.00 (BOCA) · `SELL` Sellante 50.00 (PIEZA) · `RES-S` Resina simple (1 sup.) 120.00 · `RES-C` Resina compuesta (≥ 2 sup.) 180.00 (PIEZA) · `IONO-T` Ionómero temporal 70.00 (PIEZA) · `PULPO` Pulpotomía 150.00 · `ENDO-U` Endodoncia unirradicular 350.00 · `ENDO-B` birradicular 450.00 · `ENDO-M` multirradicular 600.00 · `EXO` Exodoncia simple 100.00 · `EXO-RR` Remoción de remanente radicular 120.00 · `CORO-T` Corona temporal 150.00 · `CORO-MC` Corona metal-cerámica 900.00 · `PF-PZ` Prótesis fija por pieza 800.00 · `IMPL` Implante 3500.00 · `FERU` Férula 400.00 (ARCADA).

#### Reglas de sugerencia demo
| hallazgo | estado | tipo radicular | superficies | → procedimiento | fase | prioridad |
|---|---|---|---|---|---|---|
| (general) | — | — | — | CONS | 1 | 1 |
| (general) | — | — | — | PROF | 4 | 2 |
| REMANENTE_RADICULAR | — | — | — | EXO-RR | 1 | 10 |
| FRACTURA | — | — | — | RES-C | 1 | 20 |
| CARIES | — | — | 1–1 | RES-S | 2 | 30 |
| CARIES | — | — | 2–5 | RES-C | 2 | 31 |
| RESTAURACION_TEMPORAL | — | — | 1–1 | RES-S | 2 | 32 |
| RESTAURACION_TEMPORAL | — | — | 2–5 | RES-C | 2 | 33 |
| RESTAURACION_DEFINITIVA | MALO | — | 1–1 | RES-S | 2 | 34 |
| RESTAURACION_DEFINITIVA | MALO | — | 2–5 | RES-C | 2 | 35 |
| TRATAMIENTO_PULPAR | MALO | UNIRRADICULAR | — | ENDO-U | 1 | 40 |
| TRATAMIENTO_PULPAR | MALO | BIRRADICULAR | — | ENDO-B | 1 | 41 |
| TRATAMIENTO_PULPAR | MALO | MULTIRRADICULAR | — | ENDO-M | 1 | 42 |
| CORONA_TEMPORAL | — | — | — | CORO-MC | 3 | 50 |
| CORONA_DEFINITIVA | MALO | — | — | CORO-MC | 3 | 51 |
| MOVILIDAD | — | — | — | DEST | 2 | 60 |

---

## 4. Configuración de Maven (`pom.xml`)

Versiones: se reutilizan las del repo del docente (ya probadas juntas). Se sube Java a 21 (LTS; `jpackage` disponible). Si el curso exige Java 17, cambiar `java.version` a 17: no se usa ninguna API exclusiva de 21.

Dependencias **descartadas** a propósito: `pdfbox` (OpenPDF basta para generar), `jlatexmath` (no hay fórmulas que renderizar), `jackson` (Gson es suficiente y más liviano para 2 integraciones).

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.4</version>
        <relativePath/>
    </parent>

    <groupId>com.odontogest</groupId>
    <artifactId>odontogest</artifactId>
    <version>1.0.0</version>
    <name>OdontoGest</name>
    <description>Gestión de consultorio odontológico con odontograma interactivo</description>

    <properties>
        <java.version>21</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <javafx.version>21.0.4</javafx.version>
        <atlantafx.version>2.0.1</atlantafx.version>
        <ikonli.version>12.3.1</ikonli.version>
        <sqlite.version>3.45.3.0</sqlite.version>
        <openpdf.version>1.3.43</openpdf.version>
        <poi.version>5.2.5</poi.version>
        <archunit.version>1.3.0</archunit.version>
        <main.class>com.odontogest.Launcher</main.class>
        <!-- jpackage -->
        <jpackage.type>app-image</jpackage.type>  <!-- app-image | msi | exe (msi/exe requieren WiX en Windows) -->
        <jpackage.vendor>OdontoGest - Proyecto Ingeniería de Software</jpackage.vendor>
    </properties>

    <dependencies>
        <!-- Persistencia -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.xerial</groupId>
            <artifactId>sqlite-jdbc</artifactId>
            <version>${sqlite.version}</version>
        </dependency>
        <dependency>
            <groupId>org.hibernate.orm</groupId>
            <artifactId>hibernate-community-dialects</artifactId>
        </dependency>

        <!-- Validación, correo, hash de contraseñas -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-mail</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-crypto</artifactId>
        </dependency>

        <!-- JSON para integraciones (versión gestionada por Spring Boot) -->
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Interfaz gráfica -->
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-controls</artifactId>
            <version>${javafx.version}</version>
        </dependency>
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-fxml</artifactId>
            <version>${javafx.version}</version>
        </dependency>
        <dependency>
            <!-- SwingFXUtils: convierte el snapshot del odontograma (WritableImage) a PNG para el PDF -->
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-swing</artifactId>
            <version>${javafx.version}</version>
        </dependency>
        <dependency>
            <groupId>io.github.mkpaz</groupId>
            <artifactId>atlantafx-base</artifactId>
            <version>${atlantafx.version}</version>
        </dependency>
        <dependency>
            <groupId>org.kordamp.ikonli</groupId>
            <artifactId>ikonli-javafx</artifactId>
            <version>${ikonli.version}</version>
        </dependency>
        <dependency>
            <groupId>org.kordamp.ikonli</groupId>
            <artifactId>ikonli-feather-pack</artifactId>
            <version>${ikonli.version}</version>
        </dependency>

        <!-- Documentos -->
        <dependency>
            <groupId>com.github.librepdf</groupId>
            <artifactId>openpdf</artifactId>
            <version>${openpdf.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi-ooxml</artifactId>
            <version>${poi.version}</version>
        </dependency>

        <!-- Pruebas -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.tngtech.archunit</groupId>
            <artifactId>archunit-junit5</artifactId>
            <version>${archunit.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <finalName>odontogest</finalName>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <mainClass>${main.class}</mainClass>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
                <executions>
                    <!-- Genera META-INF/build-info.properties → BuildProperties (versión en "Acerca de") -->
                    <execution>
                        <id>build-info</id>
                        <goals><goal>build-info</goal></goals>
                    </execution>
                </executions>
            </plugin>

            <plugin>
                <!-- mvn javafx:run durante el desarrollo -->
                <groupId>org.openjfx</groupId>
                <artifactId>javafx-maven-plugin</artifactId>
                <version>0.0.8</version>
                <configuration>
                    <mainClass>${main.class}</mainClass>
                </configuration>
            </plugin>
        </plugins>
    </build>

    <profiles>
        <!-- mvn clean package -Pempaquetar   (EJECUTAR EN WINDOWS para generar el .exe con natives de Windows) -->
        <profile>
            <id>empaquetar</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-antrun-plugin</artifactId>
                        <version>3.1.0</version>
                        <executions>
                            <execution>
                                <id>preparar-entrada-jpackage</id>
                                <phase>package</phase>
                                <goals><goal>run</goal></goals>
                                <configuration>
                                    <target>
                                        <delete dir="${project.build.directory}/jpackage-input" quiet="true"/>
                                        <delete dir="${project.build.directory}/dist" quiet="true"/>
                                        <copy file="${project.build.directory}/${project.build.finalName}.jar"
                                              todir="${project.build.directory}/jpackage-input"/>
                                    </target>
                                </configuration>
                            </execution>
                        </executions>
                    </plugin>
                    <plugin>
                        <groupId>org.codehaus.mojo</groupId>
                        <artifactId>exec-maven-plugin</artifactId>
                        <version>3.1.0</version>
                        <executions>
                            <execution>
                                <id>jpackage</id>
                                <phase>package</phase>
                                <goals><goal>exec</goal></goals>
                                <configuration>
                                    <executable>jpackage</executable>
                                    <arguments>
                                        <argument>--type</argument><argument>${jpackage.type}</argument>
                                        <argument>--name</argument><argument>OdontoGest</argument>
                                        <argument>--app-version</argument><argument>${project.version}</argument>
                                        <argument>--vendor</argument><argument>${jpackage.vendor}</argument>
                                        <argument>--input</argument><argument>${project.build.directory}/jpackage-input</argument>
                                        <argument>--main-jar</argument><argument>${project.build.finalName}.jar</argument>
                                        <argument>--dest</argument><argument>${project.build.directory}/dist</argument>
                                        <argument>--icon</argument><argument>${project.basedir}/packaging/odontogest.ico</argument>
                                        <argument>--add-modules</argument>
                                        <argument>java.se,jdk.unsupported,jdk.crypto.ec,jdk.localedata,jdk.charsets,jdk.zipfs</argument>
                                        <argument>--jlink-options</argument>
                                        <argument>--strip-debug --no-man-pages --no-header-files</argument>
                                        <argument>--java-options</argument><argument>-Xmx768m</argument>
                                        <argument>--java-options</argument><argument>-Dfile.encoding=UTF-8</argument>
                                    </arguments>
                                </configuration>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
        </profile>
    </profiles>
</project>
```

Notas de empaquetado:
- Las dependencias de JavaFX de Maven traen *natives* del SO donde se compila. **El build final para INDECOPI se hace en Windows.**
- `app-image` no requiere WiX: el resultado `target/dist/OdontoGest/` se comprime en ZIP. Para `msi`/`exe` instalar WiX Toolset y pasar `-Djpackage.type=msi`.
- Si al ejecutar el app-image aparece `ClassNotFoundException`/`NoClassDefFoundError` de un módulo del JDK, agregarlo a `--add-modules`.
- `jpackage` está en `JAVA_HOME/bin` del JDK 21: verificar con `jpackage --version`.

---

## 5. Diseño de los módulos críticos

### 5.1 Módulo Odontograma — flujo técnico completo

#### 5.1.1 Modelo de dominio
```java
public record PiezaDental(int fdi) {
    public PiezaDental { if (!esValida(fdi)) throw new IllegalArgumentException("Pieza FDI inválida: " + fdi); }
    public static boolean esValida(int fdi);          // cuadrantes 1-4: 1..8, cuadrantes 5-8: 1..5
    public Cuadrante cuadrante();                     // fdi / 10
    public int posicion();                            // fdi % 10 (1 = incisivo central)
    public boolean esDecidua();                       // cuadrante >= 5
    public Arcada arcada();                           // cuadrantes 1,2,5,6 = SUPERIOR
    public boolean ladoDerechoPaciente();             // cuadrantes 1,4,5,8 (se dibuja a la IZQUIERDA del observador)
    public boolean esAnterior();                      // posicion <= 3
    public TipoPieza tipo();                          // 1-2 INCISIVO, 3 CANINO, 4-5 PREMOLAR (permanente) / MOLAR (decidua), 6-8 MOLAR
    public TipoRadicular tipoRadicular();             // ver tabla abajo
    public boolean esTerceraMolar();                  // posicion == 8
    public static List<PiezaDental> permanentes();    // orden de dibujo: 18..11,21..28,48..41,31..38
    public static List<PiezaDental> deciduas();       // 55..51,61..65,85..81,71..75
}
```
Tipo radicular (simplificación clínica, editable en el informe): incisivos, caninos y premolares inferiores/segundos superiores → `UNIRRADICULAR`; primeros premolares superiores (14, 24) → `BIRRADICULAR`; molares → `MULTIRRADICULAR`.

```java
public enum Superficie {
    O(1), M(2), D(4), V(8), L(16);       // O se rotula "I" (incisal) en piezas anteriores; L se rotula "P" (palatino) en superiores
    public final int bit;
    public String etiqueta(PiezaDental p);
}

public record HallazgoEvento(
        long id, long odontogramaId, TipoHallazgoDef tipo,
        PiezaDental pieza, PiezaDental piezaFin,          // piezaFin null salvo ENTRE/RANGO
        EnumSet<Superficie> superficies,
        EstadoHallazgo estado,                            // null si el tipo no usa estado
        String sigla, String nota,
        LocalDateTime registradoEn, long registradoPor,
        boolean anulado) {

    public ColorHallazgo color() {
        return switch (tipo.reglaColor()) {
            case SIEMPRE_ROJO -> ColorHallazgo.ROJO;
            case SIEMPRE_AZUL -> ColorHallazgo.AZUL;
            case POR_ESTADO   -> estado == EstadoHallazgo.MALO ? ColorHallazgo.ROJO : ColorHallazgo.AZUL;
        };
    }
}

public record ClaveCapa(int piezaInicio, int piezaFin, Superficie superficie, String capa) {}
```

#### 5.1.2 Algoritmo de proyección (núcleo original)
El odontograma **no se guarda como estado mutable**: se guarda la secuencia de hallazgos (eventos). El estado visible en una fecha `t` es la reducción ordenada de los eventos hasta `t`.

```java
public final class ProyectorOdontograma {

    /** Eventos de todos los odontogramas del paciente: cerrados + el borrador actual. */
    public EstadoOdontograma proyectar(List<HallazgoEvento> eventos, LocalDateTime hasta) {
        Map<ClaveCapa, HallazgoEvento> vigentes = new LinkedHashMap<>();
        eventos.stream()
               .filter(e -> !e.anulado())
               .filter(e -> !e.registradoEn().isAfter(hasta))
               .sorted(Comparator.comparing(HallazgoEvento::registradoEn).thenComparingLong(HallazgoEvento::id))
               .forEach(e -> claves(e).forEach(k -> vigentes.put(k, e)));   // el más reciente de la capa gana
        return EstadoOdontograma.desde(vigentes);
    }

    static List<ClaveCapa> claves(HallazgoEvento e) {
        String capa = e.tipo().grupoExclusion();
        return switch (e.tipo().representacion()) {
            case SUPERFICIE -> e.superficies().stream()
                    .map(s -> new ClaveCapa(e.pieza().fdi(), e.pieza().fdi(), s, capa)).toList();
            case PIEZA -> List.of(new ClaveCapa(e.pieza().fdi(), e.pieza().fdi(), null, capa));
            case ENTRE_PIEZAS, RANGO_PIEZAS -> {
                int a = Math.min(e.pieza().fdi(), e.piezaFin().fdi()), b = Math.max(e.pieza().fdi(), e.piezaFin().fdi());
                yield List.of(new ClaveCapa(a, b, null, capa));
            }
        };
    }
}
```
Complejidad: `O(n log n)` por el ordenamiento, `n` = hallazgos del paciente (decenas o cientos).

Reglas derivadas:
- Una caries en la superficie O, seguida de una restauración en O, deja vigente solo la restauración (misma capa `SUPERFICIE`).
- Un sellante usa otra capa (`SUPERFICIE_SELLANTE`), así que convive con una restauración.
- `PIEZA_AUSENTE` (capa `ESTADO_PIEZA`, `oculta_superficies=1`): el renderizador no dibuja superficies ni hallazgos de superficie de esa pieza, pero estos siguen en el historial.
- Una restauración de 3 superficies (MOD) se registra como **un** hallazgo con bitmask `M|O|D = 7`: así el motor de sugerencias y el CPO-D lo cuentan una sola vez.

```java
public final class EstadoOdontograma {
    public Map<Integer, EstadoPieza> piezas();                    // fdi -> estado
    public List<HallazgoEvento> rangos();                         // ENTRE / RANGO vigentes
    public Set<ClaveCapa> claves();
    public Optional<HallazgoEvento> enSuperficie(int fdi, Superficie s, String capa);
}

public record DiferenciaOdontograma(Set<ClaveCapa> agregadas, Set<ClaveCapa> eliminadas, Set<ClaveCapa> modificadas) {
    public static DiferenciaOdontograma entre(EstadoOdontograma base, EstadoOdontograma objetivo);
    // modificada = misma clave con distinto hallazgo (id distinto)
    public Set<Integer> piezasAfectadas();
}
```

#### 5.1.3 Ciclo de vida inicial / evolución / cierre
```
Abrir atención ──► ¿paciente tiene odontograma INICIAL?
                     │ no ─► crear INICIAL (BORRADOR)
                     │ sí ─► crear EVOLUCION (BORRADOR)  [máx. un borrador por paciente]
                     ▼
        Registrar hallazgos (BORRADOR: se pueden eliminar físicamente,
        porque aún no está firmado; queda en auditoría)
                     ▼
        Cerrar odontograma ──► estado CERRADO, cerrado_en, cerrado_por,
                               hash_anterior = hash del último CERRADO del paciente (o "GENESIS"),
                               hash_cierre = SHA-256(...)  ──► publica OdontogramaCerrado
                     ▼
        Cerrado = inmutable. Corrección ⇒ HallazgoAnulacion (motivo ≥ 10 caracteres)
```

```java
@Service @Transactional(readOnly = true) @RequiredArgsConstructor
public class OdontogramaService {
    public OdontogramaVista obtenerVista(long pacienteId, LocalDateTime hasta);            // hasta = null → ahora
    public List<OdontogramaResumen> listarPorPaciente(long pacienteId);                   // para el ComboBox/línea de tiempo
    @Transactional public long abrirBorrador(long atencionId, Denticion denticion);
    @Transactional public OdontogramaVista registrarHallazgo(NuevoHallazgo cmd);           // devuelve la vista recalculada
    @Transactional public OdontogramaVista eliminarHallazgoBorrador(long hallazgoId);       // solo BORRADOR
    @Transactional public OdontogramaVista anularHallazgo(long hallazgoId, String motivo);  // solo CERRADO
    @Transactional public void actualizarEspecificaciones(long odontogramaId, String especificaciones, String observaciones);
    @Transactional public void cerrar(long odontogramaId);
    public DiferenciaVista comparar(long pacienteId, LocalDateTime base, LocalDateTime objetivo);
    public IndicesVista calcularIndices(long pacienteId);
}

public record NuevoHallazgo(long odontogramaId, long hallazgoTipoId, int piezaFdi, Integer piezaFdiFin,
                            Set<Superficie> superficies, EstadoHallazgo estado, String sigla, String nota) {}
```
Validaciones de `registrarHallazgo` (lanzan `NegocioException`):
1. El odontograma existe, está en `BORRADOR` y su atención está `ABIERTA`.
2. El usuario tiene permiso `ODONTOGRAMA_EDITAR` (`ODONTOLOGO` o `ADMIN`).
3. La pieza pertenece a la dentición del odontograma (`MIXTA` admite ambas).
4. `SUPERFICIE` ⇒ al menos una superficie; `PIEZA` ⇒ ninguna; `ENTRE/RANGO` ⇒ `piezaFin` en la misma arcada y distinta de `pieza`; `ENTRE_PIEZAS` ⇒ piezas contiguas en el orden de dibujo.
5. `regla_color = POR_ESTADO` ⇒ `estado` obligatorio.
6. Si el tipo tiene `siglas_opciones` ⇒ `sigla` debe estar en la lista.
7. La pieza no tiene vigente un hallazgo con `oculta_superficies` cuando se intenta registrar uno de superficie (“La pieza 36 figura como ausente”).

#### 5.1.4 Cadena de integridad
```java
public final class CadenaIntegridad {
    public static final String GENESIS = "GENESIS";
    /** hash = SHA-256( hashAnterior + "\n" + lineaCabecera + "\n" + lineas ordenadas por id ) en hex. */
    public static String calcular(String hashAnterior, OdontogramaCanonico o);
    public static ResultadoVerificacion verificar(List<OdontogramaCanonico> cerradosEnOrden);
}
// línea de cabecera: id|pacienteId|tipo|denticion|cerradoEn|cerradoPor|especificaciones
// línea de hallazgo: id|codigoTipo|pieza|piezaFin|superficies|estado|sigla|registradoEn|registradoPor
```
Las anulaciones posteriores **no** alteran el hash (son registros nuevos; se listan aparte). `IntegridadService.verificar(pacienteId)` recalcula toda la cadena e indica el primer odontograma cuyo hash no coincide.

#### 5.1.5 Geometría, dibujo y detección de superficie (UI)
**Layout (`LayoutOdontograma`)**: dado el ancho y alto disponibles, calcula una celda por pieza.
- Filas (de arriba a abajo): permanentes superiores (18…11 | 21…28), deciduas superiores (55…51 | 61…65), deciduas inferiores (85…81 | 71…75), permanentes inferiores (48…41 | 31…38). En dentición `PERMANENTE` o `DECIDUA` se omiten las filas que no aplican.
- Lado de la celda `s = min((ancho − 2·margen − 15·g − gLinea) / 16, altoFila·0.45)` con `g = 0.12·s` y un espacio extra `gLinea` en la línea media.
- Cada celda superior tiene, de arriba a abajo: **recuadro de sigla** (alto `0.35·s`), **zona radicular** (alto `0.6·s`), **corona** (cuadrado `s×s`) y el número FDI. En la arcada inferior el orden se invierte (espejo), de modo que la vestibular de ambas arcadas queda hacia el borde exterior del dibujo.
```java
public record CeldaPieza(PiezaDental pieza, Rectangle2D corona, Rectangle2D raiz, Rectangle2D recuadro, Point2D etiqueta) {}
public final class LayoutOdontograma {
    public static LayoutOdontograma calcular(double ancho, double alto, Denticion denticion);
    public Optional<CeldaPieza> celdaEn(double x, double y);     // búsqueda lineal (≤ 52 celdas)
    public CeldaPieza celda(int fdi);
    public Collection<CeldaPieza> celdas();
}
```
(`Rectangle2D`/`Point2D` de `javafx.geometry` son clases simples sin dependencia del hilo de UI.)

**Corona dividida en 5 polígonos**: cuadrado exterior `(x, y, s)` y cuadrado interior centrado de lado `0.4·s`.
- Superior: `(TL, TR, tr, tl)`; derecha: `(TR, BR, br, tr)`; inferior: `(BR, BL, bl, br)`; izquierda: `(BL, TL, tl, bl)`; centro: `(tl, tr, br, bl)` → O/I.

**Detección de superficie (`DetectorSuperficie`) — fórmula:**
```
dado clic (x, y) y la corona con centro (cx, cy) y lado s:
  dx = x − cx ;  dy = y − cy ;  h = s/2 ;  r = 0.2·s
  si |dx| > h  o  |dy| > h            → ninguna (fuera de la corona)
  si |dx| ≤ r  y  |dy| ≤ r            → O (oclusal/incisal)
  si |dy| ≥ |dx|:                      → región vertical
        arriba (dy < 0) → V si arcada SUPERIOR, L si INFERIOR
        abajo  (dy > 0) → L si arcada SUPERIOR, V si INFERIOR
  si no:                               → región horizontal
        mesial apunta a la línea media:
        pieza del lado derecho del paciente (cuadrantes 1,4,5,8, dibujada a la izquierda): derecha → M, izquierda → D
        pieza del lado izquierdo del paciente (cuadrantes 2,3,6,7, dibujada a la derecha):  izquierda → M, derecha → D
```
```java
public final class DetectorSuperficie {
    public static Optional<Superficie> detectar(double x, double y, Rectangle2D corona, PiezaDental pieza);
}
```
`DetectorSuperficieTest` cubre: centro → O; las 4 regiones para una pieza de cada cuadrante (1–8); bordes exactos y diagonales (desempate `|dy| ≥ |dx|` → región vertical); clic fuera.

**Renderizado (`RenderizadorOdontograma`)** por capas, sobre `GraphicsContext`:
1. Fondo, recuadros vacíos y números FDI.
2. Superficies: relleno con `PaletaOdontograma.color(hallazgo.color())` o blanco; `CONTORNO_SUPERFICIE` dibuja solo el borde grueso.
3. Contornos de la corona y la zona radicular.
4. Símbolos de pieza: `ASPA` (dos diagonales sobre corona y raíz), `CIRCUNFERENCIA` (círculo que envuelve la corona), `LINEA_RAIZ` (línea vertical en la raíz), `FLECHA_*`, `LINEA_FRACTURA`, `RAIZ_TRIANGULO`, `ZIGZAG`.
5. Rangos: `PUENTE` (línea horizontal con topes sobre las coronas del rango), `LLAVE`, `PARENTESIS` entre dos piezas.
6. Siglas en el recuadro, coloreadas según el hallazgo.
7. Superposiciones: hover (superficie con relleno semitransparente), selección pendiente (borde discontinuo), diferencias con el inicial (marco ámbar alrededor de la pieza), piezas de solo lectura atenuadas.

`OdontogramaCanvas extends Region` contiene un `Canvas` cuyo ancho/alto se enlazan al tamaño de la región; marca `sucio = true` ante cualquier cambio del ViewModel o del tamaño y redibuja una sola vez por pulso con `AnimationTimer` (evita redibujar decenas de veces seguidas al redimensionar).

**ViewModel (`OdontogramaViewModel`)**:
```java
ObjectProperty<OdontogramaVista> vista;                // estado proyectado + metadatos (borrador/cerrado)
ObjectProperty<HallazgoTipoDto> herramienta;           // null = modo selección
ObjectProperty<EstadoHallazgo> estadoActivo;           // BUENO / MALO
ObjectProperty<Integer> piezaSeleccionada;
ObjectProperty<Integer> piezaHover;  ObjectProperty<Superficie> superficieHover;
SetProperty<Superficie> superficiesPendientes;         // selección multi-superficie aún no registrada
ObjectProperty<Integer> inicioRango;                   // primer clic de ENTRE/RANGO
BooleanProperty soloLectura;                           // CERRADO o usuario sin permiso
BooleanProperty modoComparar;  ObjectProperty<DiferenciaVista> diferencia;
ObjectProperty<LocalDateTime> fechaVista;              // línea de tiempo
ObservableList<HallazgoDto> hallazgosPieza;            // inspector
```

**Interacción (máquina de estados del clic):**
| Herramienta | Clic | Efecto |
|---|---|---|
| ninguna | sobre pieza | selecciona pieza → inspector lista sus hallazgos (vigentes, reemplazados y anulados) |
| representación SUPERFICIE | sobre superficie | alterna la superficie en `superficiesPendientes` (solo en la misma pieza; si cambia de pieza, se descarta la selección previa con aviso). `Enter` o botón **Registrar** → un hallazgo con el bitmask. `Esc` limpia. |
| PIEZA | sobre pieza | si el tipo tiene `siglas_opciones` → `ChoiceDialog`; registra directamente |
| ENTRE_PIEZAS / RANGO_PIEZAS | 1.º clic | fija `inicioRango` (pieza resaltada) |
| | 2.º clic | valida misma arcada y registra |
Cada registro: `TareaUi.ejecutar(() -> servicio.registrarHallazgo(cmd), vista::set)`. El servicio devuelve la vista recalculada, así que el canvas siempre refleja lo que quedó en la BD.

**Exportar a PDF**: en el hilo de UI se crea un `Canvas` fuera de pantalla de 1600×900, se dibuja con el mismo `RenderizadorOdontograma` y se toma `snapshot` → `SwingFXUtils.fromFXImage` → `byte[]` PNG. Luego, en segundo plano, `FichaOdontologicaPdf.generar(datosFicha, png)` arma el PDF (datos del paciente, imagen, tabla de hallazgos vigentes, especificaciones, observaciones, CPO-D/ceo-d, hash de cierre y línea de firma del odontólogo con su COP).

### 5.2 Fórmulas e índices internos

#### 5.2.1 CPO-D y ceo-d (`CalculadoraIndices`)
Se calculan **por pieza** sobre el estado vigente (cada pieza aporta como máximo 1 a un solo componente):

```
CPO-D (dentición permanente; excluye terceras molares si la configuración lo indica)
para cada pieza permanente p:
    H = hallazgos vigentes de p
    si ∃ h ∈ H con conteo_cpod = 'C'                                → C += 1
    si no, si ∃ h ∈ H con conteo_cpod = 'O' y color = ROJO          → C += 1   (restauración en mal estado)
    si no, si ∃ h ∈ H con conteo_cpod = 'P'                          → P += 1
    si no, si ∃ h ∈ H con conteo_cpod = 'O'                          → O += 1
CPO-D = C + P + O

ceo-d (dentición decidua, piezas 51–85): análogo con c, e, o (conteo_ceod)
ceo-d = c + e + o
```
Precedencia `C > P > O`: una pieza con restauración y caries nueva cuenta como cariada.

Clasificación OMS del CPO-D (`NivelSeveridadOms`), pensada para la edad índice de 12 años:
| Nivel | CPO-D |
|---|---|
| MUY_BAJO | 0,0 – 1,1 |
| BAJO | 1,2 – 2,6 |
| MODERADO | 2,7 – 4,4 |
| ALTO | 4,5 – 6,5 |
| MUY_ALTO | ≥ 6,6 |
En el informe individual se muestra el valor y el nivel con la nota “referencia OMS a los 12 años”. En el reporte epidemiológico (RF-REP-05) se promedia por grupo etario: `CPO-D promedio = Σ CPO-D / N pacientes del grupo`.

```java
public record ResultadoIndice(int c, int p, int o, int total, int piezasEvaluadas, NivelSeveridadOms nivel) {}
public final class CalculadoraIndices {
    public ResultadoIndice cpod(EstadoOdontograma estado, boolean excluirTercerasMolares);
    public ResultadoIndice ceod(EstadoOdontograma estado);
}
```

#### 5.2.2 Motor de sugerencias (`MotorSugerencias`)
```
entrada: estado vigente, reglas activas ordenadas por prioridad, tarifario
salida: List<ItemSugerido>(procedimiento, pieza, superficies, fase, cantidad, precioUnit, origenHallazgoId)

1. Reglas generales (hallazgo_tipo_id NULL) → un ítem cada una, sin pieza.
2. Para cada hallazgo vigente h (no anulado):
     candidatas = reglas donde tipo = h.tipo
                  ∧ (regla.estado = NULL ∨ regla.estado = h.estado)
                  ∧ (regla.tipoRadicular = NULL ∨ regla.tipoRadicular = h.pieza.tipoRadicular())
                  ∧ regla.min ≤ |h.superficies| ≤ regla.max
     tomar la PRIMERA candidata (menor prioridad)   → ítem
3. Omitir hallazgos sin tratamiento pendiente (p. ej. restauración BUENO no tiene regla).
4. Desduplicar por (procedimiento, pieza): si dos hallazgos generan lo mismo en la misma pieza, queda uno.
5. Precio por forma de cobro:
     PIEZA → precio ; SUPERFICIE → precio × |superficies| ; BOCA, ARCADA, SESION → precio (cantidad 1)
6. Ordenar por fase, luego por número de pieza en orden de dibujo.
```

#### 5.2.3 Totales y cronograma de cuotas (todo en céntimos `long`)
```
total_item = cantidad × precio_unit − descuento_item          (descuento_item ≤ cantidad × precio_unit)
subtotal   = Σ total_item (ítems no CANCELADOS)
total      = subtotal − descuento_global                      (descuento_global ≤ subtotal)
```
```java
public final class GeneradorCronograma {
    /**
     * @param totalCent      total del plan
     * @param inicialCent    cuota inicial (0..total); si > 0 se genera la cuota N° 0 con vencimiento = hoy
     * @param numeroCuotas   n ≥ 1 (si inicial == total, n debe ser 0)
     */
    public List<CuotaProgramada> generar(long totalCent, long inicialCent, int numeroCuotas,
                                         LocalDate primerVencimiento, Frecuencia frecuencia, LocalDate hoy);
}
```
Fórmula de reparto exacto:
```
R = total − inicial
base    = R / n          (división entera)
residuo = R mod n        (0 ≤ residuo < n)
cuota_i = base + (i ≤ residuo ? 1 : 0)        para i = 1..n
⇒ Σ cuota_i = R exactamente (los céntimos sobrantes se reparten en las primeras cuotas)

vencimiento_i:
  MENSUAL   → primerVencimiento.plusMonths(i − 1)   (Java ajusta 31 → último día del mes)
  QUINCENAL → primerVencimiento.plusDays(15·(i − 1))
  SEMANAL   → primerVencimiento.plusWeeks(i − 1)
  si cae domingo → se mueve al lunes siguiente
```
Test obligatorio: `total = 100000` (S/ 1 000.00), `inicial = 0`, `n = 3` → `33334, 33333, 33333`.

Regenerar el cronograma solo está permitido si ninguna cuota tiene pagos.

#### 5.2.4 Aplicación de pagos FIFO (`AsignadorPagosFifo`)
```java
public List<Aplicacion> asignar(long montoCent, List<SaldoCuota> pendientes);
```
```
ordenar pendientes por (vencimiento ASC, numero ASC)
restante = monto
para cada cuota c mientras restante > 0:
    aplicar = min(restante, c.saldo)
    registrar Aplicacion(c.id, aplicar); restante −= aplicar
si restante > 0 → NegocioException("El monto excede el saldo pendiente en S/ x.xx")
```
`PagoService.registrar(PagoForm)` en una transacción: genera el número de recibo con `CorrelativoService.siguiente("RECIBO")`, inserta `pago`, inserta `pago_aplicacion`, suma `pagado_cent` en cada cuota, cambia el plan a `EN_CURSO` si estaba `ACEPTADO`, audita y publica `PagoRegistrado`. `PagoService.vistaPrevia(...)` ejecuta solo el asignador para mostrar el reparto antes de confirmar.

Anulación: marca `anulado = 1`, resta las aplicaciones de cada cuota y conserva las filas de `pago_aplicacion` para trazabilidad.

Estado de una cuota (derivado, no almacenado): `PAGADA` si `pagado = monto`; `VENCIDA` si `vencimiento < hoy` y `pagado < monto`; `PARCIAL` si `0 < pagado < monto`; si no, `PENDIENTE`.

`CorrelativoService.siguiente(serie)`: `UPDATE correlativo SET ultimo = ultimo + 1 WHERE serie = ?` seguido de `SELECT ultimo`, en la misma transacción. Con una sola conexión no hay carrera posible.

#### 5.2.5 Agenda: solapamiento y siguiente hueco
Solapamiento de intervalos semiabiertos `[inicio, fin)`:
```
solapan(a, b) ⇔ a.inicio < b.fin ∧ b.inicio < a.fin
```
```java
// CitaRepository (JPQL); fechas comparadas como texto ISO gracias al converter
@Query("""
  SELECT COUNT(c) > 0 FROM Cita c
  WHERE c.odontologo.id = :odontologoId AND c.estado NOT IN ('CANCELADA','NO_ASISTIO')
    AND c.inicio < :fin AND c.fin > :inicio AND (:excluirId IS NULL OR c.id <> :excluirId)""")
boolean existeSolapamientoOdontologo(Long odontologoId, LocalDateTime inicio, LocalDateTime fin, Long excluirId);
// análogo existeSolapamientoPaciente(...)
```
Además, la cita debe quedar **dentro** de un bloque de `horario_atencion` del odontólogo para ese día.

```java
public final class BuscadorHuecos {
    public Optional<Intervalo> siguienteHueco(LocalDateTime desde, Duration duracion, Duration paso,
                                              List<BloqueHorario> horario, List<Intervalo> ocupados, int diasMaximos);
}
```
```
para cada día d desde fecha(desde) hasta fecha(desde) + diasMaximos:
    para cada bloque B del horario cuyo día de semana = d:
        cursor = max(B.inicio en d, redondearArriba(desde, paso))
        para cada ocupado o del día dentro de B, ordenados por inicio:
            si o.inicio − cursor ≥ duracion → devolver [cursor, cursor + duracion)
            cursor = max(cursor, redondearArriba(o.fin, paso))
        si B.fin − cursor ≥ duracion → devolver [cursor, cursor + duracion)
devolver vacío
```
Complejidad `O(D · k log k)` con `k` citas por día.

#### 5.2.6 Recordatorios
`RecordatorioScheduler` (`ScheduledExecutorService`, cada 15 min, primer intento 1 min después del arranque):
1. Si `recordatorio.habilitado = false` o falta SMTP → no hace nada.
2. Si `ConectividadService.hayInternet()` es falso → reintenta en el siguiente ciclo.
3. Busca citas `PROGRAMADA/CONFIRMADA` con `inicio` entre ahora y ahora + `horas_antes`, sin `recordatorio_enviado_en`, de pacientes con email y `acepta_recordatorios = 1`.
4. Envía con `SmtpCorreoGateway` (crea `JavaMailSenderImpl` con la configuración actual en cada envío), registra `notificacion_log` y fija `recordatorio_enviado_en`. Un fallo no detiene el lote.

#### 5.2.7 Consulta de DNI
```java
public interface ConsultaDniGateway { Optional<DatosPersona> consultar(String dni) throws IntegracionException; }
```
`HttpConsultaDniGateway`: `java.net.http.HttpClient` con timeout de `api.timeout_seg`; URL = plantilla con `{numero}` reemplazado; cabecera = `cabecera_auth: prefijo_token + token`; respuesta parseada con Gson a `JsonObject` y leída con `api.dni.mapeo_json`. HTTP 404 → `Optional.empty()`; otros errores → `IntegracionException` con mensaje amigable. Botón “Probar conexión” en Configuración consulta un DNI de prueba que el usuario escribe.

### 5.3 Concurrencia en JavaFX (`TareaUi`)
```java
@Component @RequiredArgsConstructor
public class TareaUi {
    private final @Qualifier("ejecutorUi") ExecutorService ejecutor;   // 2 hilos daemon "odontogest-bg-%d"
    private final Notificador notificador;

    public <T> Task<T> ejecutar(Callable<T> trabajo, Consumer<T> alExito);
    public <T> Task<T> ejecutar(Callable<T> trabajo, Consumer<T> alExito, Consumer<Throwable> alFallar);
    /** Deshabilita 'origen' y muestra ProgressIndicator mientras corre. */
    public <T> Task<T> ejecutar(Node origen, Callable<T> trabajo, Consumer<T> alExito);
}
```
- `setOnSucceeded`/`setOnFailed` ya corren en el hilo de UI.
- `NegocioException` / `PermisoDenegadoException` → `notificador.advertencia(e.getMessage())`; `IntegracionException` → `notificador.info(...)`; otras → `notificador.error("Ocurrió un error inesperado. Revise el registro.")` + `log.error`.
- Como el pool de BD tiene 1 conexión, las tareas que tocan BD se serializan en Hikari; los 2 hilos permiten que una exportación a PDF o una llamada HTTP no bloquee las consultas.
- Prohibido `Platform.runLater` dentro de servicios: los servicios no conocen JavaFX.

### 5.4 Respaldo y restauración
- `RespaldoService.crear(Path destinoZip)`: ejecuta `VACUUM INTO '<tmp>/odontogest.db'` (copia consistente aunque haya WAL; escapar comillas simples en la ruta) y comprime ese archivo + `adjuntos/` + `clave.key` en un ZIP con `manifest.json` (versión, fecha, esquema).
- `RespaldoService.programarRestauracion(Path zip)`: valida el ZIP, extrae en `${home}/restauracion/` y crea la marca `odontogest.db.restaurar`; pide reiniciar la aplicación. `RestauracionPendiente` (en `Launcher`, antes de Spring) mueve la BD actual a `respaldos/antes-de-restaurar-<fecha>.db` y coloca la restaurada.
- Respaldo automático al cerrar (`stop()`), conservando los últimos `respaldo.mantener`.

### 5.5 Auditoría
`AuditoriaService.registrar(String accion, String entidad, Long id, Object detalle)` con `@Transactional(propagation = MANDATORY)`: se llama dentro de la transacción del servicio que hace el cambio, así la auditoría no queda huérfana si la operación falla.

### 5.6 Matriz de permisos (`Permiso`)
| Permiso | ADMIN | ODONTOLOGO | RECEPCION |
|---|---|---|---|
| PACIENTE_VER / PACIENTE_EDITAR | ✔ / ✔ | ✔ / ✔ | ✔ / ✔ |
| ANAMNESIS_EDITAR | ✔ | ✔ | — |
| ATENCION_GESTIONAR | ✔ | ✔ | — |
| ODONTOGRAMA_VER / ODONTOGRAMA_EDITAR | ✔ / ✔ | ✔ / ✔ | ✔ / — |
| PLAN_EDITAR | ✔ | ✔ | — |
| PAGO_REGISTRAR / PAGO_ANULAR | ✔ / ✔ | — / — | ✔ / — |
| AGENDA_GESTIONAR | ✔ | ✔ | ✔ |
| REPORTES_VER | ✔ | ✔ (solo su producción) | ✔ (caja) |
| CATALOGOS_EDITAR / USUARIOS / CONFIGURACION / RESPALDO | ✔ | — | — |

`SesionUsuario.exigir(Permiso p)` lanza `PermisoDenegadoException`; cada método de servicio que escribe lo invoca. La UI además oculta o deshabilita los controles (`SesionUsuario.tiene(p)`).

---

## 6. Mapa de pantallas (wireframes conceptuales JavaFX)

### 6.1 Ventanas y vistas
| FXML | Controlador | Raíz y componentes clave | Datos/servicios |
|---|---|---|---|
| `login-view.fxml` | `LoginController` | `StackPane` → tarjeta `VBox` con logo, `TextField` usuario, `PasswordField`, `Label` de error, `Button` (default), `Hyperlink` “Acerca de”. Texto pequeño con credenciales demo si `app.seeder.enabled`. | `AutenticacionService.iniciarSesion` → `SesionIniciada` |
| `main-view.fxml` | `MainController` | Igual al del docente: `StackPane raiz` → `BorderPane` con `VBox sidebar` (`ToggleGroup` de `ToggleButton`, indicador animado, botones tema/contraer), barra superior `HBox` con buscador global de pacientes (`CustomTextField` + `Autocompletar`), chip del paciente activo, usuario y botón cerrar sesión; `StackPane contenido`; `ModalPane` panel lateral y diálogo; `VBox` zona de toasts. | `CargadorVistas`, `SesionUsuario` |
| `dashboard-view.fxml` | `DashboardController` (`Refrescable`) | `ScrollPane` → `FlowPane` de 4 `TarjetaKpi`; `BarChart<String,Number>` ingresos 6 meses; `ListView<CitaDto>` citas de hoy (celda con hora, paciente, estado); `TableView` cuotas vencidas (top 10). | `DashboardService` (vía `DashboardDao`) |
| `agenda-view.fxml` | `AgendaController` + `AgendaViewModel` | `BorderPane`: arriba `ToolBar` con `DatePicker`, botones semana anterior/siguiente/hoy, `ComboBox<OdontologoDto>`, `ToggleButton` día/semana, botón “Nueva cita”, botón “Siguiente hueco”. Centro: `ScrollPane` → `AgendaSemanal` (Region propia: columnas por día, filas cada 15 min, bloques fuera de horario sombreados; citas como `StackPane` posicionados con color del odontólogo y estado; arrastrar para mover con validación). Doble clic en hueco → formulario prellenado. | `CitaService`, `OdontologoService` |
| `cita-form-view.fxml` | `CitaFormController` | Panel lateral (`ModalPane`): `Autocompletar` paciente, `ComboBox` odontólogo, `DatePicker` + `ComboBox<LocalTime>` (pasos de 15 min), `Spinner<Integer>` duración, `TextField` motivo, `ComboBox` ítem de plan pendiente (opcional), `TextArea` notas, estado (`ChoiceBox`); botones Guardar, “Atender ahora”, Cancelar cita. | `CitaService.programar/mover/cancelar` |
| `pacientes-view.fxml` | `PacientesController` | `VBox`: `ToolBar` con `CustomTextField` búsqueda (debounce 300 ms con `PauseTransition`), filtro activo/inactivo, “Nuevo paciente”; `TableView<PacienteResumen>` (HC, documento, nombre, edad, teléfono, última atención, saldo, etiqueta demo); `Paginador`. Formulario en panel lateral con `GridPane`, botón “Consultar DNI” (deshabilitado si la API no está habilitada), sección apoderado visible solo si menor de edad. Doble clic → ficha. | `PacienteService`, `ConsultaDniGateway` |
| `paciente-ficha-view.fxml` | `PacienteFichaController` | `BorderPane`: arriba cabecera `HBox` (avatar con iniciales, nombre, HC, edad, documento, **alerta roja de alergias**, saldo, botones “Nueva atención” y “Nueva cita”); centro `TabPane` con `fx:include` de cada pestaña. | `PacienteActivo` |
| ↳ `paciente-datos-tab.fxml` | `DatosPacienteController` | `GridPane` editable, botones Guardar y “Consentimiento de datos (PDF)”. | `PacienteService`, `ConsentimientoService` |
| ↳ `paciente-anamnesis-tab.fxml` | `AnamnesisController` | `GridPane` con `TextArea`s, `FlowPane` de `CheckBox` de enfermedades, `TextField` presión arterial, `CheckBox` fuma/gestante. | `AnamnesisService` |
| ↳ `paciente-odontograma-tab.fxml` | `OdontogramaController` + `OdontogramaViewModel` | Ver §6.2. | `OdontogramaService`, `CatalogoHallazgoService` |
| ↳ `paciente-plan-tab.fxml` | `PlanTratamientoController` | `SplitPane` vertical. Arriba: `ToolBar` (`ComboBox` de planes del paciente, “Nuevo plan”, “Sugerir desde odontograma”, “Agregar procedimiento”, estado del plan con botones de transición, “Presupuesto PDF”); `TreeTableView<PlanItemFila>` agrupado por fase (columnas: pieza, superficies, procedimiento, cantidad, precio, descuento editable, total, estado). Abajo: totales (`Label`s), descuento global, y `TitledPane` “Cronograma”: `Spinner` N cuotas, `TextField` inicial, `DatePicker` primer vencimiento, `ChoiceBox<Frecuencia>`, “Generar”, `TableView<CuotaDto>` con estado coloreado. | `PlanTratamientoService` |
| ↳ `paciente-atenciones-tab.fxml` | `AtencionesController` | `SplitPane` horizontal: `ListView<AtencionResumen>` (línea de tiempo) / detalle con `TextArea`s (motivo, examen, diagnóstico, observaciones), `ListView<CheckBox>` de ítems del plan pendientes para marcar realizados, botones “Abrir odontograma de esta atención” y “Cerrar atención”. | `AtencionService` |
| ↳ `paciente-pagos-tab.fxml` | `PagosPacienteController` | Resumen (total, pagado, saldo, vencido); `TableView<CuotaDto>`; `TableView<PagoDto>` con menú contextual Recibo PDF / Anular. | `PagoService` |
| ↳ `paciente-adjuntos-tab.fxml` | `AdjuntosController` | `TilePane` de miniaturas (`ImageView`) en `ScrollPane`; soporte arrastrar y soltar archivos; doble clic abre con la app del sistema (`HostServices`). | `AdjuntoService` |
| `caja-view.fxml` | `CajaController` (`Refrescable`) | Izquierda: formulario de cobro (`Autocompletar` paciente, `TableView` cuotas pendientes, `TextField` monto, `ChoiceBox<MedioPago>`, referencia, **vista previa FIFO** en `TableView` que se actualiza al escribir el monto, botón Cobrar → diálogo de éxito con “Imprimir recibo”). Derecha: `DatePicker` + `TableView<PagoDto>` del día y totales por medio. | `PagoService` |
| `reportes-view.fxml` | `ReportesController` | `TabPane`: Ingresos (rango de fechas, `BarChart`, tabla, “Exportar Excel”), Producción por odontólogo, Morosidad, Epidemiológico (CPO-D/ceo-d por grupo etario con `BarChart`). | `ReporteService` |
| `catalogos-view.fxml` | `CatalogosController` | `TabPane`: Procedimientos (`TableView` editable), Hallazgos (`TableView` + vista previa del símbolo dibujado en un `Canvas` pequeño), Reglas de sugerencia, Odontólogos y horarios. | servicios de catálogo |
| `usuarios-view.fxml` | `UsuariosController` | `TableView<UsuarioDto>` + panel lateral (rol, odontólogo vinculado, activo, restablecer contraseña). | `UsuarioService` |
| `configuracion-view.fxml` | `ConfiguracionController` | `Accordion` de `TitledPane` por grupo: Clínica (logo con `FileChooser`), Agenda, Recordatorios, Correo (con “Enviar correo de prueba”), API DNI (con “Probar conexión”), Respaldo (“Crear respaldo ahora”, “Restaurar…”, lista de respaldos), Odontograma, Interfaz. Los campos `SECRETO` son `PasswordField`. | `ConfiguracionService`, `RespaldoService` |
| `acerca-de-view.fxml` | `AcercaDeController` | Diálogo: logo, nombre, versión (`BuildProperties`), autores, curso, año, aviso de registro, rutas de datos y log, botón “Verificar integridad de la base de datos” (`PRAGMA integrity_check`). | — |

### 6.2 Wireframe del odontograma (pantalla crítica)
```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│ ToolBar: [ComboBox: Inicial 12/03/2026 ▾ | Evolución 05/10/2026 | Actual (borrador)]  │
│          Dentición (◉Perm ○Dec ○Mixta)  [⇆ Comparar con inicial]  [Cerrar y firmar]   │
│          [PDF]   Slider línea de tiempo ───●──────  Estado: BORRADOR                   │
├───────────────┬───────────────────────────────────────────────────┬──────────────────┤
│ Paleta        │                                                   │ Inspector        │
│ (Accordion)   │   [ ][ ][ ][ ][ ][ ][ ][ ] │ [ ][ ][ ][ ][ ]...   │ Pieza 36         │
│ ▸ Superficie  │    ⋀  ⋀  ⋀  ⋀  ⋀  ⋀  ⋀  ⋀  │  ⋀  ⋀  ...           │ Molar · multirr. │
│   ● Caries    │   ⊠  ⊠  ⊠  ⊠  ⊠  ⊠  ⊠  ⊠  │ ⊠  ⊠  ...            │ ───────────────  │
│   ● Restaur.  │   18 17 16 15 14 13 12 11  │ 21 22 ...            │ Vigentes:        │
│ ▸ Pieza       │        55 54 53 52 51      │ 61 62 ...            │ • Caries M,O (R) │
│ ▸ Rango       │        85 84 83 82 81      │ 71 72 ...            │ Reemplazados:    │
│ ───────────── │   48 47 46 45 44 43 42 41  │ 31 32 ...            │ • ...            │
│ Estado:       │   ⊠ ...  (espejo inferior)                         │ Anulados:        │
│ (◉Bueno ○Malo)│                                                   │ • ... (motivo)   │
│ [Registrar ⏎] │          OdontogramaCanvas (Region + Canvas)      │ [Anular…]        │
├───────────────┴───────────────────────────────────────────────────┴──────────────────┤
│ Especificaciones [TextArea]          Observaciones [TextArea]                         │
│ CPO-D: C=3 P=1 O=2 → 6 (ALTO)   ceo-d: —      Hash: 9f3a…c21 ✔                        │
└──────────────────────────────────────────────────────────────────────────────────────┘
```
Componentes: `BorderPane`; arriba `ToolBar` (`ComboBox<OdontogramaResumen>`, `ToggleGroup` de `RadioButton`, `ToggleButton`, `Button`, `Slider` con `StringConverter` de fechas); izquierda `Accordion` de `TitledPane` con `ListView<HallazgoTipoDto>` (celda con muestra de color/símbolo dibujada en un `Canvas` de 20×20) y `ToggleGroup` Bueno/Malo; centro `OdontogramaCanvas` dentro de un `StackPane` (con `Label` flotante de *tooltip* que muestra “Pieza 36 · Mesial”); derecha `VBox` inspector con `ListView`s; abajo `HBox` con dos `TextArea` y `Label`s de índices.

Atajos: `Enter` registra la selección pendiente; `Esc` cancela la herramienta; `1`–`9` eligen las primeras herramientas de la paleta; `B`/`M` cambian el estado Bueno/Malo.

### 6.3 Comunicación entre controladores
1. **Creación**: todos los controladores son `@Component` singleton (como en el repo del docente). `CargadorVistas` usa `setControllerFactory(context::getBean)`; los `fx:include` de la ficha heredan esa fábrica, así que las pestañas también se inyectan.
2. **Estado compartido**:
   - `SesionUsuario` (usuario, rol, permisos) — consultado por `MainController` para mostrar/ocultar menú.
   - `PacienteActivo` con `ReadOnlyObjectProperty<PacienteResumen> paciente`: `PacienteFichaController` lo fija; cada controlador de pestaña añade un `ChangeListener` en su `initialize()` y recarga sus datos.
3. **Navegación** desacoplada con eventos de Spring:
   ```java
   public record NavegarA(String vista, Map<String, Object> parametros) {}
   // cualquier controlador:
   eventos.publishEvent(new NavegarA("paciente-ficha-view", Map.of("pacienteId", id, "pestana", "odontograma")));
   // MainController:
   @EventListener void alNavegar(NavegarA e) { mostrar(e.vista()); ... }
   ```
   Los eventos de Spring son síncronos: si se publican desde el hilo de UI, el listener corre en el hilo de UI. Los servicios publican eventos de dominio (`PagoRegistrado`, `CitaCambiada`, `OdontogramaCerrado`) desde hilos de fondo, así que los **listeners de UI deben envolver su trabajo en `Platform.runLater`** (`@EventListener` en el controlador → `Platform.runLater(this::refrescar)`).
4. **Refresco al entrar**: vistas que implementan `Refrescable` se recargan cada vez que se muestran (patrón del docente).
5. **Formularios en panel lateral**: `Notificador.abrirPanel(nodo)` y `cerrarPanel()`; el controlador del formulario recibe un `Consumer<Long> alGuardar` para avisar al listado que lo abrió.

Flujo típico de atención:
```
Agenda (doble clic en cita) → CitaFormController "Atender ahora"
  → AtencionService.abrirDesdeCita(citaId)                     [TareaUi]
  → publish NavegarA("paciente-ficha-view", {pacienteId, pestana:"odontograma"})
  → MainController muestra la ficha → PacienteActivo.set(...)
  → OdontogramaController carga la vista con el borrador recién abierto
  → el odontólogo registra hallazgos → cierra y firma
  → PlanTratamientoController "Sugerir desde odontograma"
  → AtencionesController marca ítems realizados y cierra la atención
  → CitaService marca la cita ATENDIDA (CitaCambiada → agenda y dashboard se refrescan)
```

---

## 7. Plan de desarrollo por fases (para Claude Code)

Cada fase termina con `mvn test` en verde, la app ejecutándose con `mvn javafx:run` y una pausa para revisión del estudiante.

| Fase | Entregable | Criterios de aceptación |
|---|---|---|
| **F0 — Esqueleto** | `pom.xml`, `Launcher`, `JavaFxApplication`, `OdontoGestApplication`, `DirectorioDatos`, `application.properties`, `schema.sql` completo, conversores, `EjecutorConfig`, `TareaUi`, `Notificador`, `Refrescable`, `CargadorVistas`, `main-view` con menú vacío, `login-view`, `AutenticacionService`, `ConfiguracionSeeder`, usuario admin mínimo, `ArquitecturaTest`, `ConversoresIntegracionTest`. | La app crea `~/OdontoGest/odontogest.db` con todas las tablas; login con `admin`; tema claro/oscuro; fechas y céntimos se guardan como TEXT/INTEGER esperados. |
| **F1 — Dominio puro** | Todo `dominio/` con tests: `PiezaDental`, `Superficie(s)`, `ProyectorOdontograma`, `DiferenciaOdontograma`, `CalculadoraIndices`, `CadenaIntegridad`, `MotorSugerencias`, `GeneradorCronograma`, `AsignadorPagosFifo`, `BuscadorHuecos`, `NumeroALetras`; `DetectorSuperficie` y `LayoutOdontograma` con tests. | Cobertura ≥ 70 % en `dominio`; casos límite de §5 cubiertos (reparto 33334/33333/33333, precedencia C>P>O, desempate de diagonales, capas de exclusión, hueco al final del bloque). |
| **F2 — Pacientes y catálogos** | Entidades, repositorios y servicios de paciente, anamnesis, consentimiento, odontólogo, horarios, procedimientos, hallazgos, reglas; `CatalogoSeeder`; vistas `pacientes-view`, ficha con pestañas Datos y Anamnesis; `catalogos-view`; `usuarios-view`; permisos. | CRUD completo con validaciones; búsqueda paginada; HC correlativa; alerta de alergias; PDF de consentimiento de datos. |
| **F3 — Odontograma** | Atención (abrir/cerrar), `OdontogramaService`, `IntegridadService`, `OdontogramaCanvas`, `RenderizadorOdontograma`, ViewModel, pestaña completa (§6.2), PDF de ficha odontológica. | Registrar hallazgos de los 4 tipos de representación; cerrar con hash; anular con motivo; línea de tiempo y comparación; CPO-D/ceo-d en vivo; hover fluido; PDF legible. **Antes de empezar: el estudiante valida el catálogo contra la NTS 188.** |
| **F4 — Plan de tratamiento** | `PlanTratamientoService`, pestaña Plan, cronograma, presupuesto PDF, consentimiento de tratamiento. | Sugerencias coherentes con el odontograma demo; totales exactos; transiciones de estado válidas; cronograma bloqueado si hay pagos. |
| **F5 — Caja** | `PagoService`, `CorrelativoService`, `caja-view`, pestaña Pagos, recibo PDF con monto en letras, anulación. | Vista previa FIFO; saldos correctos tras pagar y anular; recibos correlativos sin saltos. |
| **F6 — Agenda** | `CitaService`, `AgendaSemanal`, `agenda-view`, formulario de cita, “Siguiente hueco”, flujo cita → atención. | No se pueden crear solapamientos; arrastrar mueve con validación; hueco sugerido correcto. |
| **F7 — Integraciones** | `configuracion-view`, `CifradorSecretos`, `HttpConsultaDniGateway`, `SmtpCorreoGateway`, `RecordatorioScheduler`, `ConectividadService`. | Secretos cifrados en BD (`enc:v1:`); sin internet la app funciona y muestra aviso; “Probar conexión” y “Correo de prueba” funcionan con credenciales válidas. |
| **F8 — Reportes, auditoría y respaldo** | `DashboardDao`, `ReporteDao`, `dashboard-view`, `reportes-view` con Excel, `AuditoriaService` integrado, `RespaldoService`, `RestauracionPendiente`, `acerca-de-view`. | Dashboard con datos reales; Excel abre en Office/LibreOffice; respaldo ZIP y restauración tras reinicio verificados. |
| **F9 — Datos demo, empaquetado y documentación** | `DatabaseSeeder` completo (§3.5), `README.md`, `docs/manual-usuario.md` con capturas, cabeceras de autoría, perfil `empaquetar` probado en Windows, ZIP final. | En una PC Windows **sin Java**: descomprimir, abrir `OdontoGest.exe`, iniciar sesión con `admin` y recorrer todos los módulos con datos demo. |

### 7.1 Cabecera de autoría (todos los `.java`)
```java
/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
```

### 7.2 Entregables recomendados para el registro
- ZIP del app-image para Windows + `README.md` con requisitos (Windows 10/11 x64), pasos de ejecución y credenciales demo.
- Código fuente completo y este documento de arquitectura.
- Manual de usuario con capturas de cada módulo.
- Descripción de los elementos originales (§0.4).
- ⚠️ Confirmar en INDECOPI los requisitos vigentes de presentación para registro de software antes de preparar el expediente.

### 7.3 Riesgos y mitigación
| Riesgo | Mitigación |
|---|---|
| Hibernate no aplica conversores `java.time` con `autoApply` | Test de F0 lo detecta; usar `@Convert` explícito. |
| Natives de JavaFX del SO equivocado en el build final | Construir el paquete en Windows (§4). |
| Módulo JDK faltante en el app-image | Ampliar `--add-modules`; probar el ZIP en una PC limpia. |
| Catálogo de hallazgos no conforme a la NTS 188 | Validación del estudiante antes de F3; todo es dato, no código. |
| API de DNI de terceros cambia o deja de funcionar | Integración opcional y configurable; ingreso manual siempre disponible. |
| Alcance excesivo para el ciclo | Priorizar requisitos **M** (F0–F6 + F9). F7 y F8 se recortan a lo M si falta tiempo. |
