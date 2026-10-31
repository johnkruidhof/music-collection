package nl.johnkruidhof.musiccollection;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** Controleert dat de context start en de Flyway-migraties overeenkomen met de entities (ddl-auto: validate). */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class MusicCollectionApplicationTests {

    @Test
    void contextLoads() {
    }
}
