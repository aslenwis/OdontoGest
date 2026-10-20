/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.odontogest.excepcion.NegocioException;
import com.odontogest.modelo.Configuracion;
import com.odontogest.modelo.TipoConfiguracion;
import com.odontogest.persistencia.repositorio.ConfiguracionRepository;
import com.odontogest.servicio.dto.DatosClinica;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lectura y escritura de la tabla {@code configuracion} con caché en memoria.
 * <p>
 * Fase 0: valores de texto, entero y booleano. Los métodos {@code secreto} y {@code json}
 * (con {@code CifradorSecretos} y Gson) se agregan en la Fase 7.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConfiguracionService {

    private final ConfiguracionRepository configuracionRepository;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String texto(String clave) {
        return cache.computeIfAbsent(clave, this::leer);
    }

    public int entero(String clave) {
        String valor = texto(clave);
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            throw new NegocioException("La configuración '" + clave + "' no es un número entero: " + valor);
        }
    }

    public boolean booleano(String clave) {
        return Boolean.parseBoolean(texto(clave).trim());
    }

    /** Datos de la clínica para las cabeceras de los PDF. */
    public DatosClinica datosClinica() {
        return new DatosClinica(texto("clinica.nombre"), texto("clinica.ruc"), texto("clinica.direccion"),
                texto("clinica.telefono"), texto("clinica.email"));
    }

    /**
     * Guarda varios valores en una sola transacción e invalida la caché.
     */
    @Transactional
    public void guardar(Map<String, String> valores) {
        LocalDateTime ahora = LocalDateTime.now();
        valores.forEach((clave, valor) -> {
            Configuracion c = configuracionRepository.findById(clave)
                    .orElseThrow(() -> new NegocioException("No existe la configuración '" + clave + "'."));
            if (!c.isEditable()) {
                throw new NegocioException("La configuración '" + clave + "' no es editable.");
            }
            if (c.getTipo() == TipoConfiguracion.SECRETO) {
                throw new IllegalStateException("El cifrado de secretos aún no está disponible (CifradorSecretos).");
            }
            c.setValor(valor);
            c.setActualizadoEn(ahora);
        });
        cache.clear();
    }

    private String leer(String clave) {
        return configuracionRepository.findById(clave)
                .map(c -> c.getValor() == null ? "" : c.getValor())
                .orElseThrow(() -> new NegocioException("No existe la configuración '" + clave + "'."));
    }
}
