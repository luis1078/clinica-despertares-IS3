package QA;

import Entity.*;
import Entity.Emuns.*;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tratamientos, comprobantes de pago e inventario de medicamentos (FTC033–FTC053)")
class FTC03_TratamientosComprobantesInventarioTest extends SoporteQA {

    // ---------- Tratamientos ----------

    @Test
    @DisplayName("FTC033 - Registrar tratamiento y su detalle (medicamento, dosis, frecuencia)")
    void ftc033() throws Exception {
        HistoriaMedicaEntity h = historia(paciente());
        MedicamentoEntity med = medicamento(50);
        MockHttpSession s = sesion("MEDICO");
        MvcResult r1 = mvc.perform(post("/tratamientos/guardar", s)
                .param("codHistoriaMedica", h.getCodHistoriaMedica().toString())
                .param("descripcionTratamiento", "Antibioticoterapia").param("fechaInicio", LocalDate.now().toString())
                .param("fechaFin", LocalDate.now().plusDays(7).toString()).param("tipoTratamiento", "Farmacológico")).andReturn();
        List<TratamientoEntity> ts = tratamientoRepo.findByHistoriaMedica_CodHistoriaMedica(h.getCodHistoriaMedica());
        assertEquals(1, ts.size(), "Debe registrarse el tratamiento");
        TratamientoEntity t = ts.get(0);
        MvcResult r2 = mvc.perform(post("/detalle-tratamientos/guardar", s)
                .param("idTratamiento", t.getIdTratamiento().toString())
                .param("codMedicamento", med.getCodMedicamento().toString())
                .param("dosisIndicada", "500 mg").param("frecuenciaDeToma", "Cada 8 horas")).andReturn();
        List<DetalleTratamientoEntity> det = detalleTratamientoRepo.findAll().stream()
                .filter(d -> d.getTratamiento() != null && d.getTratamiento().getIdTratamiento().equals(t.getIdTratamiento())).toList();
        assertAll(
                () -> assertEquals("/tratamientos", redirect(r1)),
                () -> assertEquals(EstadoTratamientoEnum.ACTIVO, t.getEstadoTratamiento()),
                () -> assertEquals("/detalle-tratamientos", redirect(r2)),
                () -> assertEquals(1, det.size()),
                () -> assertEquals(med.getCodMedicamento(), det.get(0).getMedicamento().getCodMedicamento()),
                () -> assertEquals("500 mg", det.get(0).getDosisIndicada())
        );
    }

    @Test
    @DisplayName("FTC034 - Actualizar el estado del tratamiento")
    void ftc034() throws Exception {
        HistoriaMedicaEntity h = historia(paciente());
        TratamientoEntity t1 = tratamiento(h, EstadoTratamientoEnum.ACTIVO);
        TratamientoEntity t2 = tratamiento(h, EstadoTratamientoEnum.ACTIVO);
        MockHttpSession s = sesion("MEDICO");
        mvc.perform(post("/tratamientos/finalizar/" + t1.getIdTratamiento(), s)).andReturn();
        mvc.perform(post("/tratamientos/suspender/" + t2.getIdTratamiento(), s)).andReturn();
        EstadoTratamientoEnum e1 = tratamientoRepo.findById(t1.getIdTratamiento()).orElseThrow().getEstadoTratamiento();
        EstadoTratamientoEnum e2 = tratamientoRepo.findById(t2.getIdTratamiento()).orElseThrow().getEstadoTratamiento();
        mvc.perform(post("/tratamientos/suspender/" + t1.getIdTratamiento(), s)).andReturn();
        EstadoTratamientoEnum e3 = tratamientoRepo.findById(t1.getIdTratamiento()).orElseThrow().getEstadoTratamiento();
        assertAll(
                () -> assertEquals(EstadoTratamientoEnum.FINALIZADO, e1),
                () -> assertEquals(EstadoTratamientoEnum.SUSPENDIDO, e2),
                () -> assertEquals(EstadoTratamientoEnum.FINALIZADO, e3,
                        "Un tratamiento FINALIZADO no debería poder suspenderse (quedó " + e3 + ")")
        );
    }

