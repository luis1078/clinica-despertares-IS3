package QA;

import App.ClinicaDespertaresApplication;
import Entity.*;
import Entity.Emuns.*;
import Repository.*;
import Service.IUsuarioService;
import Util.CsrfTokenHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base común de la suite de pruebas del Informe Final (Avance 3).
 * Levanta el contexto completo de Spring Boot contra PostgreSQL (el mismo de CI)
 * y ejecuta cada caso de prueba (FTCxxx) a través de la capa web real:
 * AuthInterceptor + Controller + Service + Repository + base de datos.
 */
@SpringBootTest(classes = ClinicaDespertaresApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {"spring.jpa.show-sql=false"})
public abstract class SoporteQA {

    protected static final String CSRF = "token-qa-is3";
    private static final AtomicInteger SECUENCIA = new AtomicInteger((int) (System.currentTimeMillis() % 100000) * 10);

    @Autowired protected MockMvc mvc;
    @Autowired protected IPacienteRepository pacienteRepo;
    @Autowired protected IMedicoRepository medicoRepo;
    @Autowired protected ICitaMedicaRepository citaRepo;
    @Autowired protected IHistoriaMedicaRepository historiaRepo;
    @Autowired protected IDiagnosticoRepository diagnosticoRepo;
    @Autowired protected ITratamientoRepository tratamientoRepo;
    @Autowired protected IDetalleTratamientoRepository detalleTratamientoRepo;
    @Autowired protected IMedicamentoRepository medicamentoRepo;
    @Autowired protected IProveedorRepository proveedorRepo;
    @Autowired protected IComprobantePagoRepository comprobanteRepo;
    @Autowired protected IDetalleComprobanteRepository detalleComprobanteRepo;
    @Autowired protected IExamenMedicoRepository examenRepo;
    @Autowired protected IExamenLaboratorioRepository laboratorioRepo;
    @Autowired protected IImagenDiagnosticaRepository imagenRepo;
    @Autowired protected IUsuarioRepository usuarioRepo;
    @Autowired protected IUsuarioService usuarioService;

    protected static int siguiente() {
        return SECUENCIA.incrementAndGet();
    }

    // ---------- Sesiones por rol ----------

    protected MockHttpSession sesion(String rol) {
        MockHttpSession s = new MockHttpSession();
        UsuarioEntity u = new UsuarioEntity();
        u.setUsername("qa_" + rol.toLowerCase());
        u.setRol(RolUsuarioEnum.valueOf(rol));
        s.setAttribute("usuarioLogueado", u);
        s.setAttribute("nombreUsuario", u.getUsername());
        s.setAttribute("rolUsuario", rol);
        s.setAttribute(CsrfTokenHelper.ATRIBUTO_SESION, CSRF);
        return s;
    }

    protected MockHttpSession sesionAnonimaConCsrf() {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute(CsrfTokenHelper.ATRIBUTO_SESION, CSRF);
        return s;
    }

    protected MockHttpServletRequestBuilder get(String url, MockHttpSession s) {
        return MockMvcRequestBuilders.get(url).session(s);
    }

    protected MockHttpServletRequestBuilder post(String url, MockHttpSession s) {
        return MockMvcRequestBuilders.post(url).session(s).param(CsrfTokenHelper.PARAMETRO_REQUEST, CSRF);
    }

    protected String redirect(MvcResult r) {
        return r.getResponse().getRedirectedUrl();
    }

    protected boolean denegado(MvcResult r) {
        return "/acceso-denegado".equals(redirect(r));
    }

    protected Object modelo(MvcResult r, String clave) {
        return r.getModelAndView() == null ? null : r.getModelAndView().getModel().get(clave);
    }

    protected String vista(MvcResult r) {
        return r.getModelAndView() == null ? null : r.getModelAndView().getViewName();
    }

    protected Object flash(MvcResult r, String clave) {
        return r.getFlashMap().get(clave);
    }

    @SuppressWarnings("unchecked")
    protected <T> List<T> lista(MvcResult r, String clave) {
        return (List<T>) modelo(r, clave);
    }

    protected String html(MvcResult r) throws Exception {
        return r.getResponse().getContentAsString();
    }

    // ---------- Datos de prueba ----------

