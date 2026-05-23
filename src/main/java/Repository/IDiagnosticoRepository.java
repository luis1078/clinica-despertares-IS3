package Repository;

import Entity.DiagnosticoEntity;
import Entity.Emuns.GravedadDiagnosticoEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IDiagnosticoRepository extends JpaRepository<DiagnosticoEntity, Long> {

    List<DiagnosticoEntity> findByGravedadDiagnostico(GravedadDiagnosticoEnum gravedadDiagnostico);

    List<DiagnosticoEntity> findByHistoriaMedica_CodHistoriaMedica(Long codHistoriaMedica);
}
