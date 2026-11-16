package QA;

import Entity.*;
import Entity.Emuns.*;
import Service.ICitaMedicaService;
import Service.dto.EventoHistorialDTO;
import Service.dto.HistorialClinicoDTO;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Citas, comprobante automático, historia médica, triaje y diagnósticos (FTC011–FTC032)")
class FTC02_CitasHistoriasDiagnosticosTest extends SoporteQA {

    @Autowired ICitaMedicaService citaMedicaService;

    private static final LocalDate DIA = LocalDate.now().plusDays(20);

    private MvcResult registrarCita(PacienteEntity p, MedicoEntity m, LocalDate fecha, String hora) throws Exception {
        return mvc.perform(post("/citas/guardar", sesion("ENFERMERA"))
                .param("dniPaciente", p.getDnipaciente()).param("idMedico", m.getIdMedico().toString())
                .param("fechaCita", fecha.toString()).param("horaCita", hora)
                .param("motivoConsulta", "Dolor de cabeza")).andReturn();
    }

    @Test
    @DisplayName("FTC011 - Registrar cita con un médico disponible")
    void ftc011() throws Exception {
        PacienteEntity p = paciente();
        MedicoEntity m = medico();
        MvcResult r = registrarCita(p, m, DIA, "09:00");
        List<CitaMedicaEntity> citas = citaRepo.findByMedico_IdMedico(m.getIdMedico());
        assertAll(
                () -> assertEquals("/citas", redirect(r)),
                () -> assertEquals("Cita médica registrada correctamente. Se generó el comprobante de pago automáticamente.", flash(r, "mensaje")),
                () -> assertEquals(1, citas.size()),
                () -> assertEquals(EstadoCitaEnum.PENDIENTE, citas.get(0).getEstadoCita())
        );
    }

    @Test
    @DisplayName("FTC012 - Rechazar cita cuando el médico ya está ocupado; horario cancelado queda libre")
    void ftc012() throws Exception {
        PacienteEntity p = paciente();
        MedicoEntity m = medico();
        cita(p, m, DIA, LocalTime.of(10, 0), EstadoCitaEnum.PENDIENTE);
        cita(p, m, DIA, LocalTime.of(11, 0), EstadoCitaEnum.CANCELADA);
        long comprobantesAntes = comprobanteRepo.findByPaciente_Dnipaciente(p.getDnipaciente()).size();

        MvcResult ocupado = registrarCita(p, m, DIA, "10:00");
        long comprobantesDespues = comprobanteRepo.findByPaciente_Dnipaciente(p.getDnipaciente()).size();
        int citasTrasPaso1 = citaRepo.findByMedico_IdMedico(m.getIdMedico()).size();
        MvcResult liberado = registrarCita(p, m, DIA, "11:00");

        assertAll(
                () -> assertEquals("El médico no tiene disponibilidad en la fecha y hora seleccionada.", modelo(ocupado, "error")),
                () -> assertEquals(2, citasTrasPaso1, "No debe crearse la cita en el horario ocupado"),
                () -> assertEquals(comprobantesAntes, comprobantesDespues, "No debe crearse comprobante"),
                () -> assertEquals("/citas", redirect(liberado), "Una cita cancelada libera el horario")
        );
    }

    @Test
    @DisplayName("FTC013 - Rechazar cita con paciente o médico inexistente")
    void ftc013() {
        MedicoEntity m = medico();
        PacienteEntity p = paciente();
        long antes = citaRepo.count();
        CitaMedicaEntity c1 = new CitaMedicaEntity();
        c1.setFechaCita(DIA); c1.setHoraCita(LocalTime.of(8, 0)); c1.setMotivoConsulta("x");
        CitaMedicaEntity c2 = new CitaMedicaEntity();
        c2.setFechaCita(DIA); c2.setHoraCita(LocalTime.of(8, 0)); c2.setMotivoConsulta("x");
        EntityNotFoundException e1 = assertThrows(EntityNotFoundException.class,
                () -> citaMedicaService.registrarCitaMedica("00000001", m.getIdMedico(), c1));
        EntityNotFoundException e2 = assertThrows(EntityNotFoundException.class,
                () -> citaMedicaService.registrarCitaMedica(p.getDnipaciente(), 99999999L, c2));
        assertAll(
                () -> assertTrue(e1.getMessage().startsWith("No existe el paciente con DNI")),
                () -> assertTrue(e2.getMessage().startsWith("No existe el médico con ID")),
                () -> assertEquals(antes, citaRepo.count())
        );
    }

