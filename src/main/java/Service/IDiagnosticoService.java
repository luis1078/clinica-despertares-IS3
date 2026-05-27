package Service;

import Entity.DiagnosticoEntity;
import Entity.Emuns.GravedadDiagnosticoEnum;

import java.util.List;
import java.util.Optional;

public interface IDiagnosticoService {

    List<DiagnosticoEntity> listarTodos();

    Optional<DiagnosticoEntity> buscarPorId(Long idDiagnostico);

    DiagnosticoEntity guardar(DiagnosticoEntity diagnostico);

    void eliminar(Long idDiagnostico);

    List<DiagnosticoEntity> listarPorGravedad(GravedadDiagnosticoEnum gravedadDiagnostico);

    List<DiagnosticoEntity> listarPorHistoriaMedica(Long codHistoriaMedica);

    DiagnosticoEntity registrarDiagnostico(Long codHistoriaMedica, DiagnosticoEntity diagnostico);
}
