package Repository;

import Entity.PacienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IPacienteRepository extends JpaRepository<PacienteEntity, String> {
    Optional<PacienteEntity> findByCorreoElectronico(String correoElectronico);

    List<PacienteEntity> findByNombrePacienteContainingIgnoreCase(String nombrePaciente);

    boolean existsByCorreoElectronico(String correoElectronico);

    boolean existsByTelefono(String telefono);
}
