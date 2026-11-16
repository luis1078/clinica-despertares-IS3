package QA;

import Entity.*;
import Entity.Emuns.*;
import Service.dto.HistorialClinicoDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Exámenes de laboratorio y estudios por imágenes (FTC054–FTC069)")
class FTC04_ExamenesEImagenesTest extends SoporteQA {

    private MvcResult solicitarExamen(DiagnosticoEntity d, String tipo) throws Exception {
        return mvc.perform(post("/examenes/guardar", sesion("MEDICO"))
                .param("idDiagnostico", d.getIdDiagnostico().toString()).param("tipoExamen", tipo)
                .param("observaciones", "Solicitud QA")).andReturn();
    }

    private ComprobantePagoEntity comprobanteDeExamen(Long codExamen) {
        return detalleComprobanteRepo.findByIdReferencia(codExamen).stream()
                .filter(x -> x.getTipoItem() == TipoItemEnum.EXAMEN)
                .map(DetalleComprobanteEntity::getComprobantePago).findFirst().orElse(null);
    }

    @Test
    @DisplayName("FTC054 - Solicitar examen de laboratorio y generar comprobante pendiente")
    void ftc054() throws Exception {
        DiagnosticoEntity d = diagnosticoConPaciente(paciente());
        MvcResult r = solicitarExamen(d, "LABORATORIO");
        List<ExamenMedicoEntity> ex = examenRepo.findByDiagnostico_IdDiagnostico(d.getIdDiagnostico());
        assertEquals(1, ex.size());
        ComprobantePagoEntity c = comprobanteDeExamen(ex.get(0).getCodExamenMedico());
        assertAll(
                () -> assertEquals("/examenes", redirect(r)),
                () -> assertEquals(EstadoExamenMedicoEnum.PENDIENTE, ex.get(0).getEstadoExamenMedico()),
                () -> assertNotNull(c, "No se generó comprobante"),
                () -> assertEquals(EstadoPagoEnum.FALTA_PAGAR, c.getEstado()),
                () -> assertEquals(TipoComprobanteEnum.BOLETA, c.getTipoComprobante()),
                () -> assertEquals(0, new BigDecimal("80.00").compareTo(c.getMontoTotal()))
        );
    }

    @Test
    @DisplayName("FTC055 - Impedir que otros roles soliciten exámenes")
    void ftc055() throws Exception {
        MvcResult bio = mvc.perform(get("/examenes/nuevo", sesion("BIOLOGO"))).andReturn();
        MvcResult rad = mvc.perform(get("/examenes/nuevo", sesion("RADIOLOGO"))).andReturn();
        assertAll(
                () -> assertTrue(denegado(bio), "BIOLOGO pudo abrir /examenes/nuevo (vista: " + vista(bio) + ")"),
                () -> assertTrue(denegado(rad), "RADIOLOGO pudo abrir /examenes/nuevo (vista: " + vista(rad) + ")")
        );
    }

    @Test
    @DisplayName("FTC056 - Revertir la solicitud si el diagnóstico no tiene paciente")
    void ftc056() throws Exception {
        DiagnosticoEntity d = new DiagnosticoEntity();
        d.setDescripcionDiagnostico("Sin historia");
        d.setFechaDiagnostico(LocalDate.now());
        d.setGravedadDiagnostico(GravedadDiagnosticoEnum.LEVE);
        d = diagnosticoRepo.save(d);
        long comprobantesAntes = comprobanteRepo.count();
        MvcResult r = solicitarExamen(d, "LABORATORIO");
        int examenes = examenRepo.findByDiagnostico_IdDiagnostico(d.getIdDiagnostico()).size();
        assertAll(
                () -> assertTrue(String.valueOf(modelo(r, "error")).startsWith("El diagnóstico seleccionado no tiene un paciente asociado")),
                () -> assertEquals(0, examenes, "El examen no debe quedar guardado"),
                () -> assertEquals(comprobantesAntes, comprobanteRepo.count(), "No debe quedar comprobante")
        );
    }

