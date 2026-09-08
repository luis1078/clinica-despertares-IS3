package Config;

import Util.CsrfTokenHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cubre el control de acceso por rol y la validación de CSRF de AuthInterceptor,
 * que hasta ahora no tenían ninguna prueba pese a ser la barrera de seguridad
 * principal de la app (no hay Spring Security de por medio).
 */
class AuthInterceptorTest {

    private final AuthInterceptor interceptor = new AuthInterceptor();

    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        response = new MockHttpServletResponse();
    }

    private MockHttpServletRequest requestSinSesion(String metodo, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(metodo, uri);
        request.setRequestURI(uri);
        return request;
    }

    private MockHttpServletRequest requestConSesion(String metodo, String uri, String rol) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuarioLogueado", "cualquiera");
        session.setAttribute("rolUsuario", rol);

        MockHttpServletRequest request = new MockHttpServletRequest(metodo, uri);
        request.setRequestURI(uri);
        request.setSession(session);
        return request;
    }

    @Test
    void rutaProtegida_sinSesion_redirigeALogin() throws Exception {
        boolean continua = interceptor.preHandle(requestSinSesion("GET", "/pacientes"), response, new Object());

        assertFalse(continua);
        assertEquals("/login", response.getRedirectedUrl());
    }

    @Test
    void rutaPublica_sinSesion_permiteElPaso() throws Exception {
        boolean continua = interceptor.preHandle(requestSinSesion("GET", "/login"), response, new Object());

        assertTrue(continua);
    }

    @Test
    void rutaProtegida_conRolSinPermiso_redirigeAAccesoDenegado() throws Exception {
        // /usuarios es exclusivo de MEDICO según tienePermiso().
        boolean continua = interceptor.preHandle(requestConSesion("GET", "/usuarios", "ENFERMERA"), response, new Object());

        assertFalse(continua);
        assertEquals("/acceso-denegado", response.getRedirectedUrl());
    }

    @Test
    void rutaProtegida_conRolPermitido_permiteElPaso() throws Exception {
        boolean continua = interceptor.preHandle(requestConSesion("GET", "/usuarios", "MEDICO"), response, new Object());

        assertTrue(continua);
    }

    @Test
    void metodoMutante_sinTokenCsrf_redirigeAAccesoDenegado() throws Exception {
        boolean continua = interceptor.preHandle(requestConSesion("POST", "/pacientes/guardar", "MEDICO"), response, new Object());

        assertFalse(continua);
        assertEquals("/acceso-denegado", response.getRedirectedUrl());
    }

    @Test
    void metodoMutante_conTokenCsrfValido_permiteElPaso() throws Exception {
        MockHttpServletRequest request = requestConSesion("POST", "/pacientes/guardar", "MEDICO");
        String token = CsrfTokenHelper.obtenerOCrearToken(request.getSession());
        request.setParameter(CsrfTokenHelper.PARAMETRO_REQUEST, token);

        boolean continua = interceptor.preHandle(request, response, new Object());

        assertTrue(continua);
    }

    @Test
    void metodoMutante_conTokenCsrfInvalido_redirigeAAccesoDenegado() throws Exception {
        MockHttpServletRequest request = requestConSesion("POST", "/pacientes/guardar", "MEDICO");
        CsrfTokenHelper.obtenerOCrearToken(request.getSession());
        request.setParameter(CsrfTokenHelper.PARAMETRO_REQUEST, "token-falso");

        boolean continua = interceptor.preHandle(request, response, new Object());

        assertFalse(continua);
        assertEquals("/acceso-denegado", response.getRedirectedUrl());
    }
}
