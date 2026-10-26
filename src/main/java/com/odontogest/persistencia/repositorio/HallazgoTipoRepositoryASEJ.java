package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.HallazgoTipoASEJ;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface HallazgoTipoRepositoryASEJ extends JpaRepository<HallazgoTipoASEJ, Long> {

    Optional<HallazgoTipoASEJ> findByCodigoASEJIgnoreCase(String codigoASEJ);

    List<HallazgoTipoASEJ> findAllByOrderByOrdenASEJAscNombreASEJAsc();

    @Query(value = "SELECT COUNT(*) FROM hallazgo WHERE hallazgo_tipo_id = :tipoIdASEJ", nativeQuery = true)
    long contarHallazgosASEJ(Long tipoIdASEJ);

    @Query("SELECT DISTINCT t.grupoExclusionASEJ FROM HallazgoTipoASEJ t ORDER BY t.grupoExclusionASEJ")
    List<String> gruposExclusionASEJ();
}
