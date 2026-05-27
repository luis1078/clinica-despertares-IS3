package Service.ServiceImpl;

import Entity.DiagnosticoEntity;
import Entity.Emuns.GravedadDiagnosticoEnum;
import Entity.HistoriaMedicaEntity;
import Repository.IDiagnosticoRepository;
import Repository.IHistoriaMedicaRepository;
import Service.IDiagnosticoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class DiagnosticoServiceImpl implements IDiagnosticoService {

    private final IDiagnosticoRepository diagnosticoRepository;
    private final IHistoriaMedicaRepository historiaMedicaRepository;

    public DiagnosticoServiceImpl(IDiagnosticoRepository diagnosticoRepository,
                                  IHistoriaMedicaRepository historiaMedicaRepository) {
        this.diagnosticoRepository = diagnosticoRepository;
        this.historiaMedicaRepository = historiaMedicaRepository;
    }

    @Override
    public List<DiagnosticoEntity> listarTodos() {
        return diagnosticoRepository.findAll();
    }

    @Override
    public Optional<DiagnosticoEntity> buscarPorId(Long idDiagnostico) {
        return diagnosticoRepository.findById(idDiagnostico);
    }

    @Override
    public DiagnosticoEntity guardar(DiagnosticoEntity diagnostico) {
        return diagnosticoRepository.save(diagnostico);
    }

    @Override
    public void eliminar(Long idDiagnostico) {
        diagnosticoRepository.deleteById(idDiagnostico);
    }

    @Override
    public List<DiagnosticoEntity> listarPorGravedad(GravedadDiagnosticoEnum gravedadDiagnostico) {
        return diagnosticoRepository.findByGravedadDiagnostico(gravedadDiagnostico);
    }

    @Override
    public List<DiagnosticoEntity> listarPorHistoriaMedica(Long codHistoriaMedica) {
        return diagnosticoRepository.findByHistoriaMedica_CodHistoriaMedica(codHistoriaMedica);
    }

    @Override
    public DiagnosticoEntity registrarDiagnostico(Long codHistoriaMedica, DiagnosticoEntity diagnostico) {
        HistoriaMedicaEntity historia = historiaMedicaRepository.findById(codHistoriaMedica)
                .orElseThrow(() -> new EntityNotFoundException("No existe la historia médica con código: " + codHistoriaMedica));

        if (diagnostico.getFechaDiagnostico() == null) {
            diagnostico.setFechaDiagnostico(LocalDate.now());
        }

        diagnostico.setHistoriaMedica(historia);
        return diagnosticoRepository.save(diagnostico);
    }
}
