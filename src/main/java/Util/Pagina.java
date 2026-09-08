package Util;

import java.util.Collections;
import java.util.List;

/**
 * Paginación en memoria sobre una lista ya cargada por el service (los listados
 * de este proyecto no usan Pageable a nivel de repositorio). Es adecuada para los
 * volúmenes de datos de una clínica; si en algún momento una tabla crece mucho,
 * lo correcto sería mover esa consulta a Spring Data Pageable en el repositorio.
 */
public record Pagina<T>(List<T> contenido, int numero, int tamano, long totalElementos) {

    public static final int TAMANO_DEFECTO = 10;

    public static <T> Pagina<T> de(List<T> lista, int numeroSolicitado, int tamano) {
        int total = lista.size();
        int totalPaginas = calcularTotalPaginas(total, tamano);
        int numero = Math.max(0, Math.min(numeroSolicitado, totalPaginas - 1));

        int desde = numero * tamano;
        int hasta = Math.min(desde + tamano, total);
        List<T> contenido = desde >= hasta ? Collections.emptyList() : lista.subList(desde, hasta);

        return new Pagina<>(contenido, numero, tamano, total);
    }

    public static <T> Pagina<T> de(List<T> lista, int numeroSolicitado) {
        return de(lista, numeroSolicitado, TAMANO_DEFECTO);
    }

    public int totalPaginas() {
        return calcularTotalPaginas((int) totalElementos, tamano);
    }

    public boolean tieneAnterior() {
        return numero > 0;
    }

    public boolean tieneSiguiente() {
        return numero < totalPaginas() - 1;
    }

    private static int calcularTotalPaginas(int total, int tamano) {
        return tamano <= 0 ? 0 : Math.max(1, (int) Math.ceil((double) total / tamano));
    }
}
