package App;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
		"Controller",
		"Service",
		"Service.ServiceImpl",
		"Repository",
		"Entity",
		"Config"
})
@EntityScan(basePackages = "Entity")
@EnableJpaRepositories(basePackages = "Repository")
public class ClinicaDespertaresApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClinicaDespertaresApplication.class, args);
	}
}