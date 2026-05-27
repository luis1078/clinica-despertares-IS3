package Controller;

import Entity.MedicamentoEntity;
import Service.IMedicamentoService;
import Service.IProveedorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/medicamentos")
public class MedicamentoController {

    private final IMedicamentoService medicamentoService;
    private final IProveedorService proveedorService;

    public MedicamentoController(IMedicamentoService medicamentoService, IProveedorService proveedorService) {
        this.medicamentoService = medicamentoService;
        this.proveedorService = proveedorService;
    }

    @GetMapping
    public String listar(@RequestParam(value = "nombre", required = false) String nombre,
                         @RequestParam(value = "stockMinimo", required = false) Integer stockMinimo,
                         Model model) {
        if (stockMinimo != null) {
            model.addAttribute("medicamentos", medicamentoService.listarPorStockMenorA(stockMinimo));
        } else if (nombre != null && !nombre.isBlank()) {
            model.addAttribute("medicamentos", medicamentoService.buscarPorNombre(nombre));
        } else {
            model.addAttribute("medicamentos", medicamentoService.listarTodos());
        }
        model.addAttribute("nombre", nombre);
        model.addAttribute("stockMinimo", stockMinimo);
        return "medicamentos/listar";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("medicamento", new MedicamentoEntity());
        model.addAttribute("proveedores", proveedorService.listarTodos());
        return "medicamentos/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("medicamento") MedicamentoEntity medicamento,
                          @RequestParam("rucProveedor") String rucProveedor,
                          RedirectAttributes redirectAttributes) {
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
                                  RedirectAttributes redirectAttributes) {
        medicamentoService.actualizarStock(codMedicamento, nuevaCantidad);
        redirectAttributes.addFlashAttribute("mensaje", "Stock actualizado correctamente.");
        return "redirect:/medicamentos";
    }

    @GetMapping("/editar/{codMedicamento}")
    public String editar(@PathVariable Long codMedicamento, Model model) {
        MedicamentoEntity medicamento = medicamentoService.buscarPorId(codMedicamento)
                .orElseThrow(() -> new IllegalArgumentException("No existe el medicamento: " + codMedicamento));
        model.addAttribute("medicamento", medicamento);
        model.addAttribute("proveedores", proveedorService.listarTodos());
        return "medicamentos/formulario";
    }

    @GetMapping("/eliminar/{codMedicamento}")
    public String eliminar(@PathVariable Long codMedicamento, RedirectAttributes redirectAttributes) {
        medicamentoService.eliminar(codMedicamento);
        redirectAttributes.addFlashAttribute("mensaje", "Medicamento eliminado correctamente.");
        return "redirect:/medicamentos";
    }
}