    protected PacienteEntity paciente(String nombre, String apellido) {
        int n = siguiente();
        PacienteEntity p = new PacienteEntity();
        p.setDnipaciente(String.format("%08d", 30000000 + (n % 60000000)));
        p.setNombrePaciente(nombre);
        p.setApellidoPaciente(apellido);
        p.setTelefono(String.format("9%08d", n % 100000000));
        p.setCorreoElectronico("qa" + n + "@test.pe");
        p.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        p.setGenero(GeneroPacienteEnum.FEMENINO);
        p.setTipoSangre("O+");
        p.setDireccion("Av. Pruebas " + n);
        return pacienteRepo.save(p);
    }

    protected PacienteEntity paciente() {
        return paciente("Paciente", "Qa" + siguiente());
    }

    protected MedicoEntity medico() {
        int n = siguiente();
        MedicoEntity m = new MedicoEntity();
        m.setNombreMedico("Medico");
        m.setApellidoMedico("Qa" + n);
        m.setTelefono(String.format("8%08d", n % 100000000));
        m.setCmpMedico(String.format("Q%07d", n % 10000000));
        m.setEspecialidad("Medicina General");
        m.setHorarioAtencion("Lun-Vie 8-14");
        return medicoRepo.save(m);
    }

    protected CitaMedicaEntity cita(PacienteEntity p, MedicoEntity m, LocalDate fecha, LocalTime hora, EstadoCitaEnum estado) {
        CitaMedicaEntity c = new CitaMedicaEntity();
        c.setPaciente(p);
        c.setMedico(m);
        c.setFechaCita(fecha);
        c.setHoraCita(hora);
        c.setEstadoCita(estado);
        c.setMotivoConsulta("Control QA");
        return citaRepo.save(c);
    }

    protected HistoriaMedicaEntity historia(PacienteEntity p) {
        HistoriaMedicaEntity h = new HistoriaMedicaEntity();
        h.setPaciente(p);
        h.setFechaCreacion(LocalDate.now());
        h.setAntecedentesPersonales("Ninguno");
        return historiaRepo.save(h);
    }

    protected DiagnosticoEntity diagnostico(HistoriaMedicaEntity h) {
        DiagnosticoEntity d = new DiagnosticoEntity();
        d.setHistoriaMedica(h);
        d.setDescripcionDiagnostico("Diagnóstico QA");
        d.setFechaDiagnostico(LocalDate.now());
        d.setGravedadDiagnostico(GravedadDiagnosticoEnum.LEVE);
        return diagnosticoRepo.save(d);
    }

    protected DiagnosticoEntity diagnosticoConPaciente(PacienteEntity p) {
        return diagnostico(historia(p));
    }

    protected TratamientoEntity tratamiento(HistoriaMedicaEntity h, EstadoTratamientoEnum estado) {
        TratamientoEntity t = new TratamientoEntity();
        t.setHistoriaMedica(h);
        t.setDescripcionTratamiento("Tratamiento QA");
        t.setFechaInicio(LocalDate.now());
        t.setFechaFin(LocalDate.now().plusDays(7));
        t.setTipoTratamiento("Farmacológico");
        t.setEstadoTratamiento(estado);
        return tratamientoRepo.save(t);
    }

    protected MedicamentoEntity medicamento(String nombre, int stock, int minimo, LocalDate vencimiento) {
        MedicamentoEntity m = new MedicamentoEntity();
        m.setNombreMedicamento(nombre);
        m.setDescripcionMedicamento("Medicamento de prueba");
        m.setTipoMedicamento("Analgésico");
        m.setFechaVencimiento(vencimiento);
        m.setStockInventario(stock);
        m.setCantidadMinima(minimo);
        m.setProveedor(proveedorRepo.findAll().get(0));
        return medicamentoRepo.save(m);
    }

    protected MedicamentoEntity medicamento(int stock) {
        return medicamento("QA Med " + siguiente(), stock, 5, LocalDate.now().plusYears(1));
    }

