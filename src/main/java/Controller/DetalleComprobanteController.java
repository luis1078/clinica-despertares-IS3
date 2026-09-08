package Controller;

import Entity.DetalleComprobanteEntity;
import Entity.Emuns.TipoItemEnum;
import Service.ICitaMedicaService;
import Service.IComprobantePagoService;
import Service.IDetalleComprobanteService;
import Service.IExamenMedicoService;
import Service.IMedicamentoService;
import Util.Pagina;
import Util.RolHelper;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/detalle-comprobantes")
public class DetalleComprobanteController {

    private final IDetalleComprobanteService detalleComprobanteService;
    private final IComprobantePagoService comprobantePagoService;
    private final ICitaMedicaService citaMedicaService;
    private final IExamenMedicoService examenMedicoService;
    private final IMedicamentoService medicamentoService;

    public DetalleComprobanteController(IDetalleComprobanteService detalleComprobanteService,
                                        IComprobantePagoService comprobantePagoService,
                                        ICitaMedicaService citaMedicaService,
                                        IExamenMedicoService examenMedicoService,
                                        IMedicamentoService medicamentoService) {
        this.detalleComprobanteService = detalleComprobanteService;
        this.comprobantePagoService = comprobantePagoService;
        this.citaMedicaService = citaMedicaService;
        this.examenMedicoService = examenMedicoService;
        this.medicamentoService = medicamentoService;
    }

    @GetMapping
    public String listar(@RequestParam(value = "pagina", defaultValue = "0") int numeroPagina,
                         Model model,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {

        if (!RolHelper.tieneRol(session, "CAJERO")) {
            return RolHelper.denegar(redirectAttributes, "No tiene permisos para consultar detalles de comprobante.");
        }

        Pagina<DetalleComprobanteEntity> pagina = Pagina.de(detalleComprobanteService.listarTodos(), numeroPagina);
        model.addAttribute("detalles", pagina.contenido());
        model.addAttribute("pagina", pagina);

        return "detalle-comprobantes/listar";
    }

    @GetMapping("/nuevo")
    public String nuevo(@RequestParam(value = "codComprobante", required = false) Long codComprobante,
                        Model model,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {

        if (!RolHelper.tieneRol(session, "CAJERO")) {
            return RolHelper.denegar(redirectAttributes, "No tiene permisos para registrar detalles de comprobante.");
        }

        model.addAttribute("detalle", new DetalleComprobanteEntity());
        model.addAttribute("comprobanteSeleccionado", codComprobante);
        cargarCombos(model);

        return "detalle-comprobantes/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("detalle") DetalleComprobanteEntity detalle,
                          @RequestParam("codComprobante") Long codComprobante,
                          @RequestParam("tipoItem") TipoItemEnum tipoItem,
                          @RequestParam(value = "codCitaMedica", required = false) Long codCitaMedica,
                          @RequestParam(value = "codExamenMedico", required = false) Long codExamenMedico,
                          @RequestParam(value = "codMedicamento", required = false) Long codMedicamento,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {

        if (!RolHelper.tieneRol(session, "CAJERO")) {
            return RolHelper.denegar(redirectAttributes, "No tiene permisos para guardar detalles de comprobante.");
        }

        try {
            Long idReferencia = resolverReferencia(tipoItem, codCitaMedica, codExamenMedico, codMedicamento);
            validarReferencia(tipoItem, idReferencia);

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
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/detalle-comprobantes";
    }

    @GetMapping("/editar/{idDetalle}")
    public String editar(@PathVariable Long idDetalle,
                         Model model,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {

        if (!RolHelper.tieneRol(session, "CAJERO")) {
            return RolHelper.denegar(redirectAttributes, "No tiene permisos para editar detalles de comprobante.");
        }

        DetalleComprobanteEntity detalle = detalleComprobanteService.buscarPorId(idDetalle)
                .orElseThrow(() -> new IllegalArgumentException("No existe el detalle: " + idDetalle));

        model.addAttribute("detalle", detalle);
        cargarCombos(model);

        return "detalle-comprobantes/formulario";
    }

    @PostMapping("/eliminar/{idDetalle}")
    public String eliminar(@PathVariable Long idDetalle,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {

        if (!RolHelper.tieneRol(session, "CAJERO")) {
            return RolHelper.denegar(redirectAttributes, "No tiene permisos para eliminar detalles de comprobante.");
        }

        try {
            detalleComprobanteService.eliminar(idDetalle);
            redirectAttributes.addFlashAttribute("mensaje", "Detalle de comprobante eliminado correctamente.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/detalle-comprobantes";
    }

    private void cargarCombos(Model model) {
        model.addAttribute("comprobantes", comprobantePagoService.listarTodos());
        model.addAttribute("tiposItem", TipoItemEnum.values());
        model.addAttribute("citas", citaMedicaService.listarTodos());
        model.addAttribute("examenes", examenMedicoService.listarTodos());
        model.addAttribute("medicamentos", medicamentoService.listarTodos());
    }

    private Long resolverReferencia(TipoItemEnum tipoItem,
                                    Long codCitaMedica,
                                    Long codExamenMedico,
                                    Long codMedicamento) {
        if (tipoItem == null) {
            throw new IllegalArgumentException("Debe seleccionar el tipo de ítem.");
        }

        return switch (tipoItem) {
            case CITA -> codCitaMedica;
            case EXAMEN -> codExamenMedico;
            case MEDICAMENTO -> codMedicamento;
        };
    }

    private void validarReferencia(TipoItemEnum tipoItem, Long idReferencia) {
        if (idReferencia == null) {
            throw new IllegalArgumentException("Debe seleccionar la referencia del ítem.");
        }

        switch (tipoItem) {
            case CITA -> citaMedicaService.buscarPorId(idReferencia)
                    .orElseThrow(() -> new IllegalArgumentException("No existe la cita médica seleccionada."));
            case EXAMEN -> examenMedicoService.buscarPorId(idReferencia)
                    .orElseThrow(() -> new IllegalArgumentException("No existe el examen médico seleccionado."));
            case MEDICAMENTO -> medicamentoService.buscarPorId(idReferencia)
                    .orElseThrow(() -> new IllegalArgumentException("No existe el medicamento seleccionado."));
        }
    }
}