    @Test
    @DisplayName("FTC014 - Generar automáticamente el comprobante al registrar la cita")
    void ftc014() throws Exception {
        PacienteEntity p = paciente();
        MedicoEntity m = medico();
        registrarCita(p, m, DIA, "12:00");
        CitaMedicaEntity c = citaRepo.findByMedico_IdMedico(m.getIdMedico()).get(0);
        List<DetalleComprobanteEntity> det = detalleComprobanteRepo.findByIdReferencia(c.getCodCitaMedica()).stream()
                .filter(d -> d.getTipoItem() == TipoItemEnum.CITA).toList();
        assertEquals(1, det.size(), "Debe existir un detalle tipo CITA");
        ComprobantePagoEntity comp = det.get(0).getComprobantePago();
        MvcResult vistaCajero = mvc.perform(get("/comprobantes", sesion("CAJERO"))).andReturn();
        assertAll(
                () -> assertEquals(TipoComprobanteEnum.BOLETA, comp.getTipoComprobante()),
                () -> assertEquals(EstadoPagoEnum.FALTA_PAGAR, comp.getEstado()),
                () -> assertEquals(0, new BigDecimal("50.00").compareTo(comp.getMontoTotal())),
                () -> assertEquals(p.getDnipaciente(), comp.getPaciente().getDnipaciente()),
                () -> assertEquals("comprobantes/listar", vista(vistaCajero))
        );
    }

    @Test
    @DisplayName("FTC016 - Reprogramar cita a un horario libre")
    void ftc016() throws Exception {
        PacienteEntity p = paciente();
        MedicoEntity m = medico();
        CitaMedicaEntity c = cita(p, m, DIA, LocalTime.of(9, 0), EstadoCitaEnum.PENDIENTE);
        LocalDate nueva = DIA.plusDays(1);
        MvcResult r = mvc.perform(post("/citas/reprogramar/" + c.getCodCitaMedica(), sesion("ENFERMERA"))
                .param("nuevaFecha", nueva.toString()).param("nuevaHora", "15:30")).andReturn();
        CitaMedicaEntity act = citaRepo.findById(c.getCodCitaMedica()).orElseThrow();
        assertAll(
                () -> assertEquals("/citas", redirect(r)),
                () -> assertEquals(nueva, act.getFechaCita()),
                () -> assertEquals(LocalTime.of(15, 30), act.getHoraCita()),
                () -> assertEquals(EstadoCitaEnum.PENDIENTE, act.getEstadoCita())
        );
    }

    @Test
    @DisplayName("FTC017 - Rechazar reprogramación a un horario ocupado")
    void ftc017() throws Exception {
        PacienteEntity p = paciente();
        MedicoEntity m = medico();
        CitaMedicaEntity a = cita(p, m, DIA, LocalTime.of(9, 0), EstadoCitaEnum.PENDIENTE);
        cita(p, m, DIA, LocalTime.of(10, 0), EstadoCitaEnum.PENDIENTE);
        MvcResult r = mvc.perform(post("/citas/reprogramar/" + a.getCodCitaMedica(), sesion("ENFERMERA"))
                .param("nuevaFecha", DIA.toString()).param("nuevaHora", "10:00")).andReturn();
        CitaMedicaEntity act = citaRepo.findById(a.getCodCitaMedica()).orElseThrow();
        assertAll(
                () -> assertEquals("El médico no tiene disponibilidad en la nueva fecha y hora.", modelo(r, "error")),
                () -> assertEquals(LocalTime.of(9, 0), act.getHoraCita(), "La cita conserva su hora original")
        );
    }

    @Test
    @DisplayName("FTC018 - Cancelar una cita pendiente y liberar el horario")
    void ftc018() throws Exception {
        PacienteEntity p = paciente();
        MedicoEntity m = medico();
        CitaMedicaEntity c = cita(p, m, DIA, LocalTime.of(14, 0), EstadoCitaEnum.PENDIENTE);
        MvcResult r = mvc.perform(post("/citas/cancelar/" + c.getCodCitaMedica(), sesion("ENFERMERA"))).andReturn();
        MvcResult nueva = registrarCita(p, m, DIA, "14:00");
        assertAll(
                () -> assertEquals("Cita médica cancelada correctamente.", flash(r, "mensaje")),
                () -> assertEquals(EstadoCitaEnum.CANCELADA, citaRepo.findById(c.getCodCitaMedica()).orElseThrow().getEstadoCita()),
                () -> assertEquals("/citas", redirect(nueva), "El horario debe quedar disponible")
        );
    }

