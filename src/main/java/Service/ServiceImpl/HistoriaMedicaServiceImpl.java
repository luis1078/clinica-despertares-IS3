package Service.ServiceImpl;

import Entity.HistoriaMedicaEntity;
import Entity.PacienteEntity;
import Repository.IHistoriaMedicaRepository;
import Repository.IPacienteRepository;
import Service.IHistoriaMedicaService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class HistoriaMedicaServiceImpl implements IHistoriaMedicaService {

    private final IHistoriaMedicaRepository historiaMedicaRepository;
    private final IPacienteRepository pacienteRepository;

    public HistoriaMedicaServiceImpl(IHistoriaMedicaRepository historiaMedicaRepository,
                                     IPacienteRepository pacienteRepository) {
        this.historiaMedicaRepository = historiaMedicaRepository;
        this.pacienteRepository = pacienteRepository;
    }

    @Override
    public List<HistoriaMedicaEntity> listarTodos() {
        return historiaMedicaRepository.findAll();
    }

    @Override
    public Optional<HistoriaMedicaEntity> buscarPorId(Long codHistoriaMedica) {
        return historiaMedicaRepository.findById(codHistoriaMedica);
    }

    @Override
    public HistoriaMedicaEntity guardar(HistoriaMedicaEntity historiaMedica) {
        return historiaMedicaRepository.save(historiaMedica);
    }

    @Override
    public void eliminar(Long codHistoriaMedica) {
        historiaMedicaRepository.deleteById(codHistoriaMedica);
    }

    @Override
    public Optional<HistoriaMedicaEntity> buscarPorDniPaciente(String dniPaciente) {
        return historiaMedicaRepository.findByPaciente_Dnipaciente(dniPaciente);
    }

    @Override
    public List<HistoriaMedicaEntity> listarPorFechaCreacion(LocalDate fechaCreacion) {
        return historiaMedicaRepository.findByFechaCreacion(fechaCreacion);
    }

    @Override
    public HistoriaMedicaEntity crearHistoriaMedica(String dniPaciente, HistoriaMedicaEntity historiaMedica) {
        PacienteEntity paciente = pacienteRepository.findById(dniPaciente)
                .orElseThrow(() -> new EntityNotFoundException("No existe el paciente con DNI: " + dniPaciente));

        if (historiaMedicaRepository.findByPaciente_Dnipaciente(dniPaciente).isPresent()) {
            throw new IllegalArgumentException("El paciente ya tiene una historia médica registrada.");
        }

        if (historiaMedica.getFechaCreacion() == null) {
            historiaMedica.setFechaCreacion(LocalDate.now());
        }

        historiaMedica.setPaciente(paciente);
        return historiaMedicaRepository.save(historiaMedica);
    }

    @Override
    public HistoriaMedicaEntity actualizarHistoriaMedica(Long codHistoriaMedica, HistoriaMedicaEntity datosActualizados) {
        HistoriaMedicaEntity historia = historiaMedicaRepository.findById(codHistoriaMedica)
                .orElseThrow(() -> new EntityNotFoundException("No existe la historia médica con código: " + codHistoriaMedica));

        historia.setAntecedentesPersonales(datosActualizados.getAntecedentesPersonales());
        historia.setAntecedentesFamiliares(datosActualizados.getAntecedentesFamiliares());
        historia.setIntervencionesQuirurgicas(datosActualizados.getIntervencionesQuirurgicas());

        return historiaMedicaRepository.save(historia);
    }
}
