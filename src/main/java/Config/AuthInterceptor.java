package Config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Set;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws IOException {

        String uri = request.getRequestURI();

        if (esRutaPublica(uri)) {
            return true;
        }

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            response.sendRedirect("/");
            return false;
        }

        String rol = (String) session.getAttribute("rolUsuario");

        if (!tienePermiso(uri, rol)) {
            response.sendRedirect("/acceso-denegado");
            return false;
        }

        return true;
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

        if (uri.startsWith("/pacientes")) {
            return esRol(rol, "CAJERO", "ENFERMERA", "MEDICO");
        }

        if (uri.startsWith("/medicos")) {
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

        if (uri.startsWith("/medicamentos") || uri.startsWith("/proveedores")) {
            return esRol(rol, "FARMACEUTICO");
        }

        if (uri.startsWith("/examenes")) {
            return esRol(rol, "MEDICO", "BIOLOGO", "RADIOLOGO");
        }

        if (uri.startsWith("/laboratorio")) {
            return esRol(rol, "BIOLOGO");
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
