package QA;

import Entity.PacienteEntity;
import Entity.UsuarioEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Acceso al sistema, panel principal y pacientes (FTC001–FTC010)")
class FTC01_AccesoYPacientesTest extends SoporteQA {

    @Autowired ServerProperties serverProperties;

    @Test
    @DisplayName("FTC001 - Iniciar sesión con credenciales válidas")
    void ftc001() throws Exception {
        UsuarioEntity u = usuario("CAJERO", "Clave123*");
        MockHttpSession s = sesionAnonimaConCsrf();
        MvcResult r = mvc.perform(post("/login", s)
                .param("username", u.getUsername()).param("password", "Clave123*")).andReturn();
        assertEquals("/", redirect(r), "Debe redirigir al panel principal");
        assertNotNull(s.getAttribute("usuarioLogueado"), "La sesión debe guardar el usuario");
        assertEquals("CAJERO", s.getAttribute("rolUsuario"), "La sesión debe guardar el rol");
    }

    @Test
    @DisplayName("FTC002 - Rechazar contraseña incorrecta o usuario desactivado")
    void ftc002() throws Exception {
        UsuarioEntity a = usuario("CAJERO", "Clave123*");
        UsuarioEntity b = usuario("CAJERO", "Clave123*");
        usuarioService.desactivarUsuario(b.getIdUsuario());

        MockHttpSession s1 = sesionAnonimaConCsrf();
        MvcResult r1 = mvc.perform(post("/login", s1).param("username", a.getUsername()).param("password", "Errada99")).andReturn();
        MockHttpSession s2 = sesionAnonimaConCsrf();
        MvcResult r2 = mvc.perform(post("/login", s2).param("username", b.getUsername()).param("password", "Clave123*")).andReturn();

        assertAll(
                () -> assertEquals("/login", redirect(r1)),
                () -> assertEquals("Usuario, correo o contraseña incorrectos.", flash(r1, "error")),
                () -> assertNull(s1.getAttribute("usuarioLogueado")),
                () -> assertEquals("/login", redirect(r2)),
                () -> assertEquals("Usuario, correo o contraseña incorrectos.", flash(r2, "error")),
                () -> assertNull(s2.getAttribute("usuarioLogueado"))
        );
    }

    @Test
    @DisplayName("FTC003 - Redirigir al login sin sesión y cerrar sesión")
    void ftc003() throws Exception {
        MockHttpSession s = sesion("ENFERMERA");
        MvcResult logout = mvc.perform(get("/logout", s)).andReturn();
        MvcResult sinSesion = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/citas")).andReturn();
        System.out.println("[QA] FTC003 tiempo de expiración de sesión configurado: "
                + serverProperties.getServlet().getSession().getTimeout());
        assertAll(
                () -> assertEquals("/login", redirect(logout)),
                () -> assertTrue(s.isInvalid(), "La sesión debe quedar invalidada"),
                () -> assertEquals("/login", redirect(sinSesion), "Sin sesión debe volver al login")
        );
    }

    private static final Map<String, List<String>> MODULOS_ESPERADOS = Map.of(
            "CAJERO", List.of("/pacientes", "/citas", "/comprobantes"),
            "ENFERMERA", List.of("/pacientes", "/citas", "/historias"),
            "MEDICO", List.of("/historias", "/diagnosticos", "/tratamientos", "/examenes"),
            "FARMACEUTICO", List.of("/medicamentos"),
            "BIOLOGO", List.of("/laboratorio"),
            "RADIOLOGO", List.of("/imagenes"));

    private static final Map<String, List<String>> MODULOS_PROHIBIDOS = Map.of(
            "CAJERO", List.of("/historias", "/diagnosticos", "/usuarios"),
            "ENFERMERA", List.of("/comprobantes", "/usuarios"),
            "FARMACEUTICO", List.of("/historias", "/comprobantes"),
            "BIOLOGO", List.of("/comprobantes", "/historias"),
            "RADIOLOGO", List.of("/comprobantes", "/historias"),
            "MEDICO", List.of("/comprobantes"));

