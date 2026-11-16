package QA;

import Entity.*;
import Entity.Emuns.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas exploratorias: cada prueba afirma el comportamiento CORRECTO esperado.
 * Si falla, el defecto queda confirmado con evidencia de ejecución.
 */
@DisplayName("Pruebas exploratorias (EXP01–EXP12)")
class EXP_PruebasExploratoriasTest extends SoporteQA {

    @Test
    @DisplayName("EXP01 - El registro público no debe permitir crear cuentas con cualquier rol")
    void exp01() throws Exception {
        int n = siguiente();
        MedicoEntity m = medico();
        MockHttpSession anon = sesionAnonimaConCsrf();
        mvc.perform(post("/registro", anon).param("username", "intruso" + n).param("correo", "intruso" + n + "@x.pe")
                .param("password", "1").param("confirmarPassword", "1").param("rol", "MEDICO")
                .param("idMedico", m.getIdMedico().toString())).andReturn();
        boolean creado = usuarioRepo.findByUsername("intruso" + n).isPresent();
        MockHttpSession s = sesionAnonimaConCsrf();
        mvc.perform(post("/login", s).param("username", "intruso" + n).param("password", "1")).andReturn();
        boolean accedeUsuarios = false;
        if (s.getAttribute("usuarioLogueado") != null) {
            MvcResult u = mvc.perform(get("/usuarios", s)).andReturn();
            accedeUsuarios = "usuarios/listar".equals(vista(u));
        }
        boolean finalAccede = accedeUsuarios;
        assertAll(
                () -> assertFalse(creado, "Un visitante sin sesión creó una cuenta con rol MEDICO y contraseña '1'"),
                () -> assertFalse(finalAccede, "La cuenta auto-registrada accede a la administración de usuarios")
        );
    }

    @Test
    @DisplayName("EXP02 - Registrar un paciente con DNI existente no debe sobrescribir sus datos")
    void exp02() throws Exception {
        PacienteEntity original = paciente("Original", "Paciente");
        int n = siguiente();
        mvc.perform(post("/pacientes/guardar", sesion("ENFERMERA"))
                .param("dnipaciente", original.getDnipaciente()).param("nombrePaciente", "Sobrescrito")
                .param("apellidoPaciente", "Otro").param("genero", "MASCULINO")
                .param("telefono", String.format("5%08d", n % 100000000)).param("correoElectronico", "otro" + n + "@mail.pe")
                .param("fechaNacimiento", "2000-01-01").param("tipoSangre", "B+")).andReturn();
        String nombre = pacienteRepo.findById(original.getDnipaciente()).orElseThrow().getNombrePaciente();
        assertEquals("Original", nombre, "El registro con un DNI ya existente reemplazó al paciente original por '" + nombre + "'");
    }

    @Test
    @DisplayName("EXP03 - Editar una cita no debe permitir moverla a un horario ocupado")
    void exp03() throws Exception {
        PacienteEntity p = paciente();
        MedicoEntity m = medico();
        LocalDate dia = LocalDate.now().plusDays(25);
        cita(p, m, dia, LocalTime.of(10, 0), EstadoCitaEnum.PENDIENTE);
        CitaMedicaEntity b = cita(p, m, dia, LocalTime.of(11, 0), EstadoCitaEnum.PENDIENTE);
        mvc.perform(post("/citas/guardar", sesion("ENFERMERA"))
                .param("codCitaMedica", b.getCodCitaMedica().toString()).param("dniPaciente", p.getDnipaciente())
                .param("idMedico", m.getIdMedico().toString()).param("fechaCita", dia.toString())
                .param("horaCita", "10:00").param("estadoCita", "PENDIENTE").param("motivoConsulta", "Editada")).andReturn();
        long choques = citaRepo.findByMedico_IdMedico(m.getIdMedico()).stream()
                .filter(c -> c.getHoraCita().equals(LocalTime.of(10, 0)) && c.getEstadoCita() == EstadoCitaEnum.PENDIENTE).count();
        assertEquals(1, choques, "Quedaron " + choques + " citas PENDIENTES del mismo médico a las 10:00 (doble reserva vía edición)");
    }