    @Test
    @DisplayName("FTC035 - Impedir eliminar tratamientos y modificarlos con un rol distinto al médico")
    void ftc035() throws Exception {
        TratamientoEntity t = tratamiento(historia(paciente()), EstadoTratamientoEnum.ACTIVO);
        MvcResult farm = mvc.perform(get("/tratamientos/nuevo", sesion("FARMACEUTICO"))).andReturn();
        mvc.perform(post("/tratamientos/eliminar/" + t.getIdTratamiento(), sesion("MEDICO"))).andReturn();
        boolean sigue = tratamientoRepo.findById(t.getIdTratamiento()).isPresent();
        assertAll(
                () -> assertTrue(denegado(farm), "FARMACEUTICO pudo abrir el formulario de tratamientos (vista: " + vista(farm) + ")"),
                () -> assertTrue(sigue, "El tratamiento fue eliminado con POST /tratamientos/eliminar/{id}")
        );
    }

    // ---------- Comprobantes ----------

    @Test
    @DisplayName("FTC036 - Registrar comprobante de pago (cajero)")
    void ftc036() throws Exception {
        PacienteEntity p = paciente();
        MvcResult r = mvc.perform(post("/comprobantes/guardar", sesion("CAJERO"))
                .param("dniPaciente", p.getDnipaciente()).param("tipoComprobante", "BOLETA")
                .param("metodoPago", "EFECTIVO")).andReturn();
        List<ComprobantePagoEntity> cs = comprobanteRepo.findByPaciente_Dnipaciente(p.getDnipaciente());
        assertEquals(1, cs.size());
        ComprobantePagoEntity c = cs.get(0);
        assertAll(
                () -> assertEquals("/comprobantes", redirect(r)),
                () -> assertEquals(EstadoPagoEnum.FALTA_PAGAR, c.getEstado()),
                () -> assertEquals(LocalDate.now(), c.getFechaEmision()),
                () -> assertEquals(0, BigDecimal.ZERO.compareTo(c.getMontoTotal()))
        );
    }

    @Test
    @DisplayName("FTC037 - Impedir que otro rol registre comprobantes")
    void ftc037() throws Exception {
        assertTrue(denegado(mvc.perform(get("/comprobantes/nuevo", sesion("MEDICO"))).andReturn()));
    }

    private MvcResult agregarDetalle(ComprobantePagoEntity c, String tipo, String paramRef, Long ref, int cantidad, String precio) throws Exception {
        var b = post("/detalle-comprobantes/guardar", sesion("CAJERO"))
                .param("codComprobante", c.getCodcomprobante().toString()).param("tipoItem", tipo)
                .param(paramRef, ref.toString()).param("descripcionDetalle", "Detalle " + tipo)
                .param("cantidad", String.valueOf(cantidad));
        if (precio != null) b = b.param("precioUnitario", precio);
        return mvc.perform(b).andReturn();
    }

    private MvcResult editarDetalle(DetalleComprobanteEntity d, ComprobantePagoEntity c, MedicamentoEntity m, int cantidad, String precio) throws Exception {
        return mvc.perform(post("/detalle-comprobantes/guardar", sesion("CAJERO"))
                .param("iddetalle", d.getIddetalle().toString())
                .param("codComprobante", c.getCodcomprobante().toString()).param("tipoItem", "MEDICAMENTO")
                .param("codMedicamento", m.getCodMedicamento().toString()).param("descripcionDetalle", "Detalle editado")
                .param("cantidad", String.valueOf(cantidad)).param("precioUnitario", precio)).andReturn();
    }