    @Test
    @DisplayName("FTC004 - Panel principal y menú según el rol")
    void ftc004() throws Exception {
        List<String> problemas = new ArrayList<>();
        Pattern href = Pattern.compile("href=\"(/[a-z\\-]+)\"");
        for (String rol : MODULOS_ESPERADOS.keySet()) {
            MockHttpSession s = sesion(rol);
            MvcResult r = mvc.perform(get("/", s)).andReturn();
            if (r.getResponse().getStatus() != 200 || !"index".equals(vista(r))) {
                problemas.add(rol + ": el panel no cargó (status " + r.getResponse().getStatus() + ")");
                continue;
            }
            Set<String> enlaces = new LinkedHashSet<>();
            Matcher m = href.matcher(html(r));
            while (m.find()) enlaces.add(m.group(1));
            for (String e : MODULOS_ESPERADOS.get(rol)) {
                if (!enlaces.contains(e)) problemas.add(rol + ": falta el módulo " + e);
            }
            for (String e : MODULOS_PROHIBIDOS.get(rol)) {
                if (enlaces.contains(e)) problemas.add(rol + ": muestra módulo no autorizado " + e);
            }
            // Cada enlace del menú debe ser accesible para el rol que lo ve
            for (String e : enlaces) {
                if (e.equals("/logout") || e.equals("/") || e.startsWith("/css") || e.startsWith("/js")) continue;
                MvcResult acceso = mvc.perform(get(e, s)).andReturn();
                if (denegado(acceso)) problemas.add(rol + ": el menú muestra " + e + " pero el acceso es denegado");
            }
        }
        assertTrue(problemas.isEmpty(), String.join(" | ", problemas));
    }

    @Test
    @DisplayName("FTC005 - Bloquear acceso por URL a un módulo no autorizado")
    void ftc005() throws Exception {
        MvcResult r1 = mvc.perform(get("/historias", sesion("CAJERO"))).andReturn();
        MvcResult r2 = mvc.perform(get("/citas/nuevo", sesion("FARMACEUTICO"))).andReturn();
        assertAll(() -> assertTrue(denegado(r1)), () -> assertTrue(denegado(r2)));
    }

    @Test
    @DisplayName("FTC006 - Buscar paciente por DNI completo y parcial")
    void ftc006() throws Exception {
        PacienteEntity p = paciente();
        String dni = p.getDnipaciente();
        for (String rol : List.of("ENFERMERA", "MEDICO", "CAJERO")) {
            MvcResult completo = mvc.perform(get("/pacientes?buscar=" + dni, sesion(rol))).andReturn();
            List<PacienteEntity> l1 = lista(completo, "pacientes");
            assertNotNull(l1, rol + " no pudo consultar pacientes");
            assertEquals(1, l1.size(), rol + ": DNI completo debe devolver un paciente");
            assertEquals(dni, l1.get(0).getDnipaciente());

            String parcial = dni.substring(0, 4);
            MvcResult r2 = mvc.perform(get("/pacientes?buscar=" + parcial, sesion(rol))).andReturn();
            List<PacienteEntity> l2 = lista(r2, "pacientes");
            assertFalse(l2.isEmpty());
            assertTrue(l2.stream().allMatch(x -> x.getDnipaciente().contains(parcial)
                    || x.getTelefono().contains(parcial) || x.getCorreoElectronico().contains(parcial)
                    || x.getNombrePaciente().contains(parcial) || x.getApellidoPaciente().contains(parcial)),
                    "Todos los resultados deben coincidir con el texto buscado");
        }
    }

