package Service;

import Entity.Emuns.EstadoExamenMedicoEnum;
import Entity.Emuns.TipoExamenEnum;
import Entity.ExamenMedicoEntity;

import java.util.List;
import java.util.Optional;

public interface IExamenMedicoService {

    List<ExamenMedicoEntity> listarTodos();

    Optional<ExamenMedicoEntity> buscarPorId(Long codExamenMedico);

    ExamenMedicoEntity guardar(ExamenMedicoEntity examenMedico);

    void eliminar(Long codExamenMedico);

    List<ExamenMedicoEntity> listarPorTipoExamen(TipoExamenEnum tipoExamen);

    List<ExamenMedicoEntity> listarPorEstado(EstadoExamenMedicoEnum estadoExamenMedico);

    List<ExamenMedicoEntity> listarPorDiagnostico(Long idDiagnostico);

    ExamenMedicoEntity registrarSolicitudExamenMedico(Long idDiagnostico, ExamenMedicoEntity examenMedico);

    ExamenMedicoEntity marcarEnProceso(Long codExamenMedico);

    ExamenMedicoEntity marcarFinalizado(Long codExamenMedico);
}