    @Test
    @DisplayName("FTC019 - Impedir transiciones de estado inválidas en citas")
    void ftc019() throws Exception {
        PacienteEntity p = paciente();
        MedicoEntity m = medico();
        CitaMedicaEntity fin = cita(p, m, DIA, LocalTime.of(8, 0), EstadoCitaEnum.FINALIZADA);
        CitaMedicaEntity can = cita(p, m, DIA, LocalTime.of(8, 30), EstadoCitaEnum.CANCELADA);
        MockHttpSession s = sesion("ENFERMERA");
        mvc.perform(post("/citas/cancelar/" + fin.getCodCitaMedica(), s)).andReturn();
        mvc.perform(post("/citas/reprogramar/" + can.getCodCitaMedica(), s)
                .param("nuevaFecha", DIA.plusDays(2).toString()).param("nuevaHora", "16:00")).andReturn();
        EstadoCitaEnum e1 = citaRepo.findById(fin.getCodCitaMedica()).orElseThrow().getEstadoCita();
        EstadoCitaEnum e2 = citaRepo.findById(can.getCodCitaMedica()).orElseThrow().getEstadoCita();
        assertAll(
                () -> assertEquals(EstadoCitaEnum.FINALIZADA, e1, "Una cita Finalizada no debería poder cancelarse (quedó " + e1 + ")"),
                () -> assertEquals(EstadoCitaEnum.CANCELADA, e2, "Una cita Cancelada no debería poder reprogramarse (quedó " + e2 + ")")
        );
    }

    @Test
    @DisplayName("FTC020 - Crear historia médica (médico y enfermera)")
    void ftc020() throws Exception {
        PacienteEntity p1 = paciente();
        PacienteEntity p2 = paciente();
        MvcResult r1 = mvc.perform(post("/historias/guardar", sesion("ENFERMERA"))
                .param("dniPaciente", p1.getDnipaciente()).param("antecedentesPersonales", "Asma")).andReturn();
        MvcResult r2 = mvc.perform(post("/historias/guardar", sesion("MEDICO"))
                .param("dniPaciente", p2.getDnipaciente()).param("antecedentesPersonales", "Ninguno")).andReturn();
        assertAll(
                () -> assertEquals("/historias", redirect(r1)),
                () -> assertEquals("/historias", redirect(r2)),
                () -> assertEquals(LocalDate.now(), historiaRepo.findByPaciente_Dnipaciente(p1.getDnipaciente()).orElseThrow().getFechaCreacion()),
                () -> assertEquals(LocalDate.now(), historiaRepo.findByPaciente_Dnipaciente(p2.getDnipaciente()).orElseThrow().getFechaCreacion())
        );
    }

    @Test
    @DisplayName("FTC021 - Impedir una segunda historia para el mismo paciente")
    void ftc021() throws Exception {
        PacienteEntity p = paciente();
        historia(p);
        MvcResult r = mvc.perform(post("/historias/guardar", sesion("MEDICO"))
                .param("dniPaciente", p.getDnipaciente())).andReturn();
        long cantidad = historiaRepo.findAll().stream()
                .filter(h -> h.getPaciente() != null && p.getDnipaciente().equals(h.getPaciente().getDnipaciente())).count();
        assertAll(
                () -> assertEquals("El paciente ya tiene una historia médica registrada.", modelo(r, "error")),
                () -> assertEquals(1, cantidad)
        );
    }

    @Test
    @DisplayName("FTC022 - Impedir que un rol no autorizado cree historias")
    void ftc022() throws Exception {
        assertTrue(denegado(mvc.perform(get("/historias/nuevo", sesion("CAJERO"))).andReturn()));
    }

    private boolean existeTriaje() {
        return existeClaseQueContenga("Entity", "triaje")
                || claseTieneCampo(HistoriaMedicaEntity.class, "triaje")
                || claseTieneCampo(CitaMedicaEntity.class, "triaje")
                || claseTieneCampo(CitaMedicaEntity.class, "presion");
    }

