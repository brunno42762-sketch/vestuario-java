package br.edu.eseg.vestuario.ezVest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** A aplicação inteira sobe (beans, segurança, JPA, migração) com o banco de teste em memória. */
@SpringBootTest
@ActiveProfiles("test")
class EzVestApplicationTests {

	@Test
	void contextLoads() {
	}

}