    @Test
    @DisplayName("FTC038 - Registrar detalle por ítem (cita, examen y medicamento)")
    void ftc038() throws Exception {
        PacienteEntity p = paciente();
        ComprobantePagoEntity c = comprobante(p, EstadoPagoEnum.FALTA_PAGAR);
        CitaMedicaEntity cita = cita(p, medico(), LocalDate.now().plusDays(5), LocalTime.of(9, 0), EstadoCitaEnum.PENDIENTE);
        ExamenMedicoEntity ex = examen(diagnosticoConPaciente(p), TipoExamenEnum.LABORATORIO, EstadoExamenMedicoEnum.PENDIENTE);
        MedicamentoEntity med = medicamento(30);
        agregarDetalle(c, "CITA", "codCitaMedica", cita.getCodCitaMedica(), 1, "50.00");
        agregarDetalle(c, "EXAMEN", "codExamenMedico", ex.getCodExamenMedico(), 1, "80.00");
        agregarDetalle(c, "MEDICAMENTO", "codMedicamento", med.getCodMedicamento(), 1, "12.00");
        List<DetalleComprobanteEntity> d = detallesDe(c);
        assertAll(
                () -> assertEquals(3, d.size()),
                () -> assertTrue(d.stream().anyMatch(x -> x.getTipoItem() == TipoItemEnum.CITA && x.getIdReferencia().equals(cita.getCodCitaMedica()))),
                () -> assertTrue(d.stream().anyMatch(x -> x.getTipoItem() == TipoItemEnum.EXAMEN && x.getIdReferencia().equals(ex.getCodExamenMedico()))),
                () -> assertTrue(d.stream().anyMatch(x -> x.getTipoItem() == TipoItemEnum.MEDICAMENTO && x.getIdReferencia().equals(med.getCodMedicamento())))
        );
    }

    @Test
    @DisplayName("FTC039 - Rechazar detalle duplicado para la misma cita; medicamentos sí se repiten")
    void ftc039() throws Exception {
        PacienteEntity p = paciente();
        ComprobantePagoEntity c = comprobante(p, EstadoPagoEnum.FALTA_PAGAR);
        CitaMedicaEntity cita = cita(p, medico(), LocalDate.now().plusDays(5), LocalTime.of(9, 0), EstadoCitaEnum.PENDIENTE);
        MedicamentoEntity med = medicamento(30);
        agregarDetalle(c, "CITA", "codCitaMedica", cita.getCodCitaMedica(), 1, "50.00");
        MvcResult dup = agregarDetalle(c, "CITA", "codCitaMedica", cita.getCodCitaMedica(), 1, "50.00");
        agregarDetalle(c, "MEDICAMENTO", "codMedicamento", med.getCodMedicamento(), 1, "5.00");
        agregarDetalle(c, "MEDICAMENTO", "codMedicamento", med.getCodMedicamento(), 1, "5.00");
        long meds = detallesDe(c).stream().filter(x -> x.getTipoItem() == TipoItemEnum.MEDICAMENTO).count();
        long citas = detallesDe(c).stream().filter(x -> x.getTipoItem() == TipoItemEnum.CITA).count();
        assertAll(
                () -> assertEquals("Ya existe un detalle registrado para el ítem seleccionado.", flash(dup, "error")),
                () -> assertEquals(1, citas),
                () -> assertEquals(2, meds)
        );
    }

    @Test
    @DisplayName("FTC040 - Calcular subtotal por detalle y total del comprobante")
    void ftc040() throws Exception {
        PacienteEntity p = paciente();
        ComprobantePagoEntity c = comprobante(p, EstadoPagoEnum.FALTA_PAGAR);
        MedicamentoEntity med = medicamento(100);
        CitaMedicaEntity cita = cita(p, medico(), LocalDate.now().plusDays(5), LocalTime.of(9, 0), EstadoCitaEnum.PENDIENTE);

        agregarDetalle(c, "MEDICAMENTO", "codMedicamento", med.getCodMedicamento(), 2, "15.50");
        BigDecimal t1 = totalDe(c);
        agregarDetalle(c, "CITA", "codCitaMedica", cita.getCodCitaMedica(), 1, "50.00");
        BigDecimal t2 = totalDe(c);
        DetalleComprobanteEntity dMed = detallesDe(c).stream().filter(x -> x.getTipoItem() == TipoItemEnum.MEDICAMENTO).findFirst().orElseThrow();
        DetalleComprobanteEntity dCita = detallesDe(c).stream().filter(x -> x.getTipoItem() == TipoItemEnum.CITA).findFirst().orElseThrow();
        BigDecimal sub1 = dMed.getSubtotal();
        editarDetalle(dMed, c, med, 3, "15.50");
        BigDecimal t3 = totalDe(c);
        mvc.perform(post("/detalle-comprobantes/eliminar/" + dCita.getIddetalle(), sesion("CAJERO"))).andReturn();
        BigDecimal t4 = totalDe(c);
        assertAll(
                () -> assertEquals(0, new BigDecimal("31.00").compareTo(sub1), "Subtotal 2 × 15.50 = " + sub1),
                () -> assertEquals(0, new BigDecimal("31.00").compareTo(t1), "Paso 1 total = " + t1),
                () -> assertEquals(0, new BigDecimal("81.00").compareTo(t2), "Paso 2 total = " + t2),
                () -> assertEquals(0, new BigDecimal("96.50").compareTo(t3), "Paso 3 total = " + t3),
                () -> assertEquals(0, new BigDecimal("46.50").compareTo(t4), "Paso 4 total = " + t4)
        );
    }

