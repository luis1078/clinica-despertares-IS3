package Service;

import Entity.MedicoEntity;

import java.util.List;
import java.util.Optional;

public interface IMedicoService {

    List<MedicoEntity> listarTodos();

    Optional<MedicoEntity> buscarPorId(Long idMedico);

    MedicoEntity guardar(MedicoEntity medico);

    void eliminar(Long idMedico);

    Optional<MedicoEntity> buscarPorCmpMedico(String cmpMedico);

    List<MedicoEntity> buscarPorEspecialidad(String especialidad);

    List<MedicoEntity> buscarPorNombre(String nombreMedico);

    boolean existePorCmpMedico(String cmpMedico);

    boolean existePorTelefono(String telefono);
}
