package auca.ac.rw.gradclearspringboot;

import auca.ac.rw.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class GradClearSpringBootApplicationTests {

	@Test
	void contextLoads() {
	}

}
