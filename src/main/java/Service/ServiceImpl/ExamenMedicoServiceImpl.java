package Service.ServiceImpl;

import Entity.DiagnosticoEntity;
import Entity.Emuns.EstadoExamenMedicoEnum;
import Entity.Emuns.TipoExamenEnum;
import Entity.ExamenMedicoEntity;
import Repository.IDiagnosticoRepository;
import Repository.IExamenMedicoRepository;
import Service.IExamenMedicoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ExamenMedicoServiceImpl implements IExamenMedicoService {

    private final IExamenMedicoRepository examenMedicoRepository;
    private final IDiagnosticoRepository diagnosticoRepository;

    public ExamenMedicoServiceImpl(IExamenMedicoRepository examenMedicoRepository,
                                   IDiagnosticoRepository diagnosticoRepository) {
        this.examenMedicoRepository = examenMedicoRepository;
        this.diagnosticoRepository = diagnosticoRepository;
    }

    @Override
    public List<ExamenMedicoEntity> listarTodos() {
        return examenMedicoRepository.findAll();
    }

    @Override
    public Optional<ExamenMedicoEntity> buscarPorId(Long codExamenMedico) {
        return examenMedicoRepository.findById(codExamenMedico);
    }

    @Override
    public ExamenMedicoEntity guardar(ExamenMedicoEntity examenMedico) {
        return examenMedicoRepository.save(examenMedico);
    }

    @Override
    public void eliminar(Long codExamenMedico) {
        examenMedicoRepository.deleteById(codExamenMedico);
    }

    @Override
    public List<ExamenMedicoEntity> listarPorTipoExamen(TipoExamenEnum tipoExamen) {
        return examenMedicoRepository.findByTipoExamen(tipoExamen);
    }

    @Override
    public List<ExamenMedicoEntity> listarPorEstado(EstadoExamenMedicoEnum estadoExamenMedico) {
        return examenMedicoRepository.findByEstadoExamenMedico(estadoExamenMedico);
    }

    @Override
    public List<ExamenMedicoEntity> listarPorDiagnostico(Long idDiagnostico) {
        return examenMedicoRepository.findByDiagnostico_IdDiagnostico(idDiagnostico);
    }

    @Override
    public ExamenMedicoEntity registrarSolicitudExamenMedico(Long idDiagnostico, ExamenMedicoEntity examenMedico) {
        DiagnosticoEntity diagnostico = diagnosticoRepository.findById(idDiagnostico)
                .orElseThrow(() -> new EntityNotFoundException("No existe el diagnóstico con ID: " + idDiagnostico));

        if (examenMedico.getFechaExamen() == null) {
            examenMedico.setFechaExamen(LocalDate.now());
        }

        if (examenMedico.getEstadoExamenMedico() == null) {
            examenMedico.setEstadoExamenMedico(EstadoExamenMedicoEnum.PENDIENTE);
        }

        examenMedico.setDiagnostico(diagnostico);
        return examenMedicoRepository.save(examenMedico);
    }

    @Override
    public ExamenMedicoEntity marcarEnProceso(Long codExamenMedico) {
        ExamenMedicoEntity examen = examenMedicoRepository.findById(codExamenMedico)
                .orElseThrow(() -> new IllegalArgumentException("No existe el examen médico con ID: " + codExamenMedico));

        examen.setEstadoExamenMedico(EstadoExamenMedicoEnum.EN_PROCESO);

        return examenMedicoRepository.save(examen);
    }

    @Override
    public ExamenMedicoEntity marcarFinalizado(Long codExamenMedico) {
        ExamenMedicoEntity examen = obtenerExamen(codExamenMedico);
        examen.setEstadoExamenMedico(EstadoExamenMedicoEnum.FINALIZADO);
        examen.setFechaResultado(LocalDate.now());
        return examenMedicoRepository.save(examen);
    }

    private ExamenMedicoEntity obtenerExamen(Long codExamenMedico) {
        return examenMedicoRepository.findById(codExamenMedico)
                .orElseThrow(() -> new EntityNotFoundException("No existe el examen médico con código: " + codExamenMedico));
    }
}
