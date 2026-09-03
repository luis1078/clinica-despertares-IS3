package Repository;

import Entity.ComprobantePagoEntity;
import Entity.Emuns.EstadoPagoEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface IComprobantePagoRepository extends JpaRepository<ComprobantePagoEntity, Long> {

    List<ComprobantePagoEntity> findByPaciente_Dnipaciente(String dniPaciente);

    List<ComprobantePagoEntity> findByFechaEmision(LocalDate fechaEmision);

    List<ComprobantePagoEntity> findByEstado(EstadoPagoEnum estado);
}
