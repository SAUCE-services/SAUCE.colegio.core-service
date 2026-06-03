package ar.com.sauce.colegio.rest;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@SecurityScheme(
		name = "X-API-KEY",
		type = SecuritySchemeType.APIKEY,
		in = SecuritySchemeIn.HEADER,
		paramName = "X-API-KEY"
)
public class ColegioApplication {

	public static void main(String[] args) {
		SpringApplication.run(ColegioApplication.class, args);
	}

}
