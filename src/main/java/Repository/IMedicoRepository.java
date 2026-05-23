package Repository;

import Entity.MedicoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IMedicoRepository extends JpaRepository<MedicoEntity, Long> {

    Optional<MedicoEntity> findByCmpMedico(String cmpMedico);

    List<MedicoEntity> findByEspecialidad(String especialidad);

    List<MedicoEntity>findByNombreMedicoContainingIgnoreCase(String nombreMedico);

    boolean existsByCmpMedico(String cmpMedico);

    boolean existsByTelefono(String telefono);
}
