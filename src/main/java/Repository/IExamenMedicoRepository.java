package Repository;

import Entity.Emuns.EstadoExamenMedicoEnum;
import Entity.Emuns.TipoExamenEnum;
import Entity.ExamenMedicoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IExamenMedicoRepository extends JpaRepository<ExamenMedicoEntity, Long> {

    List<ExamenMedicoEntity> findByTipoExamen(TipoExamenEnum tipoExamen);

    List<ExamenMedicoEntity> findByEstadoExamenMedico(EstadoExamenMedicoEnum estadoExamenMedico);

    List<ExamenMedicoEntity> findByDiagnostico_IdDiagnostico(Long idDiagnostico);
}
