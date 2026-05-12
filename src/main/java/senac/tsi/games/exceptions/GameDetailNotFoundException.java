package senac.tsi.games.exceptions;

public class GameDetailNotFoundException extends RuntimeException {

    public GameDetailNotFoundException(Long id) {
        super("Could not find game detail " + id);
    }
}