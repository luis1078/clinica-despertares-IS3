package Controller;

import Entity.DetalleTratamientoEntity;
import Service.IDetalleTratamientoService;
import Service.IMedicamentoService;
import Service.ITratamientoService;
import Util.Pagina;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/detalle-tratamientos")
public class DetalleTratamientoController {

    private final IDetalleTratamientoService detalleTratamientoService;
    private final ITratamientoService tratamientoService;
    private final IMedicamentoService medicamentoService;

    public DetalleTratamientoController(IDetalleTratamientoService detalleTratamientoService,
                                        ITratamientoService tratamientoService,
                                        IMedicamentoService medicamentoService) {
        this.detalleTratamientoService = detalleTratamientoService;
        this.tratamientoService = tratamientoService;
        this.medicamentoService = medicamentoService;
    }

    @GetMapping
    public String listar(@RequestParam(value = "pagina", defaultValue = "0") int numeroPagina, Model model) {
        Pagina<DetalleTratamientoEntity> pagina = Pagina.de(detalleTratamientoService.listarTodos(), numeroPagina);
        model.addAttribute("detalles", pagina.contenido());
        model.addAttribute("pagina", pagina);
        return "detalle-tratamientos/listar";
    }

    @GetMapping("/nuevo")
    public String nuevo(@RequestParam(value = "idTratamiento", required = false) Long idTratamiento, Model model) {
        model.addAttribute("detalle", new DetalleTratamientoEntity());
        model.addAttribute("tratamientoSeleccionado", idTratamiento);
        cargarCombos(model);
        return "detalle-tratamientos/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("detalle") DetalleTratamientoEntity detalle,
                          @RequestParam("idTratamiento") Long idTratamiento,
                          @RequestParam("codMedicamento") Long codMedicamento,
                          RedirectAttributes redirectAttributes) {
        if (detalle.getIdDetalleTratamiento() == null) {
            detalleTratamientoService.registrarDetalleTratamiento(idTratamiento, codMedicamento, detalle);
        } else {
            DetalleTratamientoEntity detalleExistente = detalleTratamientoService.buscarPorId(detalle.getIdDetalleTratamiento())
                    .orElseThrow(() -> new IllegalArgumentException("No existe el detalle: " + detalle.getIdDetalleTratamiento()));
            detalleExistente.setDosisIndicada(detalle.getDosisIndicada());
            detalleExistente.setFrecuenciaDeToma(detalle.getFrecuenciaDeToma());
            detalleExistente.setObservaciones(detalle.getObservaciones());
            detalleExistente.setTratamiento(tratamientoService.buscarPorId(idTratamiento)
                    .orElseThrow(() -> new IllegalArgumentException("No existe el tratamiento: " + idTratamiento)));
            detalleExistente.setMedicamento(medicamentoService.buscarPorId(codMedicamento)
                    .orElseThrow(() -> new IllegalArgumentException("No existe el medicamento: " + codMedicamento)));
            detalleTratamientoService.guardar(detalleExistente);
        }
        redirectAttributes.addFlashAttribute("mensaje", "Detalle de tratamiento guardado correctamente.");
        return "redirect:/detalle-tratamientos";
    }

    @GetMapping("/editar/{idDetalleTratamiento}")
    public String editar(@PathVariable Long idDetalleTratamiento, Model model) {
        DetalleTratamientoEntity detalle = detalleTratamientoService.buscarPorId(idDetalleTratamiento)
                .orElseThrow(() -> new IllegalArgumentException("No existe el detalle: " + idDetalleTratamiento));
        model.addAttribute("detalle", detalle);
        cargarCombos(model);
        return "detalle-tratamientos/formulario";
    }

    @PostMapping("/eliminar/{idDetalleTratamiento}")
    public String eliminar(@PathVariable Long idDetalleTratamiento, RedirectAttributes redirectAttributes) {
        detalleTratamientoService.eliminar(idDetalleTratamiento);
        redirectAttributes.addFlashAttribute("mensaje", "Detalle de tratamiento eliminado correctamente.");
        return "redirect:/detalle-tratamientos";
    }

    private void cargarCombos(Model model) {
        model.addAttribute("tratamientos", tratamientoService.listarTodos());
        model.addAttribute("medicamentos", medicamentoService.listarTodos());
    }
}
