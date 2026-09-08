package Config;

import Util.CsrfTokenHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.Set;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Set<String> METODOS_MUTANTES = Set.of("POST", "PUT", "DELETE", "PATCH");

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws IOException {

        String uri = request.getRequestURI();

        if (esRutaPublica(uri)) {
            if (esMetodoMutante(request) && !tokenCsrfValido(request)) {
                response.sendRedirect("/acceso-denegado");
                return false;
            }
            return true;
        }

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            response.sendRedirect("/login");
            return false;
        }

        if (esMetodoMutante(request) && !tokenCsrfValido(request)) {
            response.sendRedirect("/acceso-denegado");
            return false;
        }

        String rol = (String) session.getAttribute("rolUsuario");

        if (!tienePermiso(uri, rol)) {
            response.sendRedirect("/acceso-denegado");
            return false;
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           ModelAndView modelAndView) {

        if (modelAndView == null || !modelAndView.hasView()) {
            return;
        }

        String viewName = modelAndView.getViewName();
        if (viewName != null && viewName.startsWith("redirect:")) {
            return;
        }

        // Se fuerza la creación de sesión (si no existía) porque cualquier vista pública
        // puede traer un formulario que necesita token CSRF, y no todos los controllers
        // piden HttpSession como parámetro (p. ej. GET /registro).
        HttpSession session = request.getSession(true);
        modelAndView.addObject("csrfToken", CsrfTokenHelper.obtenerOCrearToken(session));

        // #request ya no está disponible por defecto en las plantillas (Thymeleaf 3.1+),
        // así que se expone la URI actual aquí para que el sidebar marque el ítem activo.
        modelAndView.addObject("currentUri", request.getRequestURI());
    }

    private boolean esMetodoMutante(HttpServletRequest request) {
        return METODOS_MUTANTES.contains(request.getMethod());
    }

    private boolean tokenCsrfValido(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }

        String tokenSesion = (String) session.getAttribute(CsrfTokenHelper.ATRIBUTO_SESION);
        String tokenRequest = request.getParameter(CsrfTokenHelper.PARAMETRO_REQUEST);

        return tokenSesion != null && tokenSesion.equals(tokenRequest);
    }

    private boolean esRutaPublica(String uri) {
        return uri.equals("/")
                || uri.equals("/login")
                || uri.equals("/registro")
                || uri.equals("/acceso-denegado")
                || uri.equals("/error")
                || uri.equals("/favicon.ico")
                || uri.startsWith("/css/")
                || uri.startsWith("/js/")
                || uri.startsWith("/images/")
                || uri.startsWith("/webjars/");
    }

    private boolean tienePermiso(String uri, String rol) {
        if (rol == null) {
            return false;
        }

        if (uri.startsWith("/usuarios")) {
            return esRol(rol, "MEDICO");
        }

        if (uri.equals("/pacientes") || uri.equals("/pacientes/")) {
            return esRol(rol, "CAJERO", "ENFERMERA", "MEDICO");
        }

        if (uri.startsWith("/pacientes")) {
            return esRol(rol, "ENFERMERA", "MEDICO");
        }

        if (uri.startsWith("/medicos")) {
            return esRol(rol, "CAJERO", "ENFERMERA", "MEDICO");
        }

        if (uri.equals("/citas") || uri.equals("/citas/")) {
            return esRol(rol, "CAJERO", "ENFERMERA", "MEDICO");
        }

        if (uri.startsWith("/citas")) {
            return esRol(rol, "ENFERMERA", "MEDICO");
        }

        if (uri.startsWith("/historias")) {
            return esRol(rol, "ENFERMERA", "MEDICO");
        }

        if (uri.startsWith("/diagnosticos")) {
            return esRol(rol, "MEDICO", "BIOLOGO", "RADIOLOGO");
        }

        if (uri.startsWith("/tratamientos") || uri.startsWith("/detalle-tratamientos")) {
            return esRol(rol, "MEDICO", "FARMACEUTICO");
        }

        if (uri.startsWith("/comprobantes") || uri.startsWith("/detalle-comprobantes")) {
            return esRol(rol, "CAJERO");
        }

        if (uri.startsWith("/medicamentos")) {
            // El listado también lo puede consultar CAJERO; MedicamentoController
            // ya restringe las acciones de escritura solo a FARMACEUTICO.
            return esRol(rol, "FARMACEUTICO", "CAJERO");
        }

        if (uri.startsWith("/proveedores")) {
            return esRol(rol, "FARMACEUTICO");
        }

        if (uri.startsWith("/examenes")) {
            return esRol(rol, "MEDICO", "BIOLOGO", "RADIOLOGO");
        }

        if (uri.equals("/laboratorio") || uri.equals("/laboratorio/")) {
            return esRol(rol, "BIOLOGO", "MEDICO");
        }

        if (uri.startsWith("/laboratorio")) {
            return esRol(rol, "BIOLOGO");
        }

        if (uri.equals("/imagenes") || uri.equals("/imagenes/")) {
            return esRol(rol, "RADIOLOGO", "MEDICO");
        }

        if (uri.startsWith("/imagenes")) {
            return esRol(rol, "RADIOLOGO");
        }

        return true;
    }

    private boolean esRol(String rolActual, String... rolesPermitidos) {
        Set<String> permitidos = Set.of(rolesPermitidos);
        return permitidos.contains(rolActual);
    }
}