    @Test
    @DisplayName("FTC023 - Registrar el triaje de un paciente (enfermera)")
    void ftc023() throws Exception {
        MvcResult r = mvc.perform(get("/triaje/nuevo", sesion("ENFERMERA"))).andReturn();
        assertAll(
                () -> assertTrue(existeTriaje(), "No existe entidad ni campos de triaje (peso, talla, presión, temperatura, FC)"),
                () -> assertNotEquals("error-negocio", vista(r), "No existe ruta ni pantalla de triaje")
        );
    }

    @Test
    @DisplayName("FTC024 - Impedir que otro rol registre el triaje")
    void ftc024() {
        Assumptions.assumeTrue(existeTriaje(), "Bloqueado: depende de FTC023 (triaje no implementado)");
    }

    @Test
    @DisplayName("FTC025 - Habilitar consulta solo con triaje registrado")
    void ftc025() {
        Assumptions.assumeTrue(existeTriaje(), "Bloqueado: depende de FTC023 (triaje no implementado)");
    }

    @Test
    @DisplayName("FTC026 - Registrar evolución y actualizar historia (médico)")
    void ftc026() throws Exception {
        PacienteEntity p = paciente();
        HistoriaMedicaEntity h = historia(p);
        MvcResult r = mvc.perform(post("/historias/guardar", sesion("MEDICO"))
                .param("codHistoriaMedica", h.getCodHistoriaMedica().toString())
                .param("dniPaciente", p.getDnipaciente())
                .param("antecedentesPersonales", "Hipertensión").param("antecedentesFamiliares", "Diabetes (madre)")
                .param("intervencionesQuirurgicas", "Apendicectomía 2015")).andReturn();
        HistoriaMedicaEntity act = historiaRepo.findById(h.getCodHistoriaMedica()).orElseThrow();
        assertAll(
                () -> assertEquals("/historias", redirect(r)),
                () -> assertEquals("Hipertensión", act.getAntecedentesPersonales()),
                () -> assertEquals("Diabetes (madre)", act.getAntecedentesFamiliares()),
                () -> assertEquals("Apendicectomía 2015", act.getIntervencionesQuirurgicas()),
                () -> assertTrue(claseTieneCampo(HistoriaMedicaEntity.class, "evolucion"),
                        "La historia médica no tiene un campo para registrar la evolución del paciente")
        );
    }

    @Test
    @DisplayName("FTC027 - Impedir que la enfermera edite la historia médica")
    void ftc027() throws Exception {
        HistoriaMedicaEntity h = historia(paciente());
        MvcResult r = mvc.perform(get("/historias/editar/" + h.getCodHistoriaMedica(), sesion("ENFERMERA"))).andReturn();
        assertAll(
                () -> assertTrue(denegado(r)),
                () -> assertEquals("Solo el médico puede modificar historias médicas.", flash(r, "error"))
        );
    }

    @Test
    @DisplayName("FTC028 - Registrar diagnóstico asociado a una historia (médico)")
    void ftc028() throws Exception {
        HistoriaMedicaEntity h = historia(paciente());
        MvcResult r = mvc.perform(post("/diagnosticos/guardar", sesion("MEDICO"))
                .param("codHistoriaMedica", h.getCodHistoriaMedica().toString())
                .param("descripcionDiagnostico", "Faringitis aguda").param("gravedadDiagnostico", "LEVE")
                .param("fechaDiagnostico", "")).andReturn();
        List<DiagnosticoEntity> d = diagnosticoRepo.findByHistoriaMedica_CodHistoriaMedica(h.getCodHistoriaMedica());
        assertAll(
                () -> assertEquals("/diagnosticos", redirect(r)),
                () -> assertEquals(1, d.size()),
                () -> assertEquals(LocalDate.now(), d.get(0).getFechaDiagnostico())
        );
    }

    @Test
    @DisplayName("FTC029 - Impedir que otros roles registren diagnósticos")
    void ftc029() throws Exception {
        MvcResult bio = mvc.perform(get("/diagnosticos/nuevo", sesion("BIOLOGO"))).andReturn();
        MvcResult enf = mvc.perform(get("/diagnosticos/nuevo", sesion("ENFERMERA"))).andReturn();
        HistoriaMedicaEntity h = historia(paciente());
        mvc.perform(post("/diagnosticos/guardar", sesion("RADIOLOGO"))
                .param("codHistoriaMedica", h.getCodHistoriaMedica().toString())
                .param("descripcionDiagnostico", "Registrado por radiólogo").param("gravedadDiagnostico", "GRAVE")).andReturn();
        int creadosPorRadiologo = diagnosticoRepo.findByHistoriaMedica_CodHistoriaMedica(h.getCodHistoriaMedica()).size();
        assertAll(
                () -> assertTrue(denegado(bio), "BIOLOGO pudo abrir /diagnosticos/nuevo (vista: " + vista(bio) + ")"),
                () -> assertTrue(denegado(enf), "ENFERMERA pudo abrir /diagnosticos/nuevo"),
                () -> assertEquals(0, creadosPorRadiologo, "RADIOLOGO pudo registrar un diagnóstico")
        );
    }

