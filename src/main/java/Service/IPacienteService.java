package Service;

import Entity.PacienteEntity;

import java.util.List;
import java.util.Optional;

public interface IPacienteService {

    List<PacienteEntity> listarTodos();

    Optional<PacienteEntity> buscarPorId(String dniPaciente);

    PacienteEntity guardar(PacienteEntity paciente);

    void eliminar(String dniPaciente);

    Optional<PacienteEntity> buscarPorCorreoElectronico(String correoElectronico);

    List<PacienteEntity> buscarPorNombre(String nombrePaciente);

    boolean existePorCorreoElectronico(String correoElectronico);

    boolean existePorTelefono(String telefono);
}