    @Test
    @DisplayName("EXP04 - No se deben registrar ni reprogramar citas en fechas pasadas")
    void exp04() throws Exception {
        PacienteEntity p = paciente();
        MedicoEntity m = medico();
        LocalDate ayer = LocalDate.now().minusDays(1);
        mvc.perform(post("/citas/guardar", sesion("ENFERMERA")).param("dniPaciente", p.getDnipaciente())
                .param("idMedico", m.getIdMedico().toString()).param("fechaCita", ayer.toString())
                .param("horaCita", "09:00").param("motivoConsulta", "Pasada")).andReturn();
        CitaMedicaEntity futura = cita(p, m, LocalDate.now().plusDays(3), LocalTime.of(9, 0), EstadoCitaEnum.PENDIENTE);
        mvc.perform(post("/citas/reprogramar/" + futura.getCodCitaMedica(), sesion("ENFERMERA"))
                .param("nuevaFecha", LocalDate.now().minusDays(30).toString()).param("nuevaHora", "09:00")).andReturn();
        long pasadas = citaRepo.findByMedico_IdMedico(m.getIdMedico()).stream()
                .filter(c -> c.getFechaCita().isBefore(LocalDate.now())).count();
        assertEquals(0, pasadas, "Se aceptaron " + pasadas + " citas con fecha anterior a hoy");
    }

    @Test
    @DisplayName("EXP05 - No se debe vender un medicamento vencido")
    void exp05() throws Exception {
        MedicamentoEntity vencido = medicamento("QA Vencido " + siguiente(), 10, 2, LocalDate.now().minusDays(1));
        ComprobantePagoEntity c = comprobante(paciente(), EstadoPagoEnum.FALTA_PAGAR);
        mvc.perform(post("/detalle-comprobantes/guardar", sesion("CAJERO"))
                .param("codComprobante", c.getCodcomprobante().toString()).param("tipoItem", "MEDICAMENTO")
                .param("codMedicamento", vencido.getCodMedicamento().toString()).param("descripcionDetalle", "Venta")
                .param("cantidad", "1").param("precioUnitario", "3.00")).andReturn();
        long semillaVencida = medicamentoRepo.findAll().stream().filter(x -> x.getFechaVencimiento().isBefore(LocalDate.now())).count();
        System.out.println("[QA] EXP05 medicamentos vencidos en inventario (incluye datos semilla): " + semillaVencida);
        assertTrue(detallesDe(c).isEmpty(), "Se registró la venta de un medicamento vencido y el stock bajó a " + stockDe(vencido));
    }

    @Test
    @DisplayName("EXP06 - No se debe registrar un medicamento con stock inicial negativo")
    void exp06() throws Exception {
        String nombre = "QA Neg " + siguiente();
        mvc.perform(post("/medicamentos/guardar", sesion("FARMACEUTICO"))
                .param("rucProveedor", proveedorRepo.findAll().get(0).getRucProveedor()).param("nombreMedicamento", nombre)
                .param("descripcionMedicamento", "x").param("tipoMedicamento", "x")
                .param("fechaVencimiento", LocalDate.now().plusYears(1).toString())
                .param("stockInventario", "-10").param("cantidadMinima", "-3")).andReturn();
        assertTrue(medicamentoRepo.findByNombreMedicamento(nombre).isEmpty(), "Se guardó un medicamento con stock -10 y mínimo -3");
    }

    @Test
    @DisplayName("EXP07 - La solicitud de laboratorio del médico debe llegar a la bandeja del biólogo")
    void exp07() throws Exception {
        PacienteEntity p = paciente();
        DiagnosticoEntity d = diagnosticoConPaciente(p);
        mvc.perform(post("/examenes/guardar", sesion("MEDICO")).param("idDiagnostico", d.getIdDiagnostico().toString())
                .param("tipoExamen", "LABORATORIO")).andReturn();
        Long cod = examenRepo.findByDiagnostico_IdDiagnostico(d.getIdDiagnostico()).get(0).getCodExamenMedico();
        List<ExamenLaboratorioEntity> bandeja = lista(mvc.perform(get("/laboratorio?buscar=" + p.getDnipaciente(), sesion("BIOLOGO"))).andReturn(), "examenes");
        assertTrue(bandeja.stream().anyMatch(e -> e.getCodExamenMedico().equals(cod)),
                "La solicitud creada en /examenes no aparece en /laboratorio (no se crea el registro en examenlaboratorio)");
    }

