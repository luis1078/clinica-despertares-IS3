package Service.ServiceImpl;

import Entity.CitaMedicaEntity;
import Entity.Emuns.EstadoCitaEnum;
import Entity.MedicoEntity;
import Entity.PacienteEntity;
import Repository.ICitaMedicaRepository;
import Repository.IMedicoRepository;
import Repository.IPacienteRepository;
import Service.ICitaMedicaService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class CitaMedicaServiceImpl implements ICitaMedicaService {

    private final ICitaMedicaRepository citaMedicaRepository;
    private final IPacienteRepository pacienteRepository;
    private final IMedicoRepository medicoRepository;

    public CitaMedicaServiceImpl(ICitaMedicaRepository citaMedicaRepository,
                                 IPacienteRepository pacienteRepository,
                                 IMedicoRepository medicoRepository) {
        this.citaMedicaRepository = citaMedicaRepository;
        this.pacienteRepository = pacienteRepository;
        this.medicoRepository = medicoRepository;
    }

    @Override
    public List<CitaMedicaEntity> listarTodos() {
        return citaMedicaRepository.findAll();
    }

    @Override
    public Optional<CitaMedicaEntity> buscarPorId(Long codCitaMedica) {
        return citaMedicaRepository.findById(codCitaMedica);
    }

    @Override
    public CitaMedicaEntity guardar(CitaMedicaEntity citaMedica) {
        return citaMedicaRepository.save(citaMedica);
    }

    @Override
    public void eliminar(Long codCitaMedica) {
        citaMedicaRepository.deleteById(codCitaMedica);
    }

    @Override
    public List<CitaMedicaEntity> listarPorFecha(LocalDate fechaCita) {
        return citaMedicaRepository.findByFechaCita(fechaCita);
    }

    @Override
    public List<CitaMedicaEntity> listarPorMedico(Long idMedico) {
        return citaMedicaRepository.findByMedico_IdMedico(idMedico);
    }

    @Override
    public List<CitaMedicaEntity> listarPorPaciente(String dniPaciente) {
        return citaMedicaRepository.findByPaciente_Dnipaciente(dniPaciente);
    }

    @Override
    public List<CitaMedicaEntity> listarPorEstado(EstadoCitaEnum estadoCita) {
        return citaMedicaRepository.findByEstadoCita(estadoCita);
    }

    @Override
    public CitaMedicaEntity registrarCitaMedica(String dniPaciente, Long idMedico, CitaMedicaEntity citaMedica) {
        PacienteEntity paciente = pacienteRepository.findById(dniPaciente)
                .orElseThrow(() -> new EntityNotFoundException("No existe el paciente con DNI: " + dniPaciente));

        MedicoEntity medico = medicoRepository.findById(idMedico)
                .orElseThrow(() -> new EntityNotFoundException("No existe el médico con ID: " + idMedico));

        if (!existeDisponibilidadMedico(idMedico, citaMedica.getFechaCita(), citaMedica.getHoraCita())) {
            throw new IllegalArgumentException("El médico no tiene disponibilidad en la fecha y hora seleccionada.");
        }

        citaMedica.setPaciente(paciente);
        citaMedica.setMedico(medico);
        citaMedica.setEstadoCita(EstadoCitaEnum.PENDIENTE);

        return citaMedicaRepository.save(citaMedica);
    }

    @Override
    public CitaMedicaEntity cancelarCitaMedica(Long codCitaMedica) {
        CitaMedicaEntity cita = obtenerCita(codCitaMedica);
        cita.setEstadoCita(EstadoCitaEnum.CANCELADA);
        return citaMedicaRepository.save(cita);
    }

    @Override
    public CitaMedicaEntity finalizarCitaMedica(Long codCitaMedica) {
        CitaMedicaEntity cita = obtenerCita(codCitaMedica);
        cita.setEstadoCita(EstadoCitaEnum.FINALIZADA);
        return citaMedicaRepository.save(cita);
    }

    @Override
    public CitaMedicaEntity reprogramarCitaMedica(Long codCitaMedica, LocalDate nuevaFecha, LocalTime nuevaHora) {
        CitaMedicaEntity cita = obtenerCita(codCitaMedica);

        if (cita.getMedico() == null) {
            throw new IllegalArgumentException("La cita no tiene un médico asignado.");
        }

        Long idMedico = cita.getMedico().getIdMedico();

        if (!existeDisponibilidadMedico(idMedico, nuevaFecha, nuevaHora)) {
            throw new IllegalArgumentException("El médico no tiene disponibilidad en la nueva fecha y hora.");
        }

        cita.setFechaCita(nuevaFecha);
        cita.setHoraCita(nuevaHora);
        cita.setEstadoCita(EstadoCitaEnum.PENDIENTE);

        return citaMedicaRepository.save(cita);
    }

    @Override
    public boolean existeDisponibilidadMedico(Long idMedico, LocalDate fechaCita, LocalTime horaCita) {
        return citaMedicaRepository.findByMedico_IdMedico(idMedico)
                .stream()
                .noneMatch(cita ->
                        cita.getFechaCita().equals(fechaCita)
                                && cita.getHoraCita().equals(horaCita)
                                && cita.getEstadoCita() != EstadoCitaEnum.CANCELADA
                );
    }

    private CitaMedicaEntity obtenerCita(Long codCitaMedica) {
        return citaMedicaRepository.findById(codCitaMedica)
                .orElseThrow(() -> new EntityNotFoundException("No existe la cita médica con código: " + codCitaMedica));
    }
}