    @Test
    @DisplayName("FTC041 - Validar valores límite de cantidad")
    void ftc041() throws Exception {
        PacienteEntity p = paciente();
        ComprobantePagoEntity c = comprobante(p, EstadoPagoEnum.FALTA_PAGAR);
        CitaMedicaEntity c1 = cita(p, medico(), LocalDate.now().plusDays(5), LocalTime.of(9, 0), EstadoCitaEnum.PENDIENTE);
        MvcResult cero = agregarDetalle(c, "CITA", "codCitaMedica", c1.getCodCitaMedica(), 0, "50.00");
        MvcResult negativa = agregarDetalle(c, "CITA", "codCitaMedica", c1.getCodCitaMedica(), -1, "50.00");
        int antes = detallesDe(c).size();
        MvcResult sinPrecio = agregarDetalle(c, "CITA", "codCitaMedica", c1.getCodCitaMedica(), 1, "");
        List<DetalleComprobanteEntity> d = detallesDe(c);
        assertAll(
                () -> assertEquals("La cantidad debe ser mayor a cero.", flash(cero, "error")),
                () -> assertEquals("La cantidad debe ser mayor a cero.", flash(negativa, "error")),
                () -> assertEquals(0, antes),
                () -> assertEquals(1, d.size(), "Con precio vacío debe guardarse"),
                () -> assertEquals(0, BigDecimal.ZERO.compareTo(d.get(0).getSubtotal()))
        );
    }

    @Test
    @DisplayName("FTC042 - Procesar pago en efectivo")
    void ftc042() throws Exception {
        ComprobantePagoEntity c = comprobante(paciente(), EstadoPagoEnum.FALTA_PAGAR);
        MvcResult r = mvc.perform(post("/comprobantes/cancelar/" + c.getCodcomprobante(), sesion("CAJERO"))).andReturn();
        EstadoPagoEnum e = comprobanteRepo.findById(c.getCodcomprobante()).orElseThrow().getEstado();
        System.out.println("[QA] FTC042 estado tras pagar: " + e + " (el sistema usa CANCELADO como equivalente a 'Pagado')");
        assertAll(
                () -> assertEquals("/comprobantes", redirect(r)),
                () -> assertEquals(EstadoPagoEnum.CANCELADO, e)
        );
    }

    @Test
    @DisplayName("FTC043 - Procesar pago con tarjeta mediante POS")
    void ftc043() {
        boolean hayPos = existeClaseQueContenga(null, "pos") || existeClaseQueContenga(null, "pasarela")
                || existeClaseQueContenga(null, "transaccion");
        assertTrue(hayPos, "No existe integración con POS ni registro de transacciones aprobadas/rechazadas");
    }

    @Test
    @DisplayName("FTC044 - Consultar el estado de pago de comprobantes de cualquier tipo")
    void ftc044() throws Exception {
        PacienteEntity p = paciente();
        comprobante(p, EstadoPagoEnum.FALTA_PAGAR);
        comprobante(p, EstadoPagoEnum.CANCELADO);
        MvcResult r1 = mvc.perform(get("/comprobantes?estado=FALTA_PAGAR", sesion("CAJERO"))).andReturn();
        MvcResult r2 = mvc.perform(get("/comprobantes?estado=CANCELADO", sesion("CAJERO"))).andReturn();
        List<ComprobantePagoEntity> l1 = lista(r1, "comprobantes");
        List<ComprobantePagoEntity> l2 = lista(r2, "comprobantes");
        assertAll(
                () -> assertFalse(l1.isEmpty()),
                () -> assertTrue(l1.stream().allMatch(x -> x.getEstado() == EstadoPagoEnum.FALTA_PAGAR)),
                () -> assertFalse(l2.isEmpty()),
                () -> assertTrue(l2.stream().allMatch(x -> x.getEstado() == EstadoPagoEnum.CANCELADO))
        );
    }

