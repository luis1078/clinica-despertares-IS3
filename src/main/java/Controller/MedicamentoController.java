package Controller;

import Entity.MedicamentoEntity;
import Service.IMedicamentoService;
import Service.IProveedorService;
import Util.Pagina;
import Util.RolHelper;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/medicamentos")
public class MedicamentoController {

    private final IMedicamentoService medicamentoService;
    private final IProveedorService proveedorService;

    public MedicamentoController(IMedicamentoService medicamentoService,
                                 IProveedorService proveedorService) {
        this.medicamentoService = medicamentoService;
        this.proveedorService = proveedorService;
    }

    @GetMapping
    public String listar(@RequestParam(value = "buscar", required = false) String buscar,
                         @RequestParam(value = "stockMinimo", required = false) Integer stockMinimo,
                         @RequestParam(value = "pagina", defaultValue = "0") int numeroPagina,
                         Model model,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {

        if (!RolHelper.tieneRol(session, "FARMACEUTICO", "CAJERO")) {
            return RolHelper.denegar(redirectAttributes, "No tiene permisos para consultar medicamentos.");
        }

        List<MedicamentoEntity> todos = stockMinimo != null
                ? medicamentoService.listarPorStockMenorA(stockMinimo)
                : medicamentoService.buscarMedicamentos(buscar);
        Pagina<MedicamentoEntity> pagina = Pagina.de(todos, numeroPagina);
        model.addAttribute("medicamentos", pagina.contenido());
        model.addAttribute("pagina", pagina);

        model.addAttribute("buscar", buscar);
        model.addAttribute("stockMinimo", stockMinimo);

        return "medicamentos/listar";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {

        if (!RolHelper.tieneRol(session, "FARMACEUTICO")) {
            return RolHelper.denegar(redirectAttributes, "Solo el farmacéutico puede registrar medicamentos.");
        }

        model.addAttribute("medicamento", new MedicamentoEntity());
        model.addAttribute("proveedores", proveedorService.listarTodos());

        return "medicamentos/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("medicamento") MedicamentoEntity medicamento,
                          @RequestParam("rucProveedor") String rucProveedor,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {

        if (!RolHelper.tieneRol(session, "FARMACEUTICO")) {
            return RolHelper.denegar(redirectAttributes, "No tiene permisos para guardar medicamentos.");
        }

        if (medicamento.getCodMedicamento() == null) {
            medicamentoService.registrarMedicamento(rucProveedor, medicamento);
        } else {
            medicamento.setProveedor(proveedorService.buscarPorId(rucProveedor)
                    .orElseThrow(() -> new IllegalArgumentException("No existe el proveedor: " + rucProveedor)));

            medicamentoService.guardar(medicamento);
        }

        redirectAttributes.addFlashAttribute("mensaje", "Medicamento guardado correctamente.");

        return "redirect:/medicamentos";
    }

    @PostMapping("/stock/{codMedicamento}")
    public String actualizarStock(@PathVariable Long codMedicamento,
                                  @RequestParam("nuevaCantidad") int nuevaCantidad,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {

        if (!RolHelper.tieneRol(session, "FARMACEUTICO")) {
            return RolHelper.denegar(redirectAttributes, "No tiene permisos para actualizar stock.");
        }

        medicamentoService.actualizarStock(codMedicamento, nuevaCantidad);
        redirectAttributes.addFlashAttribute("mensaje", "Stock actualizado correctamente.");

        return "redirect:/medicamentos";
    }

    @GetMapping("/editar/{codMedicamento}")
    public String editar(@PathVariable Long codMedicamento,
                         Model model,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {

        if (!RolHelper.tieneRol(session, "FARMACEUTICO")) {
            return RolHelper.denegar(redirectAttributes, "No tiene permisos para editar medicamentos.");
        }

        MedicamentoEntity medicamento = medicamentoService.buscarPorId(codMedicamento)
                .orElseThrow(() -> new IllegalArgumentException("No existe el medicamento: " + codMedicamento));

        model.addAttribute("medicamento", medicamento);
        model.addAttribute("proveedores", proveedorService.listarTodos());

        return "medicamentos/formulario";
    }

    @PostMapping("/eliminar/{codMedicamento}")
    public String eliminar(@PathVariable Long codMedicamento,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {

        if (!RolHelper.tieneRol(session, "FARMACEUTICO")) {
            return RolHelper.denegar(redirectAttributes, "No tiene permisos para eliminar medicamentos.");
        }

        medicamentoService.eliminar(codMedicamento);
        redirectAttributes.addFlashAttribute("mensaje", "Medicamento eliminado correctamente.");

        return "redirect:/medicamentos";
    }
}
