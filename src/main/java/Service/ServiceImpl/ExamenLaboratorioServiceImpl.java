package Service.ServiceImpl;

import Entity.DiagnosticoEntity;
import Entity.Emuns.EstadoExamenMedicoEnum;
import Entity.Emuns.TipoExamenEnum;
import Entity.ExamenLaboratorioEntity;
import Repository.IDiagnosticoRepository;
import Repository.IExamenLaboratorioRepository;
import Service.IExamenLaboratorioService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ExamenLaboratorioServiceImpl implements IExamenLaboratorioService {

    private final IExamenLaboratorioRepository examenLaboratorioRepository;
    private final IDiagnosticoRepository diagnosticoRepository;

    public ExamenLaboratorioServiceImpl(IExamenLaboratorioRepository examenLaboratorioRepository,
                                        IDiagnosticoRepository diagnosticoRepository) {
        this.examenLaboratorioRepository = examenLaboratorioRepository;
        this.diagnosticoRepository = diagnosticoRepository;
    }

    @Override
    public List<ExamenLaboratorioEntity> listarTodos() {
        return examenLaboratorioRepository.findAll();
    }

    @Override
    public Optional<ExamenLaboratorioEntity> buscarPorId(Long codExamenMedico) {
        return examenLaboratorioRepository.findById(codExamenMedico);
    }

    @Override
    public ExamenLaboratorioEntity guardar(ExamenLaboratorioEntity examenLaboratorio) {
        return examenLaboratorioRepository.save(examenLaboratorio);
    }

    @Override
    public void eliminar(Long codExamenMedico) {
        examenLaboratorioRepository.deleteById(codExamenMedico);
    }

    @Override
    public List<ExamenLaboratorioEntity> listarPorNombreMuestra(String nombreMuestra) {
        return examenLaboratorioRepository.findByNombreMuestra(nombreMuestra);
    }

    @Override
    public List<ExamenLaboratorioEntity> listarPorUnidadMedida(String unidadMedida) {
        return examenLaboratorioRepository.findByUnidadMedida(unidadMedida);
    }

    @Override
    public List<ExamenLaboratorioEntity> buscarExamenes(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return examenLaboratorioRepository.findAll();
        }

        return examenLaboratorioRepository.buscarPorTexto(texto.trim());
    }

    @Override
    public ExamenLaboratorioEntity registrarExamenLaboratorio(Long idDiagnostico, ExamenLaboratorioEntity examenLaboratorio) {
        DiagnosticoEntity diagnostico = diagnosticoRepository.findById(idDiagnostico)
                .orElseThrow(() -> new EntityNotFoundException("No existe el diagnóstico con ID: " + idDiagnostico));

        if (examenLaboratorio.getFechaExamen() == null) {
            examenLaboratorio.setFechaExamen(LocalDate.now());
        }

        examenLaboratorio.setTipoExamen(TipoExamenEnum.LABORATORIO);
        examenLaboratorio.setEstadoExamenMedico(EstadoExamenMedicoEnum.PENDIENTE);
        examenLaboratorio.setDiagnostico(diagnostico);

        return examenLaboratorioRepository.save(examenLaboratorio);
    }

    @Override
    public ExamenLaboratorioEntity registrarResultadoLaboratorio(Long codExamenMedico,
                                                                 ExamenLaboratorioEntity datosResultado) {
        ExamenLaboratorioEntity examen = examenLaboratorioRepository.findById(codExamenMedico)
                .orElseThrow(() -> new EntityNotFoundException("No existe el examen de laboratorio con código: " + codExamenMedico));

        examen.setNombreMuestra(datosResultado.getNombreMuestra());
        examen.setDescripcionResultado(datosResultado.getDescripcionResultado());
        examen.setValorReferencia(datosResultado.getValorReferencia());
        examen.setUnidadMedida(datosResultado.getUnidadMedida());
        examen.setObservaciones(datosResultado.getObservaciones());
        examen.setFechaResultado(LocalDate.now());
        examen.setEstadoExamenMedico(EstadoExamenMedicoEnum.FINALIZADO);

        return examenLaboratorioRepository.save(examen);
    }
}