    // ---------- Medicamentos ----------

    @Test
    @DisplayName("FTC045 - Registrar y editar medicamento (farmacéutico)")
    void ftc045() throws Exception {
        String nombre = "QA Reg " + siguiente();
        String ruc = proveedorRepo.findAll().get(0).getRucProveedor();
        MockHttpSession s = sesion("FARMACEUTICO");
        mvc.perform(post("/medicamentos/guardar", s).param("rucProveedor", ruc).param("nombreMedicamento", nombre)
                .param("descripcionMedicamento", "Original").param("tipoMedicamento", "Analgésico")
                .param("fechaVencimiento", LocalDate.now().plusYears(1).toString())
                .param("stockInventario", "40").param("cantidadMinima", "10")).andReturn();
        MedicamentoEntity m = medicamentoRepo.findByNombreMedicamento(nombre).orElseThrow();
        mvc.perform(post("/medicamentos/guardar", s).param("codMedicamento", m.getCodMedicamento().toString())
                .param("rucProveedor", ruc).param("nombreMedicamento", nombre)
                .param("descripcionMedicamento", "Editada").param("tipoMedicamento", "Analgésico")
                .param("fechaVencimiento", LocalDate.now().plusYears(1).toString())
                .param("stockInventario", "40").param("cantidadMinima", "10")).andReturn();
        MedicamentoEntity act = medicamentoRepo.findById(m.getCodMedicamento()).orElseThrow();
        assertAll(
                () -> assertEquals(40, act.getStockInventario()),
                () -> assertEquals("Editada", act.getDescripcionMedicamento())
        );
    }

    @Test
    @DisplayName("FTC046 - Permitir solo consulta de medicamentos a otros roles")
    void ftc046() throws Exception {
        MvcResult lista = mvc.perform(get("/medicamentos", sesion("CAJERO"))).andReturn();
        MvcResult nuevo = mvc.perform(get("/medicamentos/nuevo", sesion("CAJERO"))).andReturn();
        assertAll(
                () -> assertEquals("medicamentos/listar", vista(lista)),
                () -> assertTrue(denegado(nuevo)),
                () -> assertEquals("Solo el farmacéutico puede registrar medicamentos.", flash(nuevo, "error"))
        );
    }

    @Test
    @DisplayName("FTC047 - Actualizar stock con valores válidos")
    void ftc047() throws Exception {
        MedicamentoEntity m = medicamento(20);
        mvc.perform(post("/medicamentos/stock/" + m.getCodMedicamento(), sesion("FARMACEUTICO")).param("nuevaCantidad", "0")).andReturn();
        int s1 = stockDe(m);
        mvc.perform(post("/medicamentos/stock/" + m.getCodMedicamento(), sesion("FARMACEUTICO")).param("nuevaCantidad", "100")).andReturn();
        int s2 = stockDe(m);
        assertAll(() -> assertEquals(0, s1), () -> assertEquals(100, s2));
    }

    @Test
    @DisplayName("FTC048 - Rechazar stock negativo y roles no autorizados")
    void ftc048() throws Exception {
        MedicamentoEntity m = medicamento(20);
        MvcResult neg = mvc.perform(post("/medicamentos/stock/" + m.getCodMedicamento(), sesion("FARMACEUTICO")).param("nuevaCantidad", "-5")).andReturn();
        MvcResult caj = mvc.perform(post("/medicamentos/stock/" + m.getCodMedicamento(), sesion("CAJERO")).param("nuevaCantidad", "50")).andReturn();
        assertAll(
                () -> assertEquals("El stock no puede ser negativo.", modelo(neg, "error")),
                () -> assertTrue(denegado(caj)),
                () -> assertEquals("No tiene permisos para actualizar stock.", flash(caj, "error")),
                () -> assertEquals(20, stockDe(m))
        );
    }

