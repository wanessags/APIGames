/*
 * Esta é a classe principal da aplicação Spring Boot.
 * É a partir dela que o Spring sobe o contexto, registra os beans e inicializa a API.
 */
package senac.tsi.games;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// @SpringBootApplication reúne configuração, escaneamento de componentes e inicialização do Spring Boot em uma única anotação.
@SpringBootApplication
// A classe GamesApplication contém o método main, que é o ponto de entrada da aplicação.
public class GamesApplication {

    // O método main é o ponto de partida da aplicação e chama o Spring para subir toda a estrutura da API.
    public static void main(String[] args) {
        SpringApplication.run(GamesApplication.class, args);
    }
}