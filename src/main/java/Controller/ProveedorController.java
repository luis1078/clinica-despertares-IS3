package Controller;

import Entity.ProveedorEntity;
import Service.IProveedorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/proveedores")
public class ProveedorController {

    private final IProveedorService proveedorService;

    public ProveedorController(IProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    @GetMapping
    public String listar(@RequestParam(value = "nombre", required = false) String nombre, Model model) {
        model.addAttribute("proveedores", nombre == null || nombre.isBlank()
                ? proveedorService.listarTodos()
                : proveedorService.buscarPorNombre(nombre));
        model.addAttribute("nombre", nombre);
        return "proveedores/listar";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("proveedor", new ProveedorEntity());
        return "proveedores/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("proveedor") ProveedorEntity proveedor, RedirectAttributes redirectAttributes) {
        proveedorService.guardar(proveedor);
        redirectAttributes.addFlashAttribute("mensaje", "Proveedor guardado correctamente.");
        return "redirect:/proveedores";
    }

    @GetMapping("/editar/{rucProveedor}")
    public String editar(@PathVariable String rucProveedor, Model model) {
        ProveedorEntity proveedor = proveedorService.buscarPorId(rucProveedor)
                .orElseThrow(() -> new IllegalArgumentException("No existe el proveedor: " + rucProveedor));
        model.addAttribute("proveedor", proveedor);
        return "proveedores/formulario";
    }

    @GetMapping("/eliminar/{rucProveedor}")
    public String eliminar(@PathVariable String rucProveedor, RedirectAttributes redirectAttributes) {
        proveedorService.eliminar(rucProveedor);
        redirectAttributes.addFlashAttribute("mensaje", "Proveedor eliminado correctamente.");
        return "redirect:/proveedores";
    }
}
