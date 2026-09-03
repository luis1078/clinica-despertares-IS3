package Util;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;

public final class RolHelper {

    private RolHelper() {
    }

    public static boolean tieneRol(HttpSession session, String... rolesPermitidos) {

        if (session == null || session.getAttribute("rolUsuario") == null) {
            return false;
        }

        String rolActual = session.getAttribute("rolUsuario").toString();

        return Arrays.stream(rolesPermitidos)
                .anyMatch(rolPermitido -> rolPermitido.equals(rolActual));
    }

    public static String denegar(RedirectAttributes redirectAttributes, String mensaje) {
        redirectAttributes.addFlashAttribute("error", mensaje);
        return "redirect:/acceso-denegado";
    }
}
