/*
 * Este arquivo pertence à infraestrutura da aplicação.
 */
package senac.tsi.games.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import senac.tsi.games.entities.*;
import senac.tsi.games.repositories.*;

import java.util.List;

@Configuration
public class LoadDatabase {

    private static final Logger log = LoggerFactory.getLogger(LoadDatabase.class);

    @Bean
    CommandLineRunner initDatabase(GameRepository gameRepository,
                                   CategoryRepository categoryRepository,
                                   PlatformRepository platformRepository,
                                   UserRepository userRepository,
                                   ReviewRepository reviewRepository,
                                   GameDetailRepository gameDetailRepository,
                                   ApiKeyRepository apiKeyRepository) {
        return args -> {

            Category rpg = categoryRepository.save(new Category(CategoryType.RPG));
            Category action = categoryRepository.save(new Category(CategoryType.ACTION));

            Platform pc = platformRepository.save(new Platform("PC"));
            Platform ps5 = platformRepository.save(new Platform("PlayStation 5"));

            User wanessa = userRepository.save(new User("Wanessa", "wanessa@email.com"));
            User ana = userRepository.save(new User("Ana", "ana@email.com"));
            ApiKey demoKey = apiKeyRepository.save(new ApiKey("Chave de teste", "games-demo-key", ApiKeyRole.ADMIN, wanessa));

            Game game1 = new Game("GTA V", 199.90);
            game1.setCategory(action);
            game1.setPlatforms(List.of(pc, ps5));
            game1 = gameRepository.save(game1);

            Game game2 = new Game("The Witcher 3", 99.90);
            game2.setCategory(rpg);
            game2.setPlatforms(List.of(pc));
            game2 = gameRepository.save(game2);

            // GameDetail precisa ser salvo pelo repository para persistir no banco
            GameDetail detail1 = new GameDetail("Jogo de mundo aberto com missões épicas", "Rockstar Games");
            detail1.setGame(game1);
            gameDetailRepository.save(detail1);

            GameDetail detail2 = new GameDetail("RPG de fantasia com mundo enorme", "CD Projekt Red");
            detail2.setGame(game2);
            gameDetailRepository.save(detail2);

            Review review1 = new Review("Muito bom", 9);
            review1.setGame(game1);
            review1.setUser(wanessa);
            reviewRepository.save(review1);

            Review review2 = new Review("Excelente", 10);
            review2.setGame(game2);
            review2.setUser(ana);
            reviewRepository.save(review2);

            log.info("Preloading " + rpg);
            log.info("Preloading " + action);
            log.info("Preloading " + pc);
            log.info("Preloading " + ps5);
            log.info("Preloading " + wanessa);
            log.info("Preloading " + ana);
            log.info("Preloading demo API key " + demoKey.getKeyValue());
            log.info("Preloading " + game1);
            log.info("Preloading " + game2);
            log.info("Preloading " + detail1);
            log.info("Preloading " + detail2);
            log.info("Preloading " + review1);
            log.info("Preloading " + review2);
        };
    }
}
