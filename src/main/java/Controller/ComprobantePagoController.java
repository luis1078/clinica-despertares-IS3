package Controller;

import Entity.ComprobantePagoEntity;
import Entity.Emuns.EstadoPagoEnum;
import Entity.Emuns.MetodoPagoEnum;
import Entity.Emuns.TipoComprobanteEnum;
import Service.IComprobantePagoService;
import Service.IPacienteService;
import Util.Pagina;
import Util.RolHelper;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/comprobantes")
public class ComprobantePagoController {

    private final IComprobantePagoService comprobantePagoService;
    private final IPacienteService pacienteService;

    public ComprobantePagoController(IComprobantePagoService comprobantePagoService,
                                     IPacienteService pacienteService) {
        this.comprobantePagoService = comprobantePagoService;
        this.pacienteService = pacienteService;
    }

    @GetMapping
    public String listar(@RequestParam(value = "estado", required = false) EstadoPagoEnum estado,
                         @RequestParam(value = "pagina", defaultValue = "0") int numeroPagina,
                         Model model,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {

        if (!RolHelper.tieneRol(session, "CAJERO")) {
            return RolHelper.denegar(redirectAttributes, "No tiene permisos para consultar comprobantes.");
        }

        List<ComprobantePagoEntity> todos = estado == null ? comprobantePagoService.listarTodos() : comprobantePagoService.listarPorEstado(estado);
        Pagina<ComprobantePagoEntity> pagina = Pagina.de(todos, numeroPagina);
        model.addAttribute("comprobantes", pagina.contenido());
        model.addAttribute("pagina", pagina);
        model.addAttribute("estados", EstadoPagoEnum.values());
        model.addAttribute("estadoSeleccionado", estado);

        return "comprobantes/listar";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {

        if (!RolHelper.tieneRol(session, "CAJERO")) {
            return RolHelper.denegar(redirectAttributes, "Solo el cajero puede registrar comprobantes de pago.");
        }

        model.addAttribute("comprobante", new ComprobantePagoEntity());
        cargarCombos(model);

        return "comprobantes/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("comprobante") ComprobantePagoEntity comprobante,
                          @RequestParam("dniPaciente") String dniPaciente,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {

        if (!RolHelper.tieneRol(session, "CAJERO")) {
            return RolHelper.denegar(redirectAttributes, "No tiene permisos para guardar comprobantes de pago.");
        }

        if (comprobante.getCodcomprobante() == null) {
            comprobantePagoService.registrarComprobante(dniPaciente, comprobante);
        } else {
            ComprobantePagoEntity comprobanteExistente = comprobantePagoService.buscarPorId(comprobante.getCodcomprobante())
                    .orElseThrow(() -> new IllegalArgumentException("No existe el comprobante: " + comprobante.getCodcomprobante()));
            comprobanteExistente.setFechaEmision(comprobante.getFechaEmision());
            comprobanteExistente.setTipoComprobante(comprobante.getTipoComprobante());
            comprobanteExistente.setMetodoPago(comprobante.getMetodoPago());
            comprobanteExistente.setEstado(comprobante.getEstado());
            comprobanteExistente.setPaciente(pacienteService.buscarPorId(dniPaciente)
                    .orElseThrow(() -> new IllegalArgumentException("No existe el paciente: " + dniPaciente)));
            comprobantePagoService.guardar(comprobanteExistente);
        }

        redirectAttributes.addFlashAttribute("mensaje", "Comprobante guardado correctamente.");

        return "redirect:/comprobantes";
    }

    @GetMapping("/editar/{codComprobante}")
    public String editar(@PathVariable Long codComprobante,
                         Model model,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {

        if (!RolHelper.tieneRol(session, "CAJERO")) {
            return RolHelper.denegar(redirectAttributes, "No tiene permisos para editar comprobantes de pago.");
        }

        ComprobantePagoEntity comprobante = comprobantePagoService.buscarPorId(codComprobante)
                .orElseThrow(() -> new IllegalArgumentException("No existe el comprobante: " + codComprobante));

        model.addAttribute("comprobante", comprobante);
        cargarCombos(model);

        return "comprobantes/formulario";
    }

    @PostMapping("/cancelar/{codComprobante}")
    public String cancelar(@PathVariable Long codComprobante,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {

        if (!RolHelper.tieneRol(session, "CAJERO")) {
            return RolHelper.denegar(redirectAttributes, "No tiene permisos para cancelar comprobantes.");
        }

        comprobantePagoService.cancelarComprobante(codComprobante);
        redirectAttributes.addFlashAttribute("mensaje", "Comprobante marcado como cancelado.");

        return "redirect:/comprobantes";
    }

    @PostMapping("/pendiente/{codComprobante}")
    public String pendiente(@PathVariable Long codComprobante,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {

        if (!RolHelper.tieneRol(session, "CAJERO")) {
            return RolHelper.denegar(redirectAttributes, "No tiene permisos para cambiar el estado del comprobante.");
        }

        comprobantePagoService.marcarComoFaltaPagar(codComprobante);
        redirectAttributes.addFlashAttribute("mensaje", "Comprobante marcado como falta pagar.");

        return "redirect:/comprobantes";
    }

    @PostMapping("/eliminar/{codComprobante}")
    public String eliminar(@PathVariable Long codComprobante,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {

        if (!RolHelper.tieneRol(session, "CAJERO")) {
            return RolHelper.denegar(redirectAttributes, "No tiene permisos para eliminar comprobantes.");
        }

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
