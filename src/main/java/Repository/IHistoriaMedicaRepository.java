package Repository;

import Entity.HistoriaMedicaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface IHistoriaMedicaRepository extends JpaRepository<HistoriaMedicaEntity, Long> {

    Optional<HistoriaMedicaEntity>findByPaciente_Dnipaciente(String dniPaciente);

    List<HistoriaMedicaEntity>findByFechaCreacion(LocalDate fechaCreacion);
}