    @Test
    @DisplayName("FTC030 - Verificar que no se puedan eliminar diagnósticos")
    void ftc030() throws Exception {
        DiagnosticoEntity d = diagnostico(historia(paciente()));
        MvcResult lista = mvc.perform(get("/diagnosticos", sesion("MEDICO"))).andReturn();
        boolean hayBoton = html(lista).contains("/diagnosticos/eliminar/");
        mvc.perform(post("/diagnosticos/eliminar/" + d.getIdDiagnostico(), sesion("MEDICO"))).andReturn();
        boolean sigue = diagnosticoRepo.findById(d.getIdDiagnostico()).isPresent();
        assertAll(
                () -> assertFalse(hayBoton, "El listado muestra la opción Eliminar"),
                () -> assertTrue(sigue, "El POST /diagnosticos/eliminar/{id} eliminó el diagnóstico")
        );
    }

    @Test
    @DisplayName("FTC031 - Consultar la historia clínica completa (línea de tiempo)")
    void ftc031() throws Exception {
        PacienteEntity p = paciente();
        MedicoEntity m = medico();
        HistoriaMedicaEntity h = historia(p);
        cita(p, m, LocalDate.now().minusDays(10), LocalTime.of(9, 0), EstadoCitaEnum.FINALIZADA);
        DiagnosticoEntity d = diagnostico(h);
        tratamiento(h, EstadoTratamientoEnum.ACTIVO);
        examen(d, TipoExamenEnum.LABORATORIO, EstadoExamenMedicoEnum.PENDIENTE);
        MvcResult r = mvc.perform(get("/pacientes/" + p.getDnipaciente() + "/historial", sesion("MEDICO"))).andReturn();
        HistorialClinicoDTO dto = (HistorialClinicoDTO) modelo(r, "historial");
        assertNotNull(dto, "No se construyó el historial");
        List<String> tipos = dto.getEventos().stream().map(EventoHistorialDTO::getTipo).toList();
        boolean ordenado = true;
        for (int i = 1; i < dto.getEventos().size(); i++) {
            if (dto.getEventos().get(i - 1).getFecha().isBefore(dto.getEventos().get(i).getFecha())) ordenado = false;
        }
        boolean finalOrdenado = ordenado;
        assertAll(
                () -> assertEquals("pacientes/historial", vista(r)),
                () -> assertTrue(tipos.contains("CITA"), "Falta la cita: " + tipos),
                () -> assertTrue(tipos.contains("DIAGNOSTICO"), "Falta el diagnóstico: " + tipos),
                () -> assertTrue(tipos.contains("TRATAMIENTO"), "Falta el tratamiento: " + tipos),
                () -> assertTrue(dto.getEventos().stream().anyMatch(e -> "Examen de laboratorio".equals(e.getTitulo())), "Falta el examen"),
                () -> assertTrue(finalOrdenado, "Los eventos deben estar en orden cronológico")
        );
    }

    @Test
    @DisplayName("FTC032 - Buscar historias por DNI o nombre del paciente")
    void ftc032() throws Exception {
        String apellido = "Huaman" + siguiente();
        PacienteEntity p = paciente("Jose", apellido);
        historia(p);
        MvcResult r1 = mvc.perform(get("/historias?q=" + p.getDnipaciente().substring(2), sesion("MEDICO"))).andReturn();
        MvcResult r2 = mvc.perform(get("/historias?q=" + apellido.toLowerCase(), sesion("MEDICO"))).andReturn();
        List<HistoriaMedicaEntity> l1 = lista(r1, "historias");
        List<HistoriaMedicaEntity> l2 = lista(r2, "historias");
        assertAll(
                () -> assertTrue(l1.stream().anyMatch(h -> h.getPaciente().getDnipaciente().equals(p.getDnipaciente()))),
                () -> assertEquals(1, l2.size()),
                () -> assertEquals(p.getDnipaciente(), l2.get(0).getPaciente().getDnipaciente())
        );
    }
}