    @Test
    @DisplayName("FTC057 - Verificar el estado de pago de una solicitud de examen")
    void ftc057() throws Exception {
        PacienteEntity p = paciente();
        DiagnosticoEntity d = diagnosticoConPaciente(p);
        ExamenLaboratorioEntity e = examenLaboratorio(d, EstadoExamenMedicoEnum.PENDIENTE);
        ComprobantePagoEntity c = comprobante(p, EstadoPagoEnum.FALTA_PAGAR);
        DetalleComprobanteEntity det = new DetalleComprobanteEntity();
        det.setComprobantePago(c); det.setTipoItem(TipoItemEnum.EXAMEN); det.setIdReferencia(e.getCodExamenMedico());
        det.setDescripcionDetalle("Examen"); det.setCantidad(1); det.setPrecioUnitario(new BigDecimal("80")); det.setSubtotal(new BigDecimal("80"));
        detalleComprobanteRepo.save(det);
        MvcResult r = mvc.perform(get("/laboratorio?buscar=" + p.getDnipaciente(), sesion("BIOLOGO"))).andReturn();
        MvcResult comp = mvc.perform(get("/comprobantes", sesion("BIOLOGO"))).andReturn();
        String h = html(r);
        boolean muestraPago = h.contains("FALTA_PAGAR") || h.contains("Falta pagar") || h.contains("Pagado") || h.contains("Estado de pago");
        assertAll(
                () -> assertTrue(muestraPago, "La vista de laboratorio no muestra el estado de pago de la solicitud"),
                () -> assertFalse(denegado(comp), "El biólogo tampoco puede consultar /comprobantes")
        );
    }

    @Test
    @DisplayName("FTC058 - Biólogo cambia el examen a En proceso y Finalizado")
    void ftc058() throws Exception {
        ExamenLaboratorioEntity e = examenLaboratorio(diagnosticoConPaciente(paciente()), EstadoExamenMedicoEnum.PENDIENTE);
        MockHttpSession s = sesion("BIOLOGO");
        mvc.perform(post("/examenes/proceso/" + e.getCodExamenMedico(), s)).andReturn();
        EstadoExamenMedicoEnum e1 = examenRepo.findById(e.getCodExamenMedico()).orElseThrow().getEstadoExamenMedico();
        mvc.perform(post("/examenes/finalizar/" + e.getCodExamenMedico(), s)).andReturn();
        ExamenMedicoEntity fin = examenRepo.findById(e.getCodExamenMedico()).orElseThrow();
        assertAll(
                () -> assertEquals(EstadoExamenMedicoEnum.EN_PROCESO, e1),
                () -> assertEquals(EstadoExamenMedicoEnum.FINALIZADO, fin.getEstadoExamenMedico()),
                () -> assertEquals(LocalDate.now(), fin.getFechaResultado())
        );
    }

    @Test
    @DisplayName("FTC059 - Rechazar cambios de estado por otros roles o transiciones inválidas")
    void ftc059() throws Exception {
        DiagnosticoEntity d = diagnosticoConPaciente(paciente());
        ExamenLaboratorioEntity pend1 = examenLaboratorio(d, EstadoExamenMedicoEnum.PENDIENTE);
        ExamenLaboratorioEntity fin = examenLaboratorio(d, EstadoExamenMedicoEnum.FINALIZADO);
        ExamenLaboratorioEntity pend2 = examenLaboratorio(d, EstadoExamenMedicoEnum.PENDIENTE);
        mvc.perform(post("/examenes/proceso/" + pend1.getCodExamenMedico(), sesion("MEDICO"))).andReturn();
        mvc.perform(post("/examenes/proceso/" + fin.getCodExamenMedico(), sesion("BIOLOGO"))).andReturn();
        mvc.perform(post("/examenes/finalizar/" + pend2.getCodExamenMedico(), sesion("BIOLOGO"))).andReturn();
        EstadoExamenMedicoEnum a = examenRepo.findById(pend1.getCodExamenMedico()).orElseThrow().getEstadoExamenMedico();
        EstadoExamenMedicoEnum b = examenRepo.findById(fin.getCodExamenMedico()).orElseThrow().getEstadoExamenMedico();
        EstadoExamenMedicoEnum c = examenRepo.findById(pend2.getCodExamenMedico()).orElseThrow().getEstadoExamenMedico();
        assertAll(
                () -> assertEquals(EstadoExamenMedicoEnum.PENDIENTE, a, "El MÉDICO cambió el estado a " + a),
                () -> assertEquals(EstadoExamenMedicoEnum.FINALIZADO, b, "Un examen FINALIZADO volvió a " + b),
                () -> assertEquals(EstadoExamenMedicoEnum.PENDIENTE, c, "Un examen PENDIENTE pasó directo a " + c)
        );
    }

