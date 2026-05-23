package Repository;

import Entity.CitaMedicaEntity;
import Entity.Emuns.EstadoCitaEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ICitaMedicaRepository extends JpaRepository<CitaMedicaEntity, Long> {

    List<CitaMedicaEntity> findByFechaCita(LocalDate fechaCita);

    List<CitaMedicaEntity> findByMedico_IdMedico(Long idMedico);

    List<CitaMedicaEntity> findByPaciente_Dnipaciente(String dniPaciente);

    List<CitaMedicaEntity> findByEstadoCita(EstadoCitaEnum estadoCita);
}
