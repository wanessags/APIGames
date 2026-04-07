/*
 * Este arquivo pertence à infraestrutura da aplicação.
 * Essa camada concentra configurações ou inicializações que dão suporte ao projeto, como Swagger/OpenAPI
 * e a carga inicial de dados no banco H2.
 */
package senac.tsi.games.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// CommandLineRunner é executado quando a aplicação sobe, servindo para popular o banco com dados iniciais de teste.
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import senac.tsi.games.entities.*;
import senac.tsi.games.repositories.*;

import java.util.List;

// @Configuration indica que esta classe fornece configurações ou beans que o Spring deve registrar no contexto.
@Configuration
// A classe LoadDatabase configura um aspecto estrutural da aplicação.
public class LoadDatabase {

    private static final Logger log = LoggerFactory.getLogger(LoadDatabase.class);

    // @Bean faz com que o objeto criado por este método seja gerenciado pelo Spring e reutilizado na aplicação.
    @Bean
// CommandLineRunner é executado quando a aplicação sobe, servindo para popular o banco com dados iniciais de teste.
    CommandLineRunner initDatabase(GameRepository gameRepository,
                                   CategoryRepository categoryRepository,
                                   PlatformRepository platformRepository,
                                   UserRepository userRepository,
                                   ReviewRepository reviewRepository) {
        return args -> {

            Category rpg = categoryRepository.save(new Category(CategoryType.RPG));
            Category action = categoryRepository.save(new Category(CategoryType.ACTION));

            Platform pc = platformRepository.save(new Platform("PC"));
            Platform ps5 = platformRepository.save(new Platform("PlayStation 5"));

            User wanessa = userRepository.save(new User("Wanessa", "wanessa@email.com"));
            User ana = userRepository.save(new User("Ana", "ana@email.com"));

            Game game1 = new Game("GTA V", 199.90);
            game1.setCategory(action);
            game1.setPlatforms(List.of(pc, ps5));
            game1 = gameRepository.save(game1);

            Game game2 = new Game("The Witcher 3", 99.90);
            game2.setCategory(rpg);
            game2.setPlatforms(List.of(pc));
            game2 = gameRepository.save(game2);

            Review review1 = new Review("Muito bom", 9);
            review1.setGame(game1);
            review1.setUser(wanessa);
            reviewRepository.save(review1);

            Review review2 = new Review("Excelente", 10);
            review2.setGame(game2);
            review2.setUser(ana);
            reviewRepository.save(review2);

// O log mostra no console o que foi inserido, o que ajuda a conferir rapidamente se a carga inicial aconteceu.
            log.info("Preloading " + rpg);
            log.info("Preloading " + action);
            log.info("Preloading " + pc);
            log.info("Preloading " + ps5);
            log.info("Preloading " + wanessa);
            log.info("Preloading " + ana);
            log.info("Preloading " + game1);
            log.info("Preloading " + game2);
            log.info("Preloading " + review1);
            log.info("Preloading " + review2);
        };
    }
}