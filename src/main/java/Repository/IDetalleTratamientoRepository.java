package Repository;

import Entity.DetalleTratamientoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IDetalleTratamientoRepository extends JpaRepository<DetalleTratamientoEntity, Long>{
    List<DetalleTratamientoEntity> findByTratamiento_IdTratamiento(Long idTratamiento);

    List<DetalleTratamientoEntity> findByMedicamento_CodMedicamento(Long codMedicamento);
}
