package Service;

import Entity.ExamenLaboratorioEntity;

import java.util.List;
import java.util.Optional;

public interface IExamenLaboratorioService {

    List<ExamenLaboratorioEntity> listarTodos();

    Optional<ExamenLaboratorioEntity> buscarPorId(Long codExamenMedico);

    ExamenLaboratorioEntity guardar(ExamenLaboratorioEntity examenLaboratorio);

    void eliminar(Long codExamenMedico);

    List<ExamenLaboratorioEntity> listarPorNombreMuestra(String nombreMuestra);

    List<ExamenLaboratorioEntity> listarPorUnidadMedida(String unidadMedida);

    List<ExamenLaboratorioEntity> buscarExamenes(String texto);

    ExamenLaboratorioEntity registrarExamenLaboratorio(Long idDiagnostico, ExamenLaboratorioEntity examenLaboratorio);

    ExamenLaboratorioEntity registrarResultadoLaboratorio(Long codExamenMedico, ExamenLaboratorioEntity datosResultado);
}
