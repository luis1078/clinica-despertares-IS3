package Service.ServiceImpl;

import Entity.CitaMedicaEntity;
import Entity.ComprobantePagoEntity;
import Entity.DetalleComprobanteEntity;
import Entity.Emuns.EstadoCitaEnum;
import Entity.Emuns.EstadoPagoEnum;
import Entity.Emuns.MetodoPagoEnum;
import Entity.Emuns.TipoComprobanteEnum;
import Entity.Emuns.TipoItemEnum;
import Entity.MedicoEntity;
import Entity.PacienteEntity;
import Repository.ICitaMedicaRepository;
import Repository.IComprobantePagoRepository;
import Repository.IDetalleComprobanteRepository;
import Repository.IMedicoRepository;
import Repository.IPacienteRepository;
import Service.ICitaMedicaService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class CitaMedicaServiceImpl implements ICitaMedicaService {

    private static final BigDecimal MONTO_CONSULTA_MEDICA = new BigDecimal("50.00");

    private final ICitaMedicaRepository citaMedicaRepository;
    private final IPacienteRepository pacienteRepository;
    private final IMedicoRepository medicoRepository;
    private final IComprobantePagoRepository comprobantePagoRepository;
    private final IDetalleComprobanteRepository detalleComprobanteRepository;

    public CitaMedicaServiceImpl(ICitaMedicaRepository citaMedicaRepository,
                                 IPacienteRepository pacienteRepository,
                                 IMedicoRepository medicoRepository,
                                 IComprobantePagoRepository comprobantePagoRepository,
                                 IDetalleComprobanteRepository detalleComprobanteRepository) {
        this.citaMedicaRepository = citaMedicaRepository;
        this.pacienteRepository = pacienteRepository;
        this.medicoRepository = medicoRepository;
        this.comprobantePagoRepository = comprobantePagoRepository;
        this.detalleComprobanteRepository = detalleComprobanteRepository;
    }

    @Override
    public List<CitaMedicaEntity> listarTodos() { return citaMedicaRepository.findAll(); }

    @Override
    public Optional<CitaMedicaEntity> buscarPorId(Long codCitaMedica) { return citaMedicaRepository.findById(codCitaMedica); }

    @Override
    public CitaMedicaEntity guardar(CitaMedicaEntity citaMedica) { return citaMedicaRepository.save(citaMedica); }

    @Override
    public void eliminar(Long codCitaMedica) { citaMedicaRepository.deleteById(codCitaMedica); }

    @Override
    public List<CitaMedicaEntity> buscarCitas(String texto, EstadoCitaEnum estadoCita) {
        String busqueda = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);

        return citaMedicaRepository.findAll()
                .stream()
                .filter(cita -> estadoCita == null || cita.getEstadoCita() == estadoCita)
                .filter(cita -> busqueda.isEmpty() || coincideBusqueda(cita, busqueda))
                .toList();
    }

    @Override
    public List<CitaMedicaEntity> listarPorFecha(LocalDate fechaCita) { return citaMedicaRepository.findByFechaCita(fechaCita); }

    @Override
    public List<CitaMedicaEntity> listarPorMedico(Long idMedico) { return citaMedicaRepository.findByMedico_IdMedico(idMedico); }

    @Override
    public List<CitaMedicaEntity> listarPorPaciente(String dniPaciente) { return citaMedicaRepository.findByPaciente_Dnipaciente(dniPaciente); }

    @Override
    public List<CitaMedicaEntity> listarPorEstado(EstadoCitaEnum estadoCita) { return citaMedicaRepository.findByEstadoCita(estadoCita); }

    @Override
    @Transactional
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

        CitaMedicaEntity citaGuardada = citaMedicaRepository.save(citaMedica);
        generarComprobantePagoPorCita(citaGuardada);

        return citaGuardada;
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
        if (cita.getMedico() == null) { throw new IllegalArgumentException("La cita no tiene un médico asignado."); }
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
                .noneMatch(cita -> cita.getFechaCita().equals(fechaCita)
                        && cita.getHoraCita().equals(horaCita)
                        && cita.getEstadoCita() != EstadoCitaEnum.CANCELADA);
    }

    private CitaMedicaEntity obtenerCita(Long codCitaMedica) {
        return citaMedicaRepository.findById(codCitaMedica)
                .orElseThrow(() -> new EntityNotFoundException("No existe la cita médica con código: " + codCitaMedica));
    }

    private boolean coincideBusqueda(CitaMedicaEntity cita, String busqueda) {
        return contiene(cita.getCodCitaMedica(), busqueda)
                || contiene(cita.getMotivoConsulta(), busqueda)
                || contiene(cita.getFechaCita(), busqueda)
                || contiene(cita.getEstadoCita() != null ? cita.getEstadoCita().getEtiqueta() : null, busqueda)
                || contiene(cita.getPaciente() != null ? cita.getPaciente().getDnipaciente() : null, busqueda)
                || contiene(cita.getPaciente() != null ? cita.getPaciente().getNombrePaciente() : null, busqueda)
                || contiene(cita.getPaciente() != null ? cita.getPaciente().getApellidoPaciente() : null, busqueda)
                || contiene(cita.getMedico() != null ? cita.getMedico().getNombreMedico() : null, busqueda)
                || contiene(cita.getMedico() != null ? cita.getMedico().getApellidoMedico() : null, busqueda);
    }

    private boolean contiene(Object valor, String busqueda) {
        return valor != null && valor.toString().toLowerCase(Locale.ROOT).contains(busqueda);
    }

    private void generarComprobantePagoPorCita(CitaMedicaEntity citaGuardada) {
        boolean yaTieneDetalle = detalleComprobanteRepository
                .existsByTipoItemAndIdReferencia(TipoItemEnum.CITA, citaGuardada.getCodCitaMedica());
        if (yaTieneDetalle) { return; }

        ComprobantePagoEntity comprobante = new ComprobantePagoEntity();
        comprobante.setFechaEmision(LocalDate.now());
        comprobante.setTipoComprobante(TipoComprobanteEnum.BOLETA);
        comprobante.setSubtotal(MONTO_CONSULTA_MEDICA);
        comprobante.setMetodoPago(MetodoPagoEnum.EFECTIVO);
        comprobante.setEstado(EstadoPagoEnum.FALTA_PAGAR);
        comprobante.setMontoTotal(MONTO_CONSULTA_MEDICA);
        comprobante.setPaciente(citaGuardada.getPaciente());

        ComprobantePagoEntity comprobanteGuardado = comprobantePagoRepository.save(comprobante);

        DetalleComprobanteEntity detalle = new DetalleComprobanteEntity();
        detalle.setComprobantePago(comprobanteGuardado);
        detalle.setTipoItem(TipoItemEnum.CITA);
        detalle.setIdReferencia(citaGuardada.getCodCitaMedica());
        detalle.setDescripcionDetalle("Comprobante generado automáticamente por cita médica N° " + citaGuardada.getCodCitaMedica());
        detalle.setCantidad(1);
        detalle.setPrecioUnitario(MONTO_CONSULTA_MEDICA);
        detalle.setSubtotal(MONTO_CONSULTA_MEDICA);

        detalleComprobanteRepository.save(detalle);
    }
}
