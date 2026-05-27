package Controller;

import Entity.DetalleComprobanteEntity;
import Entity.Emuns.TipoItemEnum;
import Service.IComprobantePagoService;
import Service.IDetalleComprobanteService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/detalle-comprobantes")
public class DetalleComprobanteController {

    private final IDetalleComprobanteService detalleComprobanteService;
    private final IComprobantePagoService comprobantePagoService;

    public DetalleComprobanteController(IDetalleComprobanteService detalleComprobanteService,
                                        IComprobantePagoService comprobantePagoService) {
        this.detalleComprobanteService = detalleComprobanteService;
        this.comprobantePagoService = comprobantePagoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("detalles", detalleComprobanteService.listarTodos());
        return "detalle-comprobantes/listar";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("detalle", new DetalleComprobanteEntity());
        cargarCombos(model);
        return "detalle-comprobantes/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("detalle") DetalleComprobanteEntity detalle,
                          @RequestParam("codComprobante") Long codComprobante,
                          @RequestParam("tipoItem") TipoItemEnum tipoItem,
                          @RequestParam("idReferencia") Long idReferencia,
                          RedirectAttributes redirectAttributes) {
        if (detalle.getIddetalle() == null) {
            switch (tipoItem) {
                case CITA:
                    detalleComprobanteService.registrarDetallePorCita(codComprobante, idReferencia, detalle);
                    break;
                case EXAMEN:
                    detalleComprobanteService.registrarDetallePorExamen(codComprobante, idReferencia, detalle);
                    break;
                case MEDICAMENTO:
                    detalleComprobanteService.registrarDetallePorMedicamento(codComprobante, idReferencia, detalle);
                    break;
                default:
                    throw new IllegalArgumentException("Tipo de ítem no válido.");
            }
        } else {
            DetalleComprobanteEntity detalleExistente = detalleComprobanteService.buscarPorId(detalle.getIddetalle())
                    .orElseThrow(() -> new IllegalArgumentException("No existe el detalle: " + detalle.getIddetalle()));
            detalleExistente.setComprobantePago(comprobantePagoService.buscarPorId(codComprobante)
                    .orElseThrow(() -> new IllegalArgumentException("No existe el comprobante: " + codComprobante)));
            detalleExistente.setTipoItem(tipoItem);
            detalleExistente.setIdReferencia(idReferencia);
            detalleExistente.setDescripcionDetalle(detalle.getDescripcionDetalle());
            detalleExistente.setCantidad(detalle.getCantidad());
            detalleExistente.setPrecioUnitario(detalle.getPrecioUnitario());
            detalleComprobanteService.guardar(detalleExistente);
        }
        redirectAttributes.addFlashAttribute("mensaje", "Detalle de comprobante guardado correctamente.");
        return "redirect:/detalle-comprobantes";
    }

    @GetMapping("/editar/{idDetalle}")
    public String editar(@PathVariable Long idDetalle, Model model) {
        DetalleComprobanteEntity detalle = detalleComprobanteService.buscarPorId(idDetalle)
                .orElseThrow(() -> new IllegalArgumentException("No existe el detalle: " + idDetalle));
        model.addAttribute("detalle", detalle);
        cargarCombos(model);
        return "detalle-comprobantes/formulario";
    }

    @GetMapping("/eliminar/{idDetalle}")
    public String eliminar(@PathVariable Long idDetalle, RedirectAttributes redirectAttributes) {
        detalleComprobanteService.eliminar(idDetalle);
        redirectAttributes.addFlashAttribute("mensaje", "Detalle de comprobante eliminado correctamente.");
        return "redirect:/detalle-comprobantes";
    }

    private void cargarCombos(Model model) {
        model.addAttribute("comprobantes", comprobantePagoService.listarTodos());
        model.addAttribute("tiposItem", TipoItemEnum.values());
    }
}
