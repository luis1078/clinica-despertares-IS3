package Util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Paginación en memoria usada por los 15 listados del sistema; cubre los bordes
 * más propensos a error (lista vacía, última página incompleta, número de
 * página fuera de rango) ya que no hay ningún test de por medio con la BD real.
 */
class PaginaTest {

    private final List<Integer> veinteElementos = java.util.stream.IntStream.rangeClosed(1, 20).boxed().toList();

    @Test
    void primeraPagina_devuelveLosPrimerosElementosYMarcaSoloSiguiente() {
        Pagina<Integer> pagina = Pagina.de(veinteElementos, 0, 10);

        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), pagina.contenido());
        assertEquals(2, pagina.totalPaginas());
        assertFalse(pagina.tieneAnterior());
        assertTrue(pagina.tieneSiguiente());
    }

    @Test
    void ultimaPaginaIncompleta_devuelveSoloLoQueQueda() {
        Pagina<Integer> pagina = Pagina.de(veinteElementos, 1, 15);

        assertEquals(List.of(16, 17, 18, 19, 20), pagina.contenido());
        assertEquals(2, pagina.totalPaginas());
        assertTrue(pagina.tieneAnterior());
        assertFalse(pagina.tieneSiguiente());
    }

    @Test
    void listaVacia_noRompeYDevuelveUnaSolaPaginaVacia() {
        Pagina<Integer> pagina = Pagina.de(List.of(), 0, 10);

        assertTrue(pagina.contenido().isEmpty());
        assertEquals(0, pagina.totalElementos());
        assertEquals(1, pagina.totalPaginas());
        assertFalse(pagina.tieneAnterior());
        assertFalse(pagina.tieneSiguiente());
    }

    @Test
    void numeroDePaginaFueraDeRango_seAjustaALaUltimaPaginaValida() {
        Pagina<Integer> pagina = Pagina.de(veinteElementos, 99, 10);

        assertEquals(1, pagina.numero());
        assertEquals(List.of(11, 12, 13, 14, 15, 16, 17, 18, 19, 20), pagina.contenido());
    }

    @Test
    void numeroDePaginaNegativo_seAjustaACero() {
        Pagina<Integer> pagina = Pagina.de(veinteElementos, -5, 10);

        assertEquals(0, pagina.numero());
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), pagina.contenido());
    }
}
