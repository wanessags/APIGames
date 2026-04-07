/*
 * Este arquivo representa uma entidade da aplicação.
 * Em JPA, uma entidade vira uma tabela no banco de dados.
 * Por isso ela recebe anotações como @Entity, @Id, validações e relacionamentos.
 * Como você pediu, os comentários abaixo só explicam o código; a lógica original foi mantida.
 */
package senac.tsi.games.entities;

// O enum CategoryType restringe os valores possíveis, evitando textos livres e ajudando na consistência dos dados.
public enum CategoryType {
    RPG,
    ACTION,
    ADVENTURE,
    SPORTS,
    STRATEGY
}