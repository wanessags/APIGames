package senac.tsi.games.exceptions;

public class ApiKeyNotFoundException extends RuntimeException {

    public ApiKeyNotFoundException(Long id) {
        super("Could not find api key " + id);
    }
}