    @Test
    @DisplayName("EXP08 - No se debe procesar un examen cuyo comprobante está pendiente de pago")
    void exp08() throws Exception {
        DiagnosticoEntity d = diagnosticoConPaciente(paciente());
        mvc.perform(post("/examenes/guardar", sesion("MEDICO")).param("idDiagnostico", d.getIdDiagnostico().toString())
                .param("tipoExamen", "LABORATORIO")).andReturn();
        Long cod = examenRepo.findByDiagnostico_IdDiagnostico(d.getIdDiagnostico()).get(0).getCodExamenMedico();
        mvc.perform(post("/examenes/proceso/" + cod, sesion("BIOLOGO"))).andReturn();
        EstadoExamenMedicoEnum e = examenRepo.findById(cod).orElseThrow().getEstadoExamenMedico();
        assertEquals(EstadoExamenMedicoEnum.PENDIENTE, e, "El examen pasó a " + e + " sin que su comprobante esté pagado");
    }

    @Test
    @DisplayName("EXP09 - Un comprobante pagado no debe modificarse")
    void exp09() throws Exception {
        PacienteEntity p = paciente();
        ComprobantePagoEntity c = comprobante(p, EstadoPagoEnum.CANCELADO);
        CitaMedicaEntity cita = cita(p, medico(), LocalDate.now().plusDays(4), LocalTime.of(9, 0), EstadoCitaEnum.PENDIENTE);
        mvc.perform(post("/detalle-comprobantes/guardar", sesion("CAJERO"))
                .param("codComprobante", c.getCodcomprobante().toString()).param("tipoItem", "CITA")
                .param("codCitaMedica", cita.getCodCitaMedica().toString()).param("descripcionDetalle", "Agregado tras el pago")
                .param("cantidad", "1").param("precioUnitario", "50.00")).andReturn();
        BigDecimal total = totalDe(c);
        assertEquals(0, BigDecimal.ZERO.compareTo(total), "El total de un comprobante ya pagado cambió a S/ " + total);
    }

    @Test
    @DisplayName("EXP10 - El servidor debe rechazar un estudio con región en blanco")
    void exp10() throws Exception {
        DiagnosticoEntity d = diagnosticoConPaciente(paciente());
        mvc.perform(post("/imagenes/guardar", sesion("RADIOLOGO")).param("idDiagnostico", d.getIdDiagnostico().toString())
                .param("nombreImagen", " ").param("regionCuerpo", "").param("informeMedico", "")
                .param("usaContraste", "false")).andReturn();
        int creados = examenRepo.findByDiagnostico_IdDiagnostico(d.getIdDiagnostico()).size();
        assertEquals(0, creados, "Se guardó un estudio con nombre, región e informe vacíos");
    }

    @Test
    @DisplayName("EXP11 - El servidor debe validar el formato del DNI (8 dígitos)")
    void exp11() throws Exception {
        int n = siguiente();
        String dni = "AB" + (n % 1000);
        mvc.perform(post("/pacientes/guardar", sesion("ENFERMERA"))
                .param("dnipaciente", dni).param("nombrePaciente", "Formato").param("apellidoPaciente", "Invalido")
                .param("genero", "MASCULINO").param("telefono", String.format("4%08d", n % 100000000))
                .param("correoElectronico", "fmt" + n + "@mail.pe").param("fechaNacimiento", "2000-01-01")
                .param("tipoSangre", "O+")).andReturn();
        assertTrue(pacienteRepo.findById(dni).isEmpty(), "Se registró un paciente con DNI '" + dni + "'");
    }

    @Test
    @DisplayName("EXP12 - Reserva simultánea: dos registros concurrentes del mismo horario")
    void exp12() throws Exception {
        PacienteEntity p = paciente();
        MedicoEntity m = medico();
        LocalDate dia = LocalDate.now().plusDays(40);
        int hilos = 8;
        var listo = new java.util.concurrent.CountDownLatch(1);
        var pool = java.util.concurrent.Executors.newFixedThreadPool(hilos);
        List<java.util.concurrent.Future<?>> fs = new java.util.ArrayList<>();
        for (int i = 0; i < hilos; i++) {
            fs.add(pool.submit(() -> {
                listo.await();
                return mvc.perform(post("/citas/guardar", sesion("ENFERMERA")).param("dniPaciente", p.getDnipaciente())
                        .param("idMedico", m.getIdMedico().toString()).param("fechaCita", dia.toString())
                        .param("horaCita", "09:00").param("motivoConsulta", "Concurrente")).andReturn();
            }));
        }
        listo.countDown();
        for (var f : fs) f.get();
        pool.shutdown();
        long creadas = citaRepo.findByMedico_IdMedico(m.getIdMedico()).size();
        System.out.println("[QA] EXP12 citas creadas en el mismo horario con " + hilos + " solicitudes simultáneas: " + creadas);
        assertEquals(1, creadas, "Con " + hilos + " solicitudes simultáneas se crearon " + creadas + " citas en el mismo horario");
    }
}
