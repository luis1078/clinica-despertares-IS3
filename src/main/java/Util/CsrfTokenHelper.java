package Util;

import jakarta.servlet.http.HttpSession;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Implementación mínima del patrón "synchronizer token" para proteger contra CSRF
 * sin depender de Spring Security. El token vive en la sesión del usuario y debe
 * viajar en un campo oculto "_csrf" en cada formulario que modifique estado.
 */
public final class CsrfTokenHelper {

    public static final String ATRIBUTO_SESION = "csrfToken";
    public static final String PARAMETRO_REQUEST = "_csrf";

    private static final SecureRandom RANDOM = new SecureRandom();

    private CsrfTokenHelper() {
    }

    public static String obtenerOCrearToken(HttpSession session) {
        String token = (String) session.getAttribute(ATRIBUTO_SESION);

        if (token == null) {
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            session.setAttribute(ATRIBUTO_SESION, token);
        }

        return token;
    }
}
