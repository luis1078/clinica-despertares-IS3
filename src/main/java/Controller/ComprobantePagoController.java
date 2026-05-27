package Controller;

import Entity.ComprobantePagoEntity;
import Entity.Emuns.EstadoPagoEnum;
import Entity.Emuns.MetodoPagoEnum;
import Entity.Emuns.TipoComprobanteEnum;
import Service.IComprobantePagoService;
import Service.IPacienteService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/comprobantes")
public class ComprobantePagoController {

    private final IComprobantePagoService comprobantePagoService;
    private final IPacienteService pacienteService;

    public ComprobantePagoController(IComprobantePagoService comprobantePagoService, IPacienteService pacienteService) {
        this.comprobantePagoService = comprobantePagoService;
        this.pacienteService = pacienteService;
    }

    @GetMapping
    public String listar(@RequestParam(value = "estado", required = false) EstadoPagoEnum estado, Model model) {
        model.addAttribute("comprobantes", estado == null
                ? comprobantePagoService.listarTodos()
                : comprobantePagoService.listarPorEstado(estado));

        model.addAttribute("estados", EstadoPagoEnum.values());
        model.addAttribute("estadoSeleccionado", estado);

        return "comprobantes/listar";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("comprobante", new ComprobantePagoEntity());
        cargarCombos(model);
        return "comprobantes/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("comprobante") ComprobantePagoEntity comprobante,
                          @RequestParam("dniPaciente") String dniPaciente,
                          RedirectAttributes redirectAttributes) {
        if (comprobante.getCodcomprobante() == null) {
            comprobantePagoService.registrarComprobante(dniPaciente, comprobante);
        } else {
            comprobante.setPaciente(pacienteService.buscarPorId(dniPaciente)
                    .orElseThrow(() -> new IllegalArgumentException("No existe el paciente: " + dniPaciente)));
            comprobantePagoService.guardar(comprobante);
        }
        redirectAttributes.addFlashAttribute("mensaje", "Comprobante guardado correctamente.");
        return "redirect:/comprobantes";
    }

    @GetMapping("/editar/{codComprobante}")
    public String editar(@PathVariable Long codComprobante, Model model) {
        ComprobantePagoEntity comprobante = comprobantePagoService.buscarPorId(codComprobante)
                .orElseThrow(() -> new IllegalArgumentException("No existe el comprobante: " + codComprobante));
        model.addAttribute("comprobante", comprobante);
        cargarCombos(model);
        return "comprobantes/formulario";
    }

    @GetMapping("/cancelar/{codComprobante}")
    public String cancelar(@PathVariable Long codComprobante, RedirectAttributes redirectAttributes) {
        comprobantePagoService.cancelarComprobante(codComprobante);
        redirectAttributes.addFlashAttribute("mensaje", "Comprobante marcado como cancelado.");
        return "redirect:/comprobantes";
    }

    @GetMapping("/pendiente/{codComprobante}")
    public String pendiente(@PathVariable Long codComprobante, RedirectAttributes redirectAttributes) {
        comprobantePagoService.marcarComoFaltaPagar(codComprobante);
        redirectAttributes.addFlashAttribute("mensaje", "Comprobante marcado como falta pagar.");
        return "redirect:/comprobantes";
    }

    @GetMapping("/eliminar/{codComprobante}")
    public String eliminar(@PathVariable Long codComprobante, RedirectAttributes redirectAttributes) {
        comprobantePagoService.eliminar(codComprobante);
        redirectAttributes.addFlashAttribute("mensaje", "Comprobante eliminado correctamente.");
        return "redirect:/comprobantes";
    }

    private void cargarCombos(Model model) {
        model.addAttribute("pacientes", pacienteService.listarTodos());
        model.addAttribute("tipos", TipoComprobanteEnum.values());
        model.addAttribute("metodos", MetodoPagoEnum.values());
        model.addAttribute("estados", EstadoPagoEnum.values());
    }
}
