/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.controlador;

import atlantafx.base.theme.Styles;
import com.odontogest.evento.NavegarA;
import com.odontogest.evento.PagoRegistrado;
import com.odontogest.evento.PlanCambiado;
import com.odontogest.seguridad.Permiso;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.PagoService;
import com.odontogest.servicio.dto.CuotaPacienteDto;
import com.odontogest.servicio.dto.EstadoCuentaDto;
import com.odontogest.servicio.dto.PacienteDetalle;
import com.odontogest.servicio.dto.PagoDto;
import com.odontogest.ui.soporte.AccionesPago;
import com.odontogest.ui.soporte.Formularios;
import com.odontogest.ui.soporte.HiloUi;
import com.odontogest.ui.soporte.PacienteActivo;
import com.odontogest.ui.soporte.TareaUi;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Pestaña "Pagos" de la ficha (RF-CAJ-04): total, pagado, saldo y vencido; cuotas de los planes aceptados y pagos
 * con su recibo y anulación.
 */
@Component
@RequiredArgsConstructor
public class PagosPacienteController {

    private final PagoService pagoService;
    private final AccionesPago accionesPago;
    private final PacienteActivo pacienteActivo;
    private final TareaUi tareaUi;
    private final SesionUsuario sesionUsuario;
    private final ApplicationEventPublisher eventos;

    @FXML private Label lblTotal;
    @FXML private Label lblPagado;
    @FXML private Label lblSaldo;
    @FXML private Label lblVencido;
    @FXML private Button btnCobrar;
    @FXML private TableView<CuotaPacienteDto> tblCuotas;
    @FXML private Button btnRecibo;
    @FXML private Button btnAnular;
    @FXML private TableView<PagoDto> tblPagos;

    private ChangeListener<PacienteDetalle> oyente;

    @FXML
    public void initialize() {
        TableColumn<CuotaPacienteDto, String> estado = Formularios.columna("Estado", c -> CajaController.etiqueta(c.estado()), 90);
        estado.setCellFactory(c -> CajaController.celdaEstado());
        Formularios.columnas(tblCuotas,
                Formularios.columna("Cuota", CuotaPacienteDto::etiqueta, 150),
                Formularios.columna("Vencimiento", c -> Formularios.fecha(c.vencimiento()), 100),
                Formularios.columna("Monto", c -> Formularios.soles(c.monto()), 100),
                Formularios.columna("Pagado", c -> Formularios.soles(c.pagado()), 100),
                Formularios.columna("Saldo", c -> Formularios.soles(c.saldo()), 100),
                estado);
        tblCuotas.setPlaceholder(new Label("Sin cuotas: el cronograma aparece cuando el paciente acepta un plan."));
        Formularios.columnas(tblPagos,
                Formularios.columna("Recibo", PagoDto::numeroRecibo, 105),
                Formularios.columna("Fecha", p -> Formularios.fechaHora(p.fecha()), 120),
                Formularios.columna("Monto", p -> Formularios.soles(p.monto()), 95),
                Formularios.columna("Medio", p -> p.medio().etiqueta(), 95),
                Formularios.columna("Referencia", p -> p.referencia() == null ? "" : p.referencia(), 110),
                Formularios.columna("Aplicado a", AccionesPago::aplicadoA, 200),
                Formularios.columna("Registrado por", PagoDto::registradoPor, 150),
                AccionesPago.columnaEstado());
        tblPagos.setPlaceholder(new Label("Sin pagos registrados."));
        tblPagos.getSelectionModel().selectedItemProperty().addListener((obs, a, b) -> actualizarBotones());
        boolean cobra = sesionUsuario.tiene(Permiso.PAGO_REGISTRAR);
        btnCobrar.setVisible(cobra);
        btnCobrar.setManaged(cobra);
        actualizarBotones();

        if (oyente != null) {
            pacienteActivo.pacienteProperty().removeListener(oyente);
        }
        oyente = (obs, a, b) -> recargar();
        pacienteActivo.pacienteProperty().addListener(oyente);
        recargar();
    }

    public void recargar() {
        PacienteDetalle p = pacienteActivo.get();
        if (p == null || tblPagos == null) {
            return;
        }
        tareaUi.ejecutar(() -> pagoService.estadoCuenta(p.id()), this::mostrar);
    }

    private void mostrar(EstadoCuentaDto e) {
        PacienteDetalle p = pacienteActivo.get();
        if (p == null || p.id() != e.pacienteId()) {
            return;
        }
        lblTotal.setText(Formularios.soles(e.total()));
        lblPagado.setText(Formularios.soles(e.pagado()));
        lblSaldo.setText(Formularios.soles(e.saldo()));
        lblVencido.setText(Formularios.soles(e.vencido()));
        lblVencido.getStyleClass().remove(Styles.DANGER);
        if (e.vencido().signum() > 0) {
            lblVencido.getStyleClass().add(Styles.DANGER);
        }
        tblCuotas.getItems().setAll(e.cuotas());
        tblPagos.getItems().setAll(e.pagos());
        btnCobrar.setDisable(e.saldo().signum() <= 0);
        actualizarBotones();
    }

    private void actualizarBotones() {
        PagoDto p = tblPagos.getSelectionModel().getSelectedItem();
        btnRecibo.setDisable(p == null);
        btnAnular.setDisable(!accionesPago.puedeAnular(p));
        btnAnular.setVisible(sesionUsuario.tiene(Permiso.PAGO_ANULAR));
        btnAnular.setManaged(btnAnular.isVisible());
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void alRegistrarPago(PagoRegistrado e) {
        HiloUi.ejecutar(() -> refrescarSiEs(e.pacienteId()));
    }

    @EventListener
    public void alCambiarPlan(PlanCambiado e) {
        HiloUi.ejecutar(() -> refrescarSiEs(e.pacienteId()));
    }

    private void refrescarSiEs(long pacienteId) {
        PacienteDetalle p = pacienteActivo.get();
        if (p != null && p.id() == pacienteId) {
            recargar();
        }
    }

    @FXML
    private void onCobrar() {
        PacienteDetalle p = pacienteActivo.get();
        if (p != null) {
            eventos.publishEvent(new NavegarA(MainController.VISTA_CAJA, Map.of("pacienteId", p.id())));
        }
    }

    @FXML
    private void onRecibo() {
        accionesPago.imprimirRecibo(btnRecibo, tblPagos.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void onAnular() {
        accionesPago.anular(tblPagos.getSelectionModel().getSelectedItem(), d -> recargar());
    }
}
