package Controller;

import Entity.CitaMedicaEntity;
import Entity.Emuns.EstadoCitaEnum;
import Service.ICitaMedicaService;
import Service.IMedicoService;
import Service.IPacienteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;

@Controller
@RequestMapping("/citas")
public class CitaMedicaController {

    private final ICitaMedicaService citaMedicaService;
    private final IPacienteService pacienteService;
    private final IMedicoService medicoService;

    public CitaMedicaController(ICitaMedicaService citaMedicaService, IPacienteService pacienteService, IMedicoService medicoService) {
        this.citaMedicaService = citaMedicaService;
        this.pacienteService = pacienteService;
        this.medicoService = medicoService;
    }

    @GetMapping
    public String listar(@RequestParam(value = "estado", required = false) EstadoCitaEnum estado,
                         @RequestParam(value = "buscar", required = false) String buscar,
                         Model model) {
        model.addAttribute("citas", citaMedicaService.buscarCitas(buscar, estado));
        model.addAttribute("estados", EstadoCitaEnum.values());
        model.addAttribute("estadoSeleccionado", estado);
        model.addAttribute("buscar", buscar);
        return "citas/listar";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("cita", new CitaMedicaEntity());
        cargarCombos(model);
        return "citas/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("cita") CitaMedicaEntity cita,
                          @RequestParam("dniPaciente") String dniPaciente,
                          @RequestParam("idMedico") Long idMedico,
                          RedirectAttributes redirectAttributes,
                          Model model) {
        try {
            if (cita.getCodCitaMedica() == null) {
                citaMedicaService.registrarCitaMedica(dniPaciente, idMedico, cita);
                redirectAttributes.addFlashAttribute("mensaje", "Cita médica registrada correctamente. Se generó el comprobante de pago automáticamente.");
            } else {
                CitaMedicaEntity citaExistente = citaMedicaService.buscarPorId(cita.getCodCitaMedica())
                        .orElseThrow(() -> new IllegalArgumentException("No existe la cita médica: " + cita.getCodCitaMedica()));
                citaExistente.setFechaCita(cita.getFechaCita());
                citaExistente.setHoraCita(cita.getHoraCita());
                citaExistente.setMotivoConsulta(cita.getMotivoConsulta());
                citaExistente.setEstadoCita(cita.getEstadoCita());
                citaExistente.setPaciente(pacienteService.buscarPorId(dniPaciente)
                        .orElseThrow(() -> new IllegalArgumentException("No existe el paciente: " + dniPaciente)));
                citaExistente.setMedico(medicoService.buscarPorId(idMedico)
                        .orElseThrow(() -> new IllegalArgumentException("No existe el médico: " + idMedico)));
                citaMedicaService.guardar(citaExistente);
                redirectAttributes.addFlashAttribute("mensaje", "Cita médica actualizada correctamente.");
            }
            return "redirect:/citas";
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("cita", cita);
            cargarCombos(model);
            return "citas/formulario";
        }
    }

    @GetMapping("/editar/{codCitaMedica}")
    public String editar(@PathVariable Long codCitaMedica, Model model) {
        CitaMedicaEntity cita = citaMedicaService.buscarPorId(codCitaMedica)
                .orElseThrow(() -> new IllegalArgumentException("No existe la cita médica: " + codCitaMedica));
        model.addAttribute("cita", cita);
        cargarCombos(model);
        return "citas/formulario";
    }

    @PostMapping("/cancelar/{codCitaMedica}")
    public String cancelar(@PathVariable Long codCitaMedica, RedirectAttributes redirectAttributes) {
        try {
            citaMedicaService.cancelarCitaMedica(codCitaMedica);
            redirectAttributes.addFlashAttribute("mensaje", "Cita médica cancelada correctamente.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/citas";
    }

    @PostMapping("/finalizar/{codCitaMedica}")
    public String finalizar(@PathVariable Long codCitaMedica, RedirectAttributes redirectAttributes) {
        try {
            citaMedicaService.finalizarCitaMedica(codCitaMedica);
            redirectAttributes.addFlashAttribute("mensaje", "Cita médica finalizada correctamente.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/citas";
    }

    @PostMapping("/reprogramar/{codCitaMedica}")
    public String reprogramar(@PathVariable Long codCitaMedica,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate nuevaFecha,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime nuevaHora,
                              RedirectAttributes redirectAttributes) {
        citaMedicaService.reprogramarCitaMedica(codCitaMedica, nuevaFecha, nuevaHora);
        redirectAttributes.addFlashAttribute("mensaje", "Cita médica reprogramada correctamente.");
        return "redirect:/citas";
    }

    @PostMapping("/eliminar/{codCitaMedica}")
    public String eliminar(@PathVariable Long codCitaMedica, RedirectAttributes redirectAttributes) {
        citaMedicaService.eliminar(codCitaMedica);
        redirectAttributes.addFlashAttribute("mensaje", "Cita médica eliminada correctamente.");
        return "redirect:/citas";
    }

    private void cargarCombos(Model model) {
        model.addAttribute("pacientes", pacienteService.listarTodos());
        model.addAttribute("medicos", medicoService.listarTodos());
        model.addAttribute("estados", EstadoCitaEnum.values());
    }
}