    @Test
    @DisplayName("FTC060 - Registrar resultado de examen (biólogo)")
    void ftc060() throws Exception {
        ExamenLaboratorioEntity e = examenLaboratorio(diagnosticoConPaciente(paciente()), EstadoExamenMedicoEnum.EN_PROCESO);
        MvcResult r = mvc.perform(post("/laboratorio/guardar", sesion("BIOLOGO"))
                .param("codExamenMedico", e.getCodExamenMedico().toString())
                .param("idDiagnostico", e.getDiagnostico().getIdDiagnostico().toString())
                .param("nombreMuestra", "Sangre venosa").param("descripcionResultado", "Hemoglobina 13.5")
                .param("valorReferencia", "13.0").param("unidadMedida", "g/dL").param("observaciones", "Normal")).andReturn();
        ExamenLaboratorioEntity act = laboratorioRepo.findById(e.getCodExamenMedico()).orElseThrow();
        assertAll(
                () -> assertEquals("/laboratorio", redirect(r)),
                () -> assertEquals("Hemoglobina 13.5", act.getDescripcionResultado()),
                () -> assertEquals("g/dL", act.getUnidadMedida()),
                () -> assertEquals(EstadoExamenMedicoEnum.FINALIZADO, act.getEstadoExamenMedico()),
                () -> assertEquals(LocalDate.now(), act.getFechaResultado())
        );
    }

    @Test
    @DisplayName("FTC061 - Impedir que otros roles registren resultados")
    void ftc061() throws Exception {
        MvcResult med = mvc.perform(get("/laboratorio/nuevo", sesion("MEDICO"))).andReturn();
        MvcResult rad = mvc.perform(get("/laboratorio/nuevo", sesion("RADIOLOGO"))).andReturn();
        assertAll(() -> assertTrue(denegado(med)), () -> assertTrue(denegado(rad)));
    }

    @Test
    @DisplayName("FTC062 - Vincular a la historia solo resultados validados")
    void ftc062() {
        boolean hayValidacion = claseTieneCampo(ExamenMedicoEntity.class, "valid")
                || java.util.Arrays.stream(EstadoExamenMedicoEnum.values()).anyMatch(v -> v.name().contains("VALID"));
        assertTrue(hayValidacion, "No existe estado ni marca de 'validado' para los resultados de examen");
    }

