/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Reglas de dependencia entre capas (PLAN §2.1, RNF-10).
 */
@AnalyzeClasses(packages = "com.odontogest", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    /*
     * La UI sí puede usar los enums de modelo (TipoDocumento, Sexo, Rol...), que son valores, no entidades:
     * lo prohibido son las entidades JPA (riesgo de LazyInitializationException) y los repositorios.
     */
    @ArchTest
    static final ArchRule ui_no_usa_persistencia = noClasses()
            .that().resideInAPackage("com.odontogest.ui..")
            .should().dependOnClassesThat().resideInAPackage("com.odontogest.persistencia..")
            .because("los controladores pasan siempre por los servicios");

    @ArchTest
    static final ArchRule ui_no_usa_entidades = noClasses()
            .that().resideInAPackage("com.odontogest.ui..")
            .should().dependOnClassesThat().areAnnotatedWith(Entity.class)
            .because("los controladores solo intercambian DTO record con los servicios");

    @ArchTest
    static final ArchRule dominio_es_java_puro = classes()
            .that().resideInAPackage("com.odontogest.dominio..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("com.odontogest.dominio..", "java..")
            .because("el dominio no conoce JavaFX, Spring ni JPA");

    @ArchTest
    static final ArchRule persistencia_no_depende_de_capas_superiores = noClasses()
            .that().resideInAPackage("com.odontogest.persistencia..")
            .should().dependOnClassesThat().resideInAnyPackage("com.odontogest.ui..", "com.odontogest.servicio..");

    @ArchTest
    static final ArchRule servicio_no_depende_de_ui = noClasses()
            .that().resideInAPackage("com.odontogest.servicio..")
            .should().dependOnClassesThat().resideInAPackage("com.odontogest.ui..");

    @ArchTest
    static final ArchRule dto_no_expone_entidades = noClasses()
            .that().resideInAPackage("com.odontogest.servicio.dto..")
            .should().dependOnClassesThat().areAnnotatedWith(Entity.class);

    @ArchTest
    static final ArchRule entidades_en_modelo = classes()
            .that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage("com.odontogest.modelo..");

    @ArchTest
    static final ArchRule documentos_solo_usan_dto = noClasses()
            .that().resideInAPackage("com.odontogest.documento..")
            .should().dependOnClassesThat().resideInAnyPackage("com.odontogest.persistencia..", "com.odontogest.ui..")
            .orShould().dependOnClassesThat().areAnnotatedWith(Entity.class);

    @ArchTest
    static final ArchRule backend_no_usa_javafx = noClasses()
            .that().resideInAnyPackage("com.odontogest.servicio..", "com.odontogest.persistencia..",
                    "com.odontogest.modelo..", "com.odontogest.seguridad..")
            .should().dependOnClassesThat().resideInAPackage("javafx..")
            .because("los servicios no conocen JavaFX (nada de Platform.runLater en servicios)");
}
