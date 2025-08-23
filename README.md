# Prompt Generator SaaS

A Spring Boot application for managing and sharing AI prompts. This SaaS platform allows users to create, store, search, and share prompts with features like categorization, tagging, and usage tracking.

## Features

- **Prompt Management**: Create, read, update, and delete prompts
- **User-specific prompts**: Each user can manage their own prompt collection
- **Public sharing**: Share prompts publicly for community use
- **Search functionality**: Search through public and private prompts
- **Categorization**: Organize prompts by categories
- **Tagging**: Add tags to prompts for better organization
- **Usage tracking**: Track how often prompts are used
- **RESTful API**: Complete REST API for frontend integration

## Project Structure

```
src/main/java/com/alejandro/microservices/promptgeneratorsaas/
│
├── config/           # Configuration classes (Security, CORS, etc.)
├── controller/       # REST controllers
├── dto/             # Data Transfer Objects
├── entity/          # JPA entities
├── repository/      # JPA repositories
├── service/         # Business logic services
└── PromptGeneratorSaasApplication.java  # Main application class
```

## Technology Stack

- **Spring Boot 3.x**: Main framework
- **Spring Data JPA**: Database access
- **Spring Security**: Security configuration
- **H2 Database**: In-memory database (for development)
- **Maven**: Build tool
- **Jakarta Validation**: Input validation

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

### Installation

1. Build the project:
```bash
mvn clean install
```

2. Run the application:
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### Database Access

For development, the application uses H2 in-memory database. You can access the H2 console at:
`http://localhost:8080/h2-console`

- JDBC URL: `jdbc:h2:mem:promptdb`
- Username: `sa`
- Password: `password`

## API Documentation

### Base URL
`http://localhost:8080/api/prompts`

### Authentication
Most endpoints require a `X-User-ID` header for user identification.

### Endpoints

#### Create Prompt
```http
POST /api/prompts
Content-Type: application/json
X-User-ID: user123

{
  "title": "Creative Writing Assistant",
  "content": "You are a creative writing assistant...",
  "description": "Helps with creative writing tasks",
  "category": "Writing",
  "tags": "creative,writing,assistant",
  "isPublic": true
}
```

#### Get Prompt by ID
```http
GET /api/prompts/{id}
```

#### Get User's Prompts
```http
GET /api/prompts/user/{userId}
```

#### Get Public Prompts
```http
GET /api/prompts/public
```

#### Search Public Prompts
```http
GET /api/prompts/public/search?q=writing
```

#### Update Prompt
```http
PUT /api/prompts/{id}
Content-Type: application/json
X-User-ID: user123

{
  "title": "Updated Title",
  "content": "Updated content...",
  "description": "Updated description",
  "category": "Updated Category",
  "tags": "updated,tags",
  "isPublic": false
}
```

#### Delete Prompt
```http
DELETE /api/prompts/{id}
X-User-ID: user123
```

#### Increment Usage Count
```http
POST /api/prompts/{id}/use
```

## Data Models

### Prompt Entity
- `id`: Unique identifier
- `title`: Prompt title
- `content`: Prompt content
- `description`: Optional description
- `category`: Prompt category
- `tags`: Comma-separated tags
- `userId`: Owner user ID
- `createdAt`: Creation timestamp
- `updatedAt`: Last update timestamp
- `isPublic`: Public visibility flag
- `usageCount`: Usage counter