    @Test
    @DisplayName("FTC007 - Buscar por nombre/apellido sin distinguir mayúsculas; sin resultados; listar todos")
    void ftc007() throws Exception {
        String apellido = "Torres" + siguiente();
        PacienteEntity p = paciente("María", apellido);
        MockHttpSession s = sesion("ENFERMERA");

        MvcResult r1 = mvc.perform(get("/pacientes?buscar=maria", s)).andReturn();
        MvcResult r2 = mvc.perform(get("/pacientes?buscar=" + apellido.toUpperCase(), s)).andReturn();
        MvcResult r3 = mvc.perform(get("/pacientes?buscar=zzzz", s)).andReturn();
        MvcResult r4 = mvc.perform(get("/pacientes?buscar=", s)).andReturn();
        Util.Pagina<?> pagina = (Util.Pagina<?>) modelo(r4, "pagina");
        long total = pacienteRepo.count();

        List<PacienteEntity> l1 = lista(r1, "pacientes");
        List<PacienteEntity> l2 = lista(r2, "pacientes");
        List<PacienteEntity> l3 = lista(r3, "pacientes");
        assertAll(
                () -> assertTrue(l1.stream().anyMatch(x -> x.getDnipaciente().equals(p.getDnipaciente())),
                        "Paso 1: buscar 'maria' (sin tilde) debería encontrar a 'María " + apellido + "' — resultados: " + l1.size()),
                () -> assertTrue(l2.stream().anyMatch(x -> x.getDnipaciente().equals(p.getDnipaciente())),
                        "Paso 2: buscar el apellido en mayúsculas debe encontrar al paciente"),
                () -> assertTrue(l3.isEmpty(), "Paso 3: 'zzzz' debe devolver lista vacía"),
                () -> assertTrue(html(r3).contains("No se encontraron pacientes."), "Paso 3: debe mostrar mensaje informativo"),
                () -> assertEquals(total, pagina.totalElementos(), "Paso 4: campo vacío debe listar a todos")
        );
    }

    @Test
    @DisplayName("FTC008 - Registrar un paciente nuevo (enfermera)")
    void ftc008() throws Exception {
        int n = siguiente();
        String dni = String.format("%08d", 20000000 + (n % 9000000));
        MvcResult r = mvc.perform(post("/pacientes/guardar", sesion("ENFERMERA"))
                .param("dnipaciente", dni).param("nombrePaciente", "Rosa").param("apellidoPaciente", "Quispe")
                .param("genero", "FEMENINO").param("telefono", String.format("7%08d", n % 100000000))
                .param("correoElectronico", "rosa" + n + "@mail.pe").param("fechaNacimiento", "1992-04-10")
                .param("tipoSangre", "A+")).andReturn();
        assertAll(
                () -> assertEquals("/pacientes", redirect(r)),
                () -> assertEquals("Paciente guardado correctamente.", flash(r, "mensaje")),
                () -> assertTrue(pacienteRepo.findById(dni).isPresent(), "El paciente debe quedar registrado")
        );
    }

    @Test
    @DisplayName("FTC009 - Actualizar datos de un paciente existente")
    void ftc009() throws Exception {
        PacienteEntity p = paciente();
        String nuevoTel = String.format("6%08d", siguiente() % 100000000);
        MvcResult r = mvc.perform(post("/pacientes/guardar", sesion("ENFERMERA"))
                .param("dnipaciente", p.getDnipaciente()).param("nombrePaciente", p.getNombrePaciente())
                .param("apellidoPaciente", p.getApellidoPaciente()).param("genero", "FEMENINO")
                .param("telefono", nuevoTel).param("correoElectronico", p.getCorreoElectronico())
                .param("fechaNacimiento", "1990-01-01").param("tipoSangre", "O+")).andReturn();
        PacienteEntity actualizado = pacienteRepo.findById(p.getDnipaciente()).orElseThrow();
        assertAll(
                () -> assertEquals("/pacientes", redirect(r)),
                () -> assertEquals(nuevoTel, actualizado.getTelefono()),
                () -> assertEquals(p.getDnipaciente(), actualizado.getDnipaciente())
        );
    }

    @Test
    @DisplayName("FTC010 - Impedir que un rol no autorizado registre pacientes")
    void ftc010() throws Exception {
        MvcResult r = mvc.perform(get("/pacientes/nuevo", sesion("CAJERO"))).andReturn();
        assertTrue(denegado(r));
    }
}
