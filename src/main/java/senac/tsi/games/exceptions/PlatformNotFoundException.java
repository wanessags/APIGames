/*
 * Este arquivo faz parte da camada de tratamento de erros.
 * O projeto segue o padrão do professor com uma exceção específica por entidade e uma classe Advice
 * para transformar esses erros em respostas HTTP corretas, como 404 e 400.
 */
package senac.tsi.games.exceptions;

// A classe PlatformNotFoundException existe para representar ou tratar um erro específico de forma controlada.
public class PlatformNotFoundException extends RuntimeException {

    // O construtor recebe o id procurado para montar uma mensagem clara, útil tanto para teste quanto para apresentação.
    public PlatformNotFoundException(Long id) {
        super("Could not find platform " + id);
    }
}