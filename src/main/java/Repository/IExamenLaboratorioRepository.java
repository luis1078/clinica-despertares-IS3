package Repository;

import Entity.ExamenLaboratorioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IExamenLaboratorioRepository extends JpaRepository<ExamenLaboratorioEntity, Long> {

    List<ExamenLaboratorioEntity> findByNombreMuestra(String nombreMuestra);

    List<ExamenLaboratorioEntity> findByUnidadMedida(String unidadMedida);
}
