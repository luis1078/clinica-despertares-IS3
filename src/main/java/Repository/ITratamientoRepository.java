package Repository;

import Entity.Emuns.EstadoTratamientoEnum;
import Entity.TratamientoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ITratamientoRepository extends JpaRepository<TratamientoEntity, Long> {

    List<TratamientoEntity> findByTipoTratamiento(String tipoTratamiento);

    List<TratamientoEntity> findByEstadoTratamiento(EstadoTratamientoEnum estadoTratamiento);

    List<TratamientoEntity> findByHistoriaMedica_CodHistoriaMedica(Long codHistoriaMedica);
}
