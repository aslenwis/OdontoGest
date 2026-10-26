package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.NotificacionLogASEJ;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionLogRepositoryASEJ extends JpaRepository<NotificacionLogASEJ, Long> {

    List<NotificacionLogASEJ> findByCitaIdASEJOrderByFechaASEJAsc(Long citaIdASEJ);

    List<NotificacionLogASEJ> findTop50ByOrderByFechaASEJDescIdASEJDesc();
}
