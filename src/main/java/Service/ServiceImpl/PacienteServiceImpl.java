package Service.ServiceImpl;

import Entity.PacienteEntity;
import Repository.IPacienteRepository;
import Service.IPacienteService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PacienteServiceImpl implements IPacienteService {

    private final IPacienteRepository pacienteRepository;

    public PacienteServiceImpl(IPacienteRepository pacienteRepository) {
        this.pacienteRepository = pacienteRepository;
    }

    @Override
    public List<PacienteEntity> listarTodos() {
        return pacienteRepository.findAll();
    }

    @Override
    public Optional<PacienteEntity> buscarPorId(String dniPaciente) {
        return pacienteRepository.findById(dniPaciente);
    }

    @Override
    public PacienteEntity guardar(PacienteEntity paciente) {
        return pacienteRepository.save(paciente);
    }

    @Override
    public void eliminar(String dniPaciente) {
        pacienteRepository.deleteById(dniPaciente);
    }

    @Override
    public Optional<PacienteEntity> buscarPorCorreoElectronico(String correoElectronico) {
        return pacienteRepository.findByCorreoElectronico(correoElectronico);
    }

    @Override
    public List<PacienteEntity> buscarPorNombre(String nombrePaciente) {
        return pacienteRepository.findByNombrePacienteContainingIgnoreCase(nombrePaciente);
    }

    @Override
    public List<PacienteEntity> buscarPacientes(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return pacienteRepository.findAll();
        }
        String busqueda = texto.trim();
        return pacienteRepository
                .findByDnipacienteContainingIgnoreCaseOrNombrePacienteContainingIgnoreCaseOrApellidoPacienteContainingIgnoreCaseOrCorreoElectronicoContainingIgnoreCaseOrTelefonoContainingIgnoreCase(
                        busqueda, busqueda, busqueda, busqueda, busqueda
                );
    }

    @Override
    public boolean existePorCorreoElectronico(String correoElectronico) {
        return pacienteRepository.existsByCorreoElectronico(correoElectronico);
    }

    @Override
    public boolean existePorTelefono(String telefono) {
        return pacienteRepository.existsByTelefono(telefono);
    }
}