    protected ComprobantePagoEntity comprobante(PacienteEntity p, EstadoPagoEnum estado) {
        ComprobantePagoEntity c = new ComprobantePagoEntity();
        c.setPaciente(p);
        c.setFechaEmision(LocalDate.now());
        c.setTipoComprobante(TipoComprobanteEnum.BOLETA);
        c.setMetodoPago(MetodoPagoEnum.EFECTIVO);
        c.setEstado(estado);
        c.setSubtotal(BigDecimal.ZERO);
        c.setMontoTotal(BigDecimal.ZERO);
        return comprobanteRepo.save(c);
    }

    protected ExamenMedicoEntity examen(DiagnosticoEntity d, TipoExamenEnum tipo, EstadoExamenMedicoEnum estado) {
        ExamenMedicoEntity e = new ExamenMedicoEntity();
        e.setDiagnostico(d);
        e.setTipoExamen(tipo);
        e.setEstadoExamenMedico(estado);
        e.setFechaExamen(LocalDate.now());
        return examenRepo.save(e);
    }

    protected ExamenLaboratorioEntity examenLaboratorio(DiagnosticoEntity d, EstadoExamenMedicoEnum estado) {
        ExamenLaboratorioEntity e = new ExamenLaboratorioEntity();
        e.setDiagnostico(d);
        e.setTipoExamen(TipoExamenEnum.LABORATORIO);
        e.setEstadoExamenMedico(estado);
        e.setFechaExamen(LocalDate.now());
        e.setNombreMuestra("Sangre");
        e.setDescripcionResultado("Pendiente de resultado");
        return laboratorioRepo.save(e);
    }

    protected ImagenDiagnosticaEntity imagen(DiagnosticoEntity d, EstadoExamenMedicoEnum estado) {
        ImagenDiagnosticaEntity e = new ImagenDiagnosticaEntity();
        e.setDiagnostico(d);
        e.setTipoExamen(TipoExamenEnum.IMAGEN);
        e.setEstadoExamenMedico(estado);
        e.setFechaExamen(LocalDate.now());
        e.setNombreImagen("Rayos X");
        e.setRegionCuerpo("Tórax");
        e.setInformeMedico("Sin hallazgos");
        return imagenRepo.save(e);
    }

    protected UsuarioEntity usuario(String rol, String clave) {
        int n = siguiente();
        UsuarioEntity u = new UsuarioEntity();
        u.setUsername("qa_user_" + n);
        u.setCorreo("qa_user_" + n + "@clinica.pe");
        u.setPassword(clave);
        u.setRol(RolUsuarioEnum.valueOf(rol));
        return usuarioService.registrar(u);
    }

    protected List<DetalleComprobanteEntity> detallesDe(ComprobantePagoEntity c) {
        return detalleComprobanteRepo.findByComprobantePago_Codcomprobante(c.getCodcomprobante());
    }

    protected BigDecimal totalDe(ComprobantePagoEntity c) {
        return comprobanteRepo.findById(c.getCodcomprobante()).orElseThrow().getMontoTotal();
    }

    protected int stockDe(MedicamentoEntity m) {
        return medicamentoRepo.findById(m.getCodMedicamento()).orElseThrow().getStockInventario();
    }

    protected static boolean existeClaseQueContenga(String paquete, String fragmento) {
        String[] candidatos = {"Entity", "Controller", "Service", "Service.ServiceImpl"};
        for (String p : candidatos) {
            if (paquete != null && !paquete.equals(p)) continue;
            try {
                var urls = SoporteQA.class.getClassLoader().getResources(p.replace('.', '/'));
                while (urls.hasMoreElements()) {
                    var url = urls.nextElement();
                    if (url.getPath().contains("test-classes")) continue;
                    String[] archivos = new java.io.File(url.toURI()).list();
                    if (archivos == null) continue;
                    for (String a : archivos) {
                        if (a.toLowerCase().contains(fragmento.toLowerCase())) return true;
                    }
                }
            } catch (Exception ignored) {
                // sin acceso al directorio de clases
            }
        }
        return false;
    }

    protected static boolean claseTieneCampo(Class<?> clase, String fragmento) {
        for (Class<?> c = clase; c != null; c = c.getSuperclass()) {
            for (var f : c.getDeclaredFields()) {
                if (f.getName().toLowerCase().contains(fragmento.toLowerCase())) return true;
            }
        }
        return false;
    }
}