    @Test
    @DisplayName("FTC063 - Consultar resultados de laboratorio por paciente (médico y biólogo)")
    void ftc063() throws Exception {
        PacienteEntity p = paciente();
        ExamenLaboratorioEntity e = examenLaboratorio(diagnosticoConPaciente(p), EstadoExamenMedicoEnum.FINALIZADO);
        MvcResult med = mvc.perform(get("/laboratorio?buscar=" + p.getDnipaciente(), sesion("MEDICO"))).andReturn();
        MvcResult bio = mvc.perform(get("/laboratorio?buscar=" + p.getDnipaciente(), sesion("BIOLOGO"))).andReturn();
        List<ExamenLaboratorioEntity> lm = lista(med, "examenes");
        List<ExamenLaboratorioEntity> lb = lista(bio, "examenes");
        assertAll(
                () -> assertTrue(lm.stream().anyMatch(x -> x.getCodExamenMedico().equals(e.getCodExamenMedico()))),
                () -> assertTrue(lb.stream().anyMatch(x -> x.getCodExamenMedico().equals(e.getCodExamenMedico()))),
                () -> assertTrue(denegado(mvc.perform(get("/laboratorio", sesion("ENFERMERA"))).andReturn())),
                () -> assertTrue(denegado(mvc.perform(get("/laboratorio", sesion("CAJERO"))).andReturn())),
                () -> assertTrue(denegado(mvc.perform(get("/laboratorio", sesion("RADIOLOGO"))).andReturn()))
        );
    }

    @Test
    @DisplayName("FTC064 - Solicitar estudio por imágenes y generar comprobante pendiente")
    void ftc064() throws Exception {
        DiagnosticoEntity d = diagnosticoConPaciente(paciente());
        solicitarExamen(d, "IMAGEN");
        List<ExamenMedicoEntity> ex = examenRepo.findByDiagnostico_IdDiagnostico(d.getIdDiagnostico());
        assertEquals(1, ex.size());
        ComprobantePagoEntity c = comprobanteDeExamen(ex.get(0).getCodExamenMedico());
        assertAll(
                () -> assertEquals(TipoExamenEnum.IMAGEN, ex.get(0).getTipoExamen()),
                () -> assertEquals(EstadoExamenMedicoEnum.PENDIENTE, ex.get(0).getEstadoExamenMedico()),
                () -> assertNotNull(c),
                () -> assertEquals(EstadoPagoEnum.FALTA_PAGAR, c.getEstado())
        );
    }

    @Test
    @DisplayName("FTC065 - Registrar tipo de imagen y región evaluada")
    void ftc065() throws Exception {
        DiagnosticoEntity d = diagnosticoConPaciente(paciente());
        MvcResult r = mvc.perform(post("/imagenes/guardar", sesion("RADIOLOGO"))
                .param("idDiagnostico", d.getIdDiagnostico().toString()).param("nombreImagen", "Rayos X")
                .param("regionCuerpo", "Tórax").param("informeMedico", "Campos pulmonares libres")
                .param("usaContraste", "false")).andReturn();
        List<ExamenMedicoEntity> ex = examenRepo.findByDiagnostico_IdDiagnostico(d.getIdDiagnostico());
        MvcResult form = mvc.perform(get("/imagenes/nuevo", sesion("RADIOLOGO"))).andReturn();
        String h = html(form);
        int idx = h.indexOf("name=\"regionCuerpo\"");
        String etiquetaRegion = idx < 0 ? "" : h.substring(h.lastIndexOf("<input", idx), h.indexOf(">", idx) + 1);
        assertAll(
                () -> assertEquals("/imagenes", redirect(r)),
                () -> assertEquals(1, ex.size()),
                () -> assertEquals("Tórax", ((ImagenDiagnosticaEntity) imagenRepo.findById(ex.get(0).getCodExamenMedico()).orElseThrow()).getRegionCuerpo()),
                () -> assertTrue(etiquetaRegion.contains("required"), "El campo región debe ser obligatorio en el formulario: " + etiquetaRegion)
        );
    }

