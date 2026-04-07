# 🎮 API Games - Spring Boot RESTful

<p align="center">
  <a href="https://apigames-kpkn.onrender.com/" target="_blank">
    <img src="https://img.shields.io/badge/Java_17+-ED8B00?style=for-the-badge&logo=java&logoColor=white"/>
  </a>
  <a href="https://apigames-kpkn.onrender.com/" target="_blank">
    <img src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white"/>
  </a>
  <a href="https://apigames-kpkn.onrender.com/" target="_blank">
    <img src="https://img.shields.io/badge/Status-Online-10B981?style=for-the-badge&logo=uptime-robot&logoColor=white"/>
  </a>
  <a href="https://apigames-kpkn.onrender.com/" target="_blank">
    <img src="https://img.shields.io/badge/Render-46E3B7?style=for-the-badge&logo=render&logoColor=white"/>
  </a>
</p>

## Descrição

Esta aplicação consiste em uma **API RESTful** desenvolvida com **Spring Boot**, com o objetivo de gerenciar jogos, categorias, plataformas, usuários e avaliações.

O projeto foi desenvolvido seguindo boas práticas de desenvolvimento e atende aos requisitos acadêmicos, incluindo:

✔ CRUD completo  
✔ Relacionamentos entre entidades  
✔ Paginação  
✔ HATEOAS  
✔ Documentação com Swagger  
✔ Deploy em produção  

## Tecnologias Utilizadas

- Java 17+
- Spring Boot
- Maven
- Spring Data JPA
- H2 Database (em memória)
- Springdoc OpenAPI (Swagger)
- Spring HATEOAS
- Docker
- Render (Deploy)

## API Online

Acesse a API: https://apigames-kpkn.onrender.com

## Documentação (Swagger):
https://apigames-kpkn.onrender.com/swagger-ui/index.html

## Estrutura do Projeto
controllers      → endpoints da API
entities         → entidades (tabelas)
repositories     → acesso ao banco (JPA)
exceptions       → tratamento de erros
infrastructure   → configurações

## Entidades

- Game
- Category
- Platform
- User
- Review

## Relacionamentos

- One-to-Many → Category → Game  
- Many-to-Many → Game ↔ Platform  
- One-to-One → User ↔ Review  

## Enum

A entidade Category utiliza enum:

- RPG  
- ACTION  
- ADVENTURE
- 
## Funcionalidades

- CRUD completo
- Paginação com Pageable
- Busca personalizada
- Validações (Bean Validation)
- Tratamento de erros
- HATEOAS
- Swagger

## Endpoints

### Games
* ✔ **GET** `/games`
* ✔ **GET** `/games/{id}`
* ✔ **POST** `/games`
* ✔ **PUT** `/games/{id}`
* ✔ **DELETE** `/games/{id}`
* ✔ **GET** `/games/search?name=...`

### Categories
* ✔ **GET** `/categories`
* ✔ **GET** `/categories/{id}`
* ✔ **POST** `/categories`
* ✔ **PUT** `/categories/{id}`
* ✔ **DELETE** `/categories/{id}`
* ✔ **GET** `/categories/search?type=...`

### Platforms
* ✔ **GET** `/platforms`
* ✔ **GET** `/platforms/{id}`
* ✔ **POST** `/platforms`
* ✔ **PUT** `/platforms/{id}`
* ✔ **DELETE** `/platforms/{id}`
* ✔ **GET** `/platforms/search?name=...`
  
### Users
* ✔ **GET** `/users`
* ✔ **GET** `/users/{id}`
* ✔ **POST** `/users`
* ✔ **PUT** `/users/{id}`
* ✔ **DELETE** `/users/{id}`
* ✔ **GET** `/users/search?email=...`
  
### Reviews
* ✔ **GET** `/reviews`
* ✔ **GET** `/reviews/{id}`
* ✔ **POST** `/reviews`
* ✔ **PUT** `/reviews/{id}`
* ✔ **DELETE** `/reviews/{id}`
* ✔ **GET** `/reviews/search?score=...`
✔ GET    /reviews/search?score=…
