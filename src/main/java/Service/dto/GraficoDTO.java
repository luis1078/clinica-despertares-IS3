package Service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Un dataset listo para pintar en un gráfico de Chart.js: una etiqueta por
 * cada barra/porción y su valor correspondiente, en el mismo orden.
 * {@code Controller.HomeController} arma estos objetos y la plantilla
 * {@code index.html} los vuelca a JSON con la inlining de Thymeleaf
 * (th:inline="javascript") para no depender de un endpoint REST aparte.
 */
@Getter
@Setter
@AllArgsConstructor
public class GraficoDTO {
    private String titulo;
    private List<String> etiquetas;
    private List<Number> valores;
}
