package ar.com.sauce.colegio.rest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requiere MySQL real; no disponible en CI")
public class ApplicationTests {
	@Test
	public void contextLoads() {}
}
