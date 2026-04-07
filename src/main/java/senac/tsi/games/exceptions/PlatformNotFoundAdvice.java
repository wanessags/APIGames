/*
 * Este arquivo faz parte da camada de tratamento de erros.
 * O projeto segue o padrão do professor com uma exceção específica por entidade e uma classe Advice
 * para transformar esses erros em respostas HTTP corretas, como 404 e 400.
 */
package senac.tsi.games.exceptions;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// @RestControllerAdvice centraliza o tratamento de exceções para que os controllers não precisem repetir essa lógica.
@RestControllerAdvice
// A classe PlatformNotFoundAdvice existe para representar ou tratar um erro específico de forma controlada.
public class PlatformNotFoundAdvice {

    // @ExceptionHandler diz qual tipo de erro este método sabe capturar e transformar em resposta HTTP.
    @ExceptionHandler(ConversionFailedException.class)
// Aqui o erro é convertido em HTTP 400, normalmente usado quando a requisição veio com formato ou valor inválido.
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String handleConversion(PlatformNotFoundException ex) {
        return ex.getMessage();
    }

    // @ExceptionHandler diz qual tipo de erro este método sabe capturar e transformar em resposta HTTP.
    @ExceptionHandler(PlatformNotFoundException.class)
// Aqui o erro é convertido em HTTP 404, que é o status adequado quando o recurso solicitado não existe.
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String platformNotFoundHandler(PlatformNotFoundException ex) {
        return ex.getMessage();
    }
}