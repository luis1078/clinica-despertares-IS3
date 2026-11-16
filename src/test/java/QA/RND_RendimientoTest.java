package QA;

import App.ClinicaDespertaresApplication;
import Entity.*;
import Entity.Emuns.*;
import Repository.*;
import Service.IUsuarioService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de rendimiento básicas (RNF006: respuesta < 3 s).
 * Servidor real (Tomcat embebido, puerto aleatorio), peticiones HTTP reales con
 * usuarios concurrentes y una base con volumen de datos realista.
 */
@SpringBootTest(classes = ClinicaDespertaresApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"spring.jpa.show-sql=false", "spring.thymeleaf.cache=true"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Pruebas de rendimiento (RNF006)")
class RND_RendimientoTest {

    static final int USUARIOS = 10;
    static final int PETICIONES_POR_USUARIO = 30;
    static final int PACIENTES = 5000;
    static final int CITAS = 3000;
    static final int MEDICAMENTOS = 300;
    static final int COMPROBANTES = 1500;

    @LocalServerPort int puerto;
    @Autowired IPacienteRepository pacienteRepo;
    @Autowired IMedicoRepository medicoRepo;
    @Autowired ICitaMedicaRepository citaRepo;
    @Autowired IMedicamentoRepository medicamentoRepo;
    @Autowired IProveedorRepository proveedorRepo;
    @Autowired IComprobantePagoRepository comprobanteRepo;
    @Autowired IHistoriaMedicaRepository historiaRepo;
    @Autowired IDiagnosticoRepository diagnosticoRepo;
    @Autowired ITratamientoRepository tratamientoRepo;
    @Autowired IUsuarioService usuarioService;

    final List<String> filasCsv = new ArrayList<>();
    final List<PacienteEntity> pacientes = new ArrayList<>();
    final List<MedicoEntity> medicos = new ArrayList<>();
    final List<ComprobantePagoEntity> comprobantes = new ArrayList<>();
    String dniHistorialGrande;

    @BeforeAll
    void cargarVolumen() {
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < 40; i++) {
            MedicoEntity m = new MedicoEntity();
            m.setNombreMedico("Perf"); m.setApellidoMedico("Medico" + i);
            m.setTelefono(String.format("7%08d", 50000000 + i)); m.setCmpMedico(String.format("P%07d", i));
            m.setEspecialidad("General"); m.setHorarioAtencion("Lun-Vie");
            medicos.add(m);
        }
        medicoRepo.saveAll(medicos);
        String[] nombres = {"Juan", "Rosa", "Carlos", "Lucía", "Pedro", "Ana", "Miguel", "Elena"};
        String[] apellidos = {"Quispe", "Flores", "Gonzales", "Rodríguez", "Huamán", "Mamani", "Torres", "Ramos"};
        for (int i = 0; i < PACIENTES; i++) {
            PacienteEntity p = new PacienteEntity();
            p.setDnipaciente(String.format("%08d", 50000000 + i));
            p.setNombrePaciente(nombres[i % nombres.length]);
            p.setApellidoPaciente(apellidos[(i / 8) % apellidos.length] + " " + i);
            p.setTelefono(String.format("6%08d", 10000000 + i));
            p.setCorreoElectronico("perf" + i + "@carga.pe");
            p.setFechaNacimiento(LocalDate.of(1960 + i % 50, 1 + i % 12, 1 + i % 28));
            p.setGenero(i % 2 == 0 ? GeneroPacienteEnum.FEMENINO : GeneroPacienteEnum.MASCULINO);
            p.setTipoSangre("O+");
            pacientes.add(p);
        }
        pacienteRepo.saveAll(pacientes);
        List<CitaMedicaEntity> citas = new ArrayList<>();
        for (int i = 0; i < CITAS; i++) {
            CitaMedicaEntity c = new CitaMedicaEntity();
            c.setPaciente(pacientes.get(i % PACIENTES));
            c.setMedico(medicos.get(i % medicos.size()));
            c.setFechaCita(LocalDate.now().plusDays(1 + (i / 400)));
            c.setHoraCita(LocalTime.of(8, 0).plusMinutes(15L * ((i / medicos.size()) % 40)));
            c.setEstadoCita(EstadoCitaEnum.values()[i % 3]);
            c.setMotivoConsulta("Consulta de carga " + i);
            citas.add(c);
        }
        citaRepo.saveAll(citas);
        ProveedorEntity prov = proveedorRepo.findAll().get(0);
        List<MedicamentoEntity> meds = new ArrayList<>();
        for (int i = 0; i < MEDICAMENTOS; i++) {
            MedicamentoEntity m = new MedicamentoEntity();
            m.setNombreMedicamento("Perf Med " + i); m.setDescripcionMedicamento("Carga");
            m.setTipoMedicamento("General"); m.setFechaVencimiento(LocalDate.now().plusYears(1));
            m.setStockInventario(i % 50); m.setCantidadMinima(10); m.setProveedor(prov);
            meds.add(m);
        }
        medicamentoRepo.saveAll(meds);
        for (int i = 0; i < COMPROBANTES; i++) {
            ComprobantePagoEntity c = new ComprobantePagoEntity();
            c.setPaciente(pacientes.get(i % PACIENTES)); c.setFechaEmision(LocalDate.now().minusDays(i % 30));
            c.setTipoComprobante(TipoComprobanteEnum.BOLETA); c.setMetodoPago(MetodoPagoEnum.EFECTIVO);
            c.setEstado(EstadoPagoEnum.FALTA_PAGAR); c.setSubtotal(new BigDecimal("50.00")); c.setMontoTotal(new BigDecimal("50.00"));
            comprobantes.add(c);
        }
        comprobanteRepo.saveAll(comprobantes);
        // Paciente con historial grande (40 diagnósticos, 20 tratamientos)
        PacienteEntity grande = pacientes.get(7);
        dniHistorialGrande = grande.getDnipaciente();
        HistoriaMedicaEntity h = new HistoriaMedicaEntity();
        h.setPaciente(grande); h.setFechaCreacion(LocalDate.now().minusYears(2));
        h = historiaRepo.save(h);
        for (int i = 0; i < 40; i++) {
            DiagnosticoEntity d = new DiagnosticoEntity();
            d.setHistoriaMedica(h); d.setDescripcionDiagnostico("Dx " + i);
            d.setFechaDiagnostico(LocalDate.now().minusDays(i * 10L)); d.setGravedadDiagnostico(GravedadDiagnosticoEnum.LEVE);
            diagnosticoRepo.save(d);
        }
        for (int i = 0; i < 20; i++) {
            TratamientoEntity t = new TratamientoEntity();
            t.setHistoriaMedica(h); t.setDescripcionTratamiento("Tx " + i); t.setFechaInicio(LocalDate.now().minusDays(i * 15L));
            t.setFechaFin(LocalDate.now()); t.setTipoTratamiento("General"); t.setEstadoTratamiento(EstadoTratamientoEnum.FINALIZADO);
            tratamientoRepo.save(t);
        }
        System.out.println("[QA-RND] Carga de volumen completada en " + (System.currentTimeMillis() - t0) + " ms");
        filasCsv.add("escenario,usuarios,peticiones,exitosas,errores,tasa_error_pct,promedio_ms,p50_ms,p95_ms,p99_ms,max_ms,cumple");
    }

    // ---------- Cliente HTTP con sesión ----------

    class Cliente {
        final HttpClient http;
        final String base = "http://localhost:" + puerto;
        String csrf;

        Cliente(String rol) throws Exception {
            http = HttpClient.newBuilder().cookieHandler(new CookieManager())
                    .followRedirects(HttpClient.Redirect.NEVER).connectTimeout(Duration.ofSeconds(10)).build();
            int n = (int) (System.nanoTime() % 1000000);
            UsuarioEntity u = new UsuarioEntity();
            u.setUsername("perf_" + rol.toLowerCase() + "_" + n + "_" + UUID.randomUUID().toString().substring(0, 4));
            u.setCorreo(u.getUsername() + "@perf.pe"); u.setPassword("Perf123*"); u.setRol(RolUsuarioEnum.valueOf(rol));
            usuarioService.registrar(u);
            String login = http.send(HttpRequest.newBuilder(URI.create(base + "/login")).GET().build(),
                    HttpResponse.BodyHandlers.ofString()).body();
            csrf = extraerCsrf(login);
            HttpResponse<String> r = postForm("/login", Map.of("username", u.getUsername(), "password", "Perf123*"));
            assertEquals(302, r.statusCode(), "Login de prueba falló");
            // refrescar token tras el login (misma sesión)
            String nuevo = extraerCsrf(get("/").body());
            if (!nuevo.isEmpty()) csrf = nuevo;
        }

        HttpResponse<String> get(String ruta) throws IOException, InterruptedException {
            return http.send(HttpRequest.newBuilder(URI.create(base + ruta)).timeout(Duration.ofSeconds(30)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
        }

        HttpResponse<String> postForm(String ruta, Map<String, String> datos) throws IOException, InterruptedException {
            Map<String, String> todo = new LinkedHashMap<>(datos);
            todo.put("_csrf", csrf);
            StringBuilder sb = new StringBuilder();
            for (var e : todo.entrySet()) {
                if (sb.length() > 0) sb.append('&');
                sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)).append('=')
                        .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
            }
            return http.send(HttpRequest.newBuilder(URI.create(base + ruta)).timeout(Duration.ofSeconds(30))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(sb.toString())).build(),
                    HttpResponse.BodyHandlers.ofString());
        }
    }

    static String extraerCsrf(String html) {
        Matcher m = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"").matcher(html);
        if (m.find()) return m.group(1);
        m = Pattern.compile("value=\"([^\"]+)\"\\s+name=\"_csrf\"").matcher(html);
        return m.find() ? m.group(1) : "";
    }

    interface Peticion {
        HttpResponse<String> ejecutar(Cliente c, int i) throws Exception;
    }

    record Resultado(int status, long ms, boolean ok) {}

    /** Ejecuta USUARIOS hilos concurrentes, cada uno con su propia sesión. */
    void medir(String escenario, String rol, Peticion peticion, IntFunction<Boolean> esExito) throws Exception {
        List<Cliente> clientes = new ArrayList<>();
        for (int u = 0; u < USUARIOS; u++) clientes.add(new Cliente(rol));
        // calentamiento
        for (int i = 0; i < 5; i++) peticion.ejecutar(clientes.get(0), 100000 + i);

        ExecutorService pool = Executors.newFixedThreadPool(USUARIOS);
        CountDownLatch inicio = new CountDownLatch(1);
        List<Future<List<Resultado>>> futuros = new ArrayList<>();
        for (int u = 0; u < USUARIOS; u++) {
            final Cliente c = clientes.get(u);
            final int usuario = u;
            futuros.add(pool.submit(() -> {
                inicio.await();
                List<Resultado> rs = new ArrayList<>();
                for (int i = 0; i < PETICIONES_POR_USUARIO; i++) {
                    long t = System.nanoTime();
                    int status;
                    try {
                        status = peticion.ejecutar(c, usuario * PETICIONES_POR_USUARIO + i).statusCode();
                    } catch (Exception ex) {
                        status = -1;
                    }
                    long ms = (System.nanoTime() - t) / 1_000_000;
                    rs.add(new Resultado(status, ms, esExito.apply(status)));
                }
                return rs;
            }));
        }
        inicio.countDown();
        List<Resultado> todos = new ArrayList<>();
        for (var f : futuros) todos.addAll(f.get(10, TimeUnit.MINUTES));
        pool.shutdown();

        long[] t = todos.stream().mapToLong(Resultado::ms).sorted().toArray();
        long errores = todos.stream().filter(r -> !r.ok()).count();
        double tasa = 100.0 * errores / todos.size();
        double prom = Arrays.stream(t).average().orElse(0);
        long p50 = t[(int) Math.ceil(0.50 * t.length) - 1];
        long p95 = t[(int) Math.ceil(0.95 * t.length) - 1];
        long p99 = t[(int) Math.ceil(0.99 * t.length) - 1];
        long max = t[t.length - 1];
        boolean cumple = p95 < 3000 && tasa < 1.0;
        String fila = String.format(Locale.ROOT, "%s,%d,%d,%d,%d,%.2f,%.1f,%d,%d,%d,%d,%s",
                escenario, USUARIOS, todos.size(), todos.size() - errores, errores, tasa, prom, p50, p95, p99, max, cumple ? "SI" : "NO");
        filasCsv.add(fila);
        System.out.println("[QA-RND] " + fila);
        escribirCsv();
        assertTrue(cumple, escenario + ": p95=" + p95 + " ms, errores=" + tasa + "%");
    }

    void escribirCsv() throws IOException {
        Path dir = Path.of("target", "qa");
        Files.createDirectories(dir);
        Files.write(dir.resolve("rendimiento.csv"), filasCsv, StandardCharsets.UTF_8);
    }

    @Test @Order(1)
    @DisplayName("RND01 - Búsqueda de pacientes por DNI (5 000 pacientes)")
    void rnd01() throws Exception {
        medir("RND01 Buscar paciente por DNI", "ENFERMERA",
                (c, i) -> c.get("/pacientes?buscar=" + pacientes.get((i * 37) % PACIENTES).getDnipaciente()), s -> s == 200);
    }

    @Test @Order(2)
    @DisplayName("RND02 - Búsqueda de pacientes por nombre (5 000 pacientes)")
    void rnd02() throws Exception {
        String[] t = {"quispe", "Rosa", "mamani", "TORRES", "Lucía"};
        medir("RND02 Buscar paciente por nombre", "ENFERMERA",
                (c, i) -> c.get("/pacientes?buscar=" + URLEncoder.encode(t[i % t.length], StandardCharsets.UTF_8)), s -> s == 200);
    }

    @Test @Order(3)
    @DisplayName("RND03 - Listado y filtro de la agenda de citas (3 000 citas)")
    void rnd03() throws Exception {
        medir("RND03 Listar/filtrar citas", "ENFERMERA",
                (c, i) -> c.get(i % 2 == 0 ? "/citas" : "/citas?estado=PENDIENTE&buscar=carga"), s -> s == 200);
    }

    @Test @Order(4)
    @DisplayName("RND04 - Registro concurrente de citas (verificación de disponibilidad)")
    void rnd04() throws Exception {
        medir("RND04 Registrar cita (disponibilidad)", "ENFERMERA",
                (c, i) -> c.postForm("/citas/guardar", Map.of(
                        "dniPaciente", pacientes.get(i % PACIENTES).getDnipaciente(),
                        "idMedico", medicos.get(i % medicos.size()).getIdMedico().toString(),
                        "fechaCita", LocalDate.now().plusDays(200 + i / 40).toString(),
                        "horaCita", LocalTime.of(7, 0).plusMinutes(i % 40).toString(),
                        "motivoConsulta", "Carga concurrente")), s -> s == 302);
    }

    @Test @Order(5)
    @DisplayName("RND05 - Consulta de inventario de medicamentos (300 medicamentos)")
    void rnd05() throws Exception {
        medir("RND05 Consultar inventario", "FARMACEUTICO",
                (c, i) -> c.get(i % 2 == 0 ? "/medicamentos" : "/medicamentos?stockMinimo=10"), s -> s == 200);
    }

    @Test @Order(6)
    @DisplayName("RND06 - Procesamiento de pagos en caja (1 500 comprobantes)")
    void rnd06() throws Exception {
        medir("RND06 Procesar pago", "CAJERO",
                (c, i) -> c.postForm("/comprobantes/cancelar/" + comprobantes.get(i % COMPROBANTES).getCodcomprobante(), Map.of()),
                s -> s == 302);
    }

    @Test @Order(7)
    @DisplayName("RND07 - Carga de historia clínica completa")
    void rnd07() throws Exception {
        medir("RND07 Historial clínico completo", "MEDICO",
                (c, i) -> c.get("/pacientes/" + dniHistorialGrande + "/historial"), s -> s == 200);
    }

    @Test @Order(8)
    @DisplayName("RND08 - Panel principal con indicadores (dashboard)")
    void rnd08() throws Exception {
        medir("RND08 Panel principal", "CAJERO", (c, i) -> c.get("/"), s -> s == 200);
    }
}
