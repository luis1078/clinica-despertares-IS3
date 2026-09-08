package Despertares.clinica_Despertares;

import App.ClinicaDespertaresApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Se indica explícitamente la clase de arranque: esta clase de test vive en
// Despertares.clinica_Despertares, un paquete distinto al de App (donde está
// @SpringBootApplication), así que Spring Boot no puede encontrarla buscando
// hacia arriba por convención y hay que decírselo.
@SpringBootTest(classes = ClinicaDespertaresApplication.class)
class ClinicaDespertaresApplicationTests {

	@Test
	void contextLoads() {
	}

}