    @Test
    @DisplayName("FTC066 - Estado del estudio entre En proceso y Finalizado (radiólogo)")
    void ftc066() throws Exception {
        DiagnosticoEntity d = diagnosticoConPaciente(paciente());
        ImagenDiagnosticaEntity i1 = imagen(d, EstadoExamenMedicoEnum.PENDIENTE);
        ImagenDiagnosticaEntity i2 = imagen(d, EstadoExamenMedicoEnum.PENDIENTE);
        MockHttpSession rad = sesion("RADIOLOGO");
        mvc.perform(post("/examenes/proceso/" + i1.getCodExamenMedico(), rad)).andReturn();
        EstadoExamenMedicoEnum a = examenRepo.findById(i1.getCodExamenMedico()).orElseThrow().getEstadoExamenMedico();
        mvc.perform(post("/examenes/finalizar/" + i1.getCodExamenMedico(), rad)).andReturn();
        EstadoExamenMedicoEnum b = examenRepo.findById(i1.getCodExamenMedico()).orElseThrow().getEstadoExamenMedico();
        mvc.perform(post("/examenes/proceso/" + i2.getCodExamenMedico(), sesion("BIOLOGO"))).andReturn();
        EstadoExamenMedicoEnum c = examenRepo.findById(i2.getCodExamenMedico()).orElseThrow().getEstadoExamenMedico();
        assertAll(
                () -> assertEquals(EstadoExamenMedicoEnum.EN_PROCESO, a),
                () -> assertEquals(EstadoExamenMedicoEnum.FINALIZADO, b),
                () -> assertEquals(EstadoExamenMedicoEnum.PENDIENTE, c, "El BIÓLOGO cambió el estado de un estudio de imágenes a " + c)
        );
    }

    @Test
    @DisplayName("FTC067 - Cargar archivo de imagen y registrar informe interpretativo")
    void ftc067() {
        boolean hayArchivo = claseTieneCampo(ImagenDiagnosticaEntity.class, "archivo")
                || claseTieneCampo(ImagenDiagnosticaEntity.class, "ruta")
                || claseTieneCampo(ImagenDiagnosticaEntity.class, "bytes")
                || claseTieneCampo(ImagenDiagnosticaEntity.class, "url");
        assertTrue(hayArchivo, "ImagenDiagnosticaEntity solo guarda el nombre de la imagen; no hay carga de archivos");
    }

    @Test
    @DisplayName("FTC068 - Vincular las imágenes con la historia médica")
    void ftc068() throws Exception {
        PacienteEntity p = paciente();
        imagen(diagnosticoConPaciente(p), EstadoExamenMedicoEnum.FINALIZADO);
        MvcResult r = mvc.perform(get("/pacientes/" + p.getDnipaciente() + "/historial", sesion("MEDICO"))).andReturn();
        HistorialClinicoDTO dto = (HistorialClinicoDTO) modelo(r, "historial");
        assertTrue(dto.getEventos().stream().anyMatch(e -> "Imagen diagnóstica".equals(e.getTitulo())),
                "El estudio no aparece en la línea de tiempo del paciente");
    }

    @Test
    @DisplayName("FTC069 - Consultar informes de imágenes por paciente (médico y radiólogo)")
    void ftc069() throws Exception {
        PacienteEntity p = paciente();
        ImagenDiagnosticaEntity i = imagen(diagnosticoConPaciente(p), EstadoExamenMedicoEnum.FINALIZADO);
        List<ImagenDiagnosticaEntity> lm = lista(mvc.perform(get("/imagenes?buscar=" + p.getDnipaciente(), sesion("MEDICO"))).andReturn(), "imagenes");
        List<ImagenDiagnosticaEntity> lr = lista(mvc.perform(get("/imagenes?buscar=" + p.getDnipaciente(), sesion("RADIOLOGO"))).andReturn(), "imagenes");
        assertAll(
                () -> assertTrue(lm.stream().anyMatch(x -> x.getCodExamenMedico().equals(i.getCodExamenMedico()))),
                () -> assertTrue(lr.stream().anyMatch(x -> x.getCodExamenMedico().equals(i.getCodExamenMedico()))),
                () -> assertTrue(denegado(mvc.perform(get("/imagenes", sesion("BIOLOGO"))).andReturn())),
                () -> assertTrue(denegado(mvc.perform(get("/imagenes", sesion("ENFERMERA"))).andReturn())),
                () -> assertTrue(denegado(mvc.perform(get("/imagenes", sesion("CAJERO"))).andReturn()))
        );
    }
}
