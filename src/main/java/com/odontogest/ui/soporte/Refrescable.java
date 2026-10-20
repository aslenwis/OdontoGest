/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.soporte;

/**
 * Vista que recarga sus datos cada vez que se muestra en el área de contenido.
 * Se invoca en el hilo de UI; la carga de datos debe ir por {@link TareaUi}.
 */
public interface Refrescable {

    void refrescar();
}