    @Test
    @DisplayName("FTC049 - Resaltar medicamentos con stock menor o igual al mínimo (valores límite)")
    void ftc049() throws Exception {
        String token = "QALIM" + siguiente();
        medicamento(token + " A", 9, 10, LocalDate.now().plusYears(1));
        medicamento(token + " B", 10, 10, LocalDate.now().plusYears(1));
        medicamento(token + " C", 11, 10, LocalDate.now().plusYears(1));
        MvcResult r = mvc.perform(get("/medicamentos?buscar=" + token, sesion("FARMACEUTICO"))).andReturn();
        List<MedicamentoEntity> l = lista(r, "medicamentos");
        String h = html(r);
        int etiquetas = h.split("Stock bajo", -1).length - 1;
        assertAll(
                () -> assertEquals(3, l.size()),
                () -> assertTrue(l.stream().filter(x -> x.getNombreMedicamento().endsWith(" A")).findFirst().orElseThrow().isStockBajo()),
                () -> assertTrue(l.stream().filter(x -> x.getNombreMedicamento().endsWith(" B")).findFirst().orElseThrow().isStockBajo()),
                () -> assertFalse(l.stream().filter(x -> x.getNombreMedicamento().endsWith(" C")).findFirst().orElseThrow().isStockBajo()),
                () -> assertEquals(2, etiquetas, "Deben verse 2 etiquetas 'Stock bajo'")
        );
    }

    private boolean existeDespacho() {
        return existeClaseQueContenga(null, "despacho");
    }

    @Test
    @DisplayName("FTC050 - Registrar despacho de un tratamiento con comprobante pagado")
    void ftc050() {
        assertTrue(existeDespacho(), "No existe módulo de despacho de medicamentos por tratamiento");
    }

    @Test
    @DisplayName("FTC051 - Rechazar despacho cuando el comprobante no está pagado")
    void ftc051() {
        Assumptions.assumeTrue(existeDespacho(), "Bloqueado: depende de FTC050 (despacho no implementado)");
    }

    @Test
    @DisplayName("FTC052 - Descontar stock al despachar/vender medicamento")
    void ftc052() throws Exception {
        ComprobantePagoEntity c = comprobante(paciente(), EstadoPagoEnum.FALTA_PAGAR);
        MedicamentoEntity m = medicamento(20);
        agregarDetalle(c, "MEDICAMENTO", "codMedicamento", m.getCodMedicamento(), 5, "2.00");
        int s1 = stockDe(m);
        MvcResult insuf = agregarDetalle(c, "MEDICAMENTO", "codMedicamento", m.getCodMedicamento(), 30, "2.00");
        int s2 = stockDe(m);
        DetalleComprobanteEntity d = detallesDe(c).get(0);
        mvc.perform(post("/detalle-comprobantes/eliminar/" + d.getIddetalle(), sesion("CAJERO"))).andReturn();
        int s3 = stockDe(m);
        assertAll(
                () -> assertEquals(15, s1),
                () -> assertTrue(String.valueOf(flash(insuf, "error")).startsWith("Stock insuficiente")),
                () -> assertEquals(15, s2),
                () -> assertEquals(20, s3)
        );
    }

    @Test
    @DisplayName("FTC053 - Ajustar stock al editar la cantidad despachada")
    void ftc053() throws Exception {
        ComprobantePagoEntity c = comprobante(paciente(), EstadoPagoEnum.FALTA_PAGAR);
        MedicamentoEntity m = medicamento(20);
        agregarDetalle(c, "MEDICAMENTO", "codMedicamento", m.getCodMedicamento(), 5, "2.00");
        DetalleComprobanteEntity d = detallesDe(c).get(0);
        int s0 = stockDe(m);
        editarDetalle(d, c, m, 8, "2.00");
        int s1 = stockDe(m);
        editarDetalle(d, c, m, 2, "2.00");
        int s2 = stockDe(m);
        assertAll(
                () -> assertEquals(15, s0),
                () -> assertEquals(12, s1, "Al editar 5→8 el stock debía bajar a 12 y quedó en " + s1),
                () -> assertEquals(18, s2, "Al editar 8→2 el stock debía subir a 18 y quedó en " + s2)
        );
    }
}
