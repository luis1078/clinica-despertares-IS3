package Service.ServiceImpl;

import Entity.ComprobantePagoEntity;
import Entity.DetalleComprobanteEntity;
import Entity.DiagnosticoEntity;
import Entity.Emuns.EstadoExamenMedicoEnum;
import Entity.Emuns.EstadoPagoEnum;
import Entity.Emuns.MetodoPagoEnum;
import Entity.Emuns.TipoComprobanteEnum;
import Entity.Emuns.TipoExamenEnum;
import Entity.Emuns.TipoItemEnum;
import Entity.ExamenMedicoEntity;
import Entity.PacienteEntity;
import Repository.IDiagnosticoRepository;
import Repository.IComprobantePagoRepository;
import Repository.IDetalleComprobanteRepository;
import Repository.IExamenMedicoRepository;
import Service.IExamenMedicoService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ExamenMedicoServiceImpl implements IExamenMedicoService {

    private static final BigDecimal MONTO_SOLICITUD_EXAMEN = new BigDecimal("80.00");

    private final IExamenMedicoRepository examenMedicoRepository;
    private final IDiagnosticoRepository diagnosticoRepository;
    private final IComprobantePagoRepository comprobantePagoRepository;
    private final IDetalleComprobanteRepository detalleComprobanteRepository;

    public ExamenMedicoServiceImpl(IExamenMedicoRepository examenMedicoRepository,
                                   IDiagnosticoRepository diagnosticoRepository,
                                   IComprobantePagoRepository comprobantePagoRepository,
                                   IDetalleComprobanteRepository detalleComprobanteRepository) {
        this.examenMedicoRepository = examenMedicoRepository;
        this.diagnosticoRepository = diagnosticoRepository;
        this.comprobantePagoRepository = comprobantePagoRepository;
        this.detalleComprobanteRepository = detalleComprobanteRepository;
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
    @Transactional
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
        ExamenMedicoEntity examenGuardado = examenMedicoRepository.save(examenMedico);
        generarComprobantePagoPorExamen(examenGuardado);

        return examenGuardado;
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

    private void generarComprobantePagoPorExamen(ExamenMedicoEntity examenGuardado) {
        boolean yaTieneDetalle = detalleComprobanteRepository
                .existsByTipoItemAndIdReferencia(TipoItemEnum.EXAMEN, examenGuardado.getCodExamenMedico());
        if (yaTieneDetalle) { return; }

        PacienteEntity paciente = obtenerPacienteDelExamen(examenGuardado);

        ComprobantePagoEntity comprobante = new ComprobantePagoEntity();
        comprobante.setFechaEmision(LocalDate.now());
        comprobante.setTipoComprobante(TipoComprobanteEnum.BOLETA);
        comprobante.setSubtotal(MONTO_SOLICITUD_EXAMEN);
        comprobante.setMetodoPago(MetodoPagoEnum.EFECTIVO);
        comprobante.setEstado(EstadoPagoEnum.FALTA_PAGAR);
        comprobante.setMontoTotal(MONTO_SOLICITUD_EXAMEN);
        comprobante.setPaciente(paciente);

        ComprobantePagoEntity comprobanteGuardado = comprobantePagoRepository.save(comprobante);

        DetalleComprobanteEntity detalle = new DetalleComprobanteEntity();
        detalle.setComprobantePago(comprobanteGuardado);
        detalle.setTipoItem(TipoItemEnum.EXAMEN);
        detalle.setIdReferencia(examenGuardado.getCodExamenMedico());
        detalle.setDescripcionDetalle("Comprobante generado automáticamente por solicitud de examen médico N° " + examenGuardado.getCodExamenMedico());
        detalle.setCantidad(1);
        detalle.setPrecioUnitario(MONTO_SOLICITUD_EXAMEN);
        detalle.setSubtotal(MONTO_SOLICITUD_EXAMEN);

        detalleComprobanteRepository.save(detalle);
    }

    private PacienteEntity obtenerPacienteDelExamen(ExamenMedicoEntity examen) {
        if (examen.getDiagnostico() == null
                || examen.getDiagnostico().getHistoriaMedica() == null
                || examen.getDiagnostico().getHistoriaMedica().getPaciente() == null) {
            throw new IllegalArgumentException("El diagnóstico seleccionado no tiene un paciente asociado para generar el comprobante.");
        }

        return examen.getDiagnostico().getHistoriaMedica().getPaciente();
    }
}
