window.BLOOM_ARCHITECTURE = {
  "commit": "e7a7ccf",
  "nodes": [
    {
      "id": "api-gateway",
      "title": "API Gateway",
      "type": "java",
      "state": "implemented",
      "port": 8080,
      "description": "Единая точка входа. Направляет HTTP и WebSocket-запросы, объединяет Swagger сервисов. HTTP-вход приложения опубликован на порту 8080.",
      "published": [
        "8080:8080"
      ],
      "inCompose": true,
      "profiles": [],
      "endpoints": [
        {
          "method": "GET",
          "path": "/docs/services",
          "internal": false,
          "source": {
            "file": "server/api-gateway/src/main/java/app/bloom/gateway/docs/ApiDocsController.java",
            "line": 28,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "GET",
          "path": "/docs/openapi/{service}",
          "internal": false,
          "source": {
            "file": "server/api-gateway/src/main/java/app/bloom/gateway/docs/ApiDocsController.java",
            "line": 33,
            "found": true
          },
          "origin": "Spring mapping"
        }
      ],
      "layers": {
        "application": [
          {
            "name": "ApiDocsConfiguration.java",
            "source": {
              "file": "server/api-gateway/src/main/java/app/bloom/gateway/docs/ApiDocsConfiguration.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "ApiDocsController.java",
            "source": {
              "file": "server/api-gateway/src/main/java/app/bloom/gateway/docs/ApiDocsController.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "ApiDocService.java",
            "source": {
              "file": "server/api-gateway/src/main/java/app/bloom/gateway/docs/ApiDocService.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "ApiDocsProperties.java",
            "source": {
              "file": "server/api-gateway/src/main/java/app/bloom/gateway/docs/ApiDocsProperties.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "OpenApiCatalog.java",
            "source": {
              "file": "server/api-gateway/src/main/java/app/bloom/gateway/docs/OpenApiCatalog.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "GatewayApplication.java",
            "source": {
              "file": "server/api-gateway/src/main/java/app/bloom/gateway/GatewayApplication.java",
              "line": 1,
              "found": true
            }
          }
        ]
      },
      "files": 6,
      "source": {
        "file": "compose.yml",
        "line": 184,
        "found": true
      }
    },
    {
      "id": "identity",
      "title": "Identity",
      "type": "java",
      "state": "implemented",
      "port": 8081,
      "description": "Аккаунты, вход по телефону и паролю, access/refresh-токены, сессии и блокировка аккаунта.",
      "published": [
        "127.0.0.1:8081:8081"
      ],
      "inCompose": true,
      "profiles": [],
      "endpoints": [
        {
          "method": "DELETE",
          "path": "/internal/identity/accounts/{id}",
          "internal": true,
          "source": {
            "file": "server/identity/src/main/java/app/bloom/identity/controller/AccountDeletionController.java",
            "line": 19,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "PUT",
          "path": "/internal/identity/accounts/{id}/status",
          "internal": true,
          "source": {
            "file": "server/identity/src/main/java/app/bloom/identity/controller/AccountStatusController.java",
            "line": 30,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "POST",
          "path": "/auth/register",
          "internal": false,
          "source": {
            "file": "server/identity/src/main/java/app/bloom/identity/controller/AuthController.java",
            "line": 36,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "POST",
          "path": "/auth/login",
          "internal": false,
          "source": {
            "file": "server/identity/src/main/java/app/bloom/identity/controller/AuthController.java",
            "line": 42,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "POST",
          "path": "/auth/refresh",
          "internal": false,
          "source": {
            "file": "server/identity/src/main/java/app/bloom/identity/controller/AuthController.java",
            "line": 47,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "POST",
          "path": "/auth/logout",
          "internal": false,
          "source": {
            "file": "server/identity/src/main/java/app/bloom/identity/controller/AuthController.java",
            "line": 53,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "POST",
          "path": "/auth/logout-all",
          "internal": false,
          "source": {
            "file": "server/identity/src/main/java/app/bloom/identity/controller/AuthController.java",
            "line": 60,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "GET",
          "path": "/auth/me",
          "internal": false,
          "source": {
            "file": "server/identity/src/main/java/app/bloom/identity/controller/AuthController.java",
            "line": 66,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "GET",
          "path": "/auth/sessions",
          "internal": false,
          "source": {
            "file": "server/identity/src/main/java/app/bloom/identity/controller/AuthController.java",
            "line": 71,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "DELETE",
          "path": "/auth/sessions/{familyId}",
          "internal": false,
          "source": {
            "file": "server/identity/src/main/java/app/bloom/identity/controller/AuthController.java",
            "line": 77,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "POST",
          "path": "/internal/identity/introspect",
          "internal": true,
          "source": {
            "file": "server/identity/src/main/java/app/bloom/identity/controller/InternalIdentityController.java",
            "line": 47,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "POST",
          "path": "/internal/identity/active-accounts",
          "internal": true,
          "source": {
            "file": "server/identity/src/main/java/app/bloom/identity/controller/InternalIdentityController.java",
            "line": 57,
            "found": true
          },
          "origin": "Spring mapping"
        }
      ],
      "layers": {
        "config": [
          {
            "name": "OpenApiConfiguration.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/config/OpenApiConfiguration.java",
              "line": 1,
              "found": true
            }
          }
        ],
        "controller": [
          {
            "name": "AccountDeletionController.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/controller/AccountDeletionController.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "AccountStatusController.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/controller/AccountStatusController.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "AuthController.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/controller/AuthController.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "GlobalExceptionHandler.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/controller/GlobalExceptionHandler.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "InternalIdentityController.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/controller/InternalIdentityController.java",
              "line": 1,
              "found": true
            }
          }
        ],
        "dto": [
          {
            "name": "AuthResponse.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/dto/AuthResponse.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "LoginRequest.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/dto/LoginRequest.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "LogoutRequest.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/dto/LogoutRequest.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "RefreshRequest.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/dto/RefreshRequest.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "RegisterRequest.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/dto/RegisterRequest.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "UpdateAccountStatusRequest.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/dto/UpdateAccountStatusRequest.java",
              "line": 1,
              "found": true
            }
          }
        ],
        "events": [
          {
            "name": "OutboxPublisher.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/events/OutboxPublisher.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "OutboxSchedule.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/events/OutboxSchedule.java",
              "line": 1,
              "found": true
            }
          }
        ],
        "application": [
          {
            "name": "AuthenticationException.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/exception/AuthenticationException.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "InvalidTokenException.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/exception/InvalidTokenException.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "RegistrationException.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/exception/RegistrationException.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "IdentityApplication.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/IdentityApplication.java",
              "line": 1,
              "found": true
            }
          }
        ],
        "model": [
          {
            "name": "Account.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/model/Account.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "AccountStatus.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/model/AccountStatus.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "IdentityEvent.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/model/IdentityEvent.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "IdentityEventData.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/model/IdentityEventData.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "PendingEvent.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/model/PendingEvent.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "RefreshSession.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/model/RefreshSession.java",
              "line": 1,
              "found": true
            }
          }
        ],
        "repository": [
          {
            "name": "AccountRepository.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/repository/AccountRepository.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "EventRepository.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/repository/EventRepository.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "RefreshSessionRepository.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/repository/RefreshSessionRepository.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "SecurityAudit.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/repository/SecurityAudit.java",
              "line": 1,
              "found": true
            }
          }
        ],
        "security": [
          {
            "name": "AccessIdentity.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/security/AccessIdentity.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "AccessValidator.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/security/AccessValidator.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "JwtAuthenticationFilter.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/security/JwtAuthenticationFilter.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "JwtProvider.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/security/JwtProvider.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "LoginProtection.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/security/LoginProtection.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "SecurityConfig.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/security/SecurityConfig.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "TokenHasher.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/security/TokenHasher.java",
              "line": 1,
              "found": true
            }
          }
        ],
        "service": [
          {
            "name": "AccountDeletionService.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/service/AccountDeletionService.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "AccountStatusService.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/service/AccountStatusService.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "AuthService.java",
            "source": {
              "file": "server/identity/src/main/java/app/bloom/identity/service/AuthService.java",
              "line": 1,
              "found": true
            }
          }
        ]
      },
      "files": 38,
      "source": {
        "file": "compose.yml",
        "line": 80,
        "found": true
      }
    },
    {
      "id": "db-identity",
      "title": "bloom_identity",
      "type": "database",
      "state": "implemented",
      "description": "Отдельная база сервиса identity в общем PostgreSQL-контейнере. Не отдельный сервер PostgreSQL.",
      "source": {
        "file": "compose.yml",
        "line": 92,
        "found": true
      },
      "endpoints": [],
      "layers": {}
    },
    {
      "id": "users",
      "title": "Users",
      "type": "java",
      "state": "implemented",
      "port": 8082,
      "description": "Анкета, интересы, настройки поиска, приватность, блокировки и удаление пользователя.",
      "published": [],
      "inCompose": true,
      "profiles": [],
      "endpoints": [
        {
          "method": "GET",
          "path": "/internal/reports/{id}",
          "internal": true,
          "source": {
            "file": "server/users/src/main/java/app/bloom/users/controller/InternalReportsController.java",
            "line": 18,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "POST",
          "path": "/internal/users/can-interact",
          "internal": true,
          "source": {
            "file": "server/users/src/main/java/app/bloom/users/controller/InternalUsersController.java",
            "line": 29,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "POST",
          "path": "/internal/users/profiles",
          "internal": true,
          "source": {
            "file": "server/users/src/main/java/app/bloom/users/controller/InternalUsersController.java",
            "line": 34,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "POST",
          "path": "/internal/users/can-view-photo",
          "internal": true,
          "source": {
            "file": "server/users/src/main/java/app/bloom/users/controller/InternalUsersController.java",
            "line": 39,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "GET",
          "path": "/internal/users/{id}/snapshot",
          "internal": true,
          "source": {
            "file": "server/users/src/main/java/app/bloom/users/controller/InternalUsersController.java",
            "line": 44,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "PUT",
          "path": "/users/me",
          "internal": false,
          "source": {
            "file": "server/users/src/main/java/app/bloom/users/controller/ProfileController.java",
            "line": 35,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "GET",
          "path": "/users/me",
          "internal": false,
          "source": {
            "file": "server/users/src/main/java/app/bloom/users/controller/ProfileController.java",
            "line": 41,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "PATCH",
          "path": "/users/me",
          "internal": false,
          "source": {
            "file": "server/users/src/main/java/app/bloom/users/controller/ProfileController.java",
            "line": 46,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "DELETE",
          "path": "/users/me",
          "internal": false,
          "source": {
            "file": "server/users/src/main/java/app/bloom/users/controller/ProfileController.java",
            "line": 51,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "GET",
          "path": "/users/{id}",
          "internal": false,
          "source": {
            "file": "server/users/src/main/java/app/bloom/users/controller/ProfileController.java",
            "line": 57,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "POST",
          "path": "/users/me/heartbeat",
          "internal": false,
          "source": {
            "file": "server/users/src/main/java/app/bloom/users/controller/ProfileController.java",
            "line": 62,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "POST",
          "path": "/users/{id}/block",
          "internal": false,
          "source": {
            "file": "server/users/src/main/java/app/bloom/users/controller/SafetyController.java",
            "line": 28,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "DELETE",
          "path": "/users/{id}/block",
          "internal": false,
          "source": {
            "file": "server/users/src/main/java/app/bloom/users/controller/SafetyController.java",
            "line": 34,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "GET",
          "path": "/users/me/blocks",
          "internal": false,
          "source": {
            "file": "server/users/src/main/java/app/bloom/users/controller/SafetyController.java",
            "line": 40,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "POST",
          "path": "/reports",
          "internal": false,
          "source": {
            "file": "server/users/src/main/java/app/bloom/users/controller/SafetyController.java",
            "line": 47,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "GET",
          "path": "/interests",
          "internal": false,
          "source": {
            "file": "server/users/src/main/java/app/bloom/users/controller/SettingsController.java",
            "line": 36,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "GET",
          "path": "/users/me/preferences",
          "internal": false,
          "source": {
            "file": "server/users/src/main/java/app/bloom/users/controller/SettingsController.java",
            "line": 41,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "PUT",
          "path": "/users/me/preferences",
          "internal": false,
          "source": {
            "file": "server/users/src/main/java/app/bloom/users/controller/SettingsController.java",
            "line": 46,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "GET",
          "path": "/users/me/privacy",
          "internal": false,
          "source": {
            "file": "server/users/src/main/java/app/bloom/users/controller/SettingsController.java",
            "line": 51,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "PUT",
          "path": "/users/me/privacy",
          "internal": false,
          "source": {
            "file": "server/users/src/main/java/app/bloom/users/controller/SettingsController.java",
            "line": 56,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "GET",
          "path": "/users/me/interests",
          "internal": false,
          "source": {
            "file": "server/users/src/main/java/app/bloom/users/controller/SettingsController.java",
            "line": 62,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "PUT",
          "path": "/users/me/interests",
          "internal": false,
          "source": {
            "file": "server/users/src/main/java/app/bloom/users/controller/SettingsController.java",
            "line": 67,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "PUT",
          "path": "/users/me/photos",
          "internal": false,
          "source": {
            "file": "server/users/src/main/java/app/bloom/users/controller/SettingsController.java",
            "line": 72,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "PUT",
          "path": "/users/me/location",
          "internal": false,
          "source": {
            "file": "server/users/src/main/java/app/bloom/users/controller/SettingsController.java",
            "line": 77,
            "found": true
          },
          "origin": "Spring mapping"
        },
        {
          "method": "DELETE",
          "path": "/users/me/location",
          "internal": false,
          "source": {
            "file": "server/users/src/main/java/app/bloom/users/controller/SettingsController.java",
            "line": 83,
            "found": true
          },
          "origin": "Spring mapping"
        }
      ],
      "layers": {
        "client": [
          {
            "name": "IdentityClient.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/client/IdentityClient.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "MediaClient.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/client/MediaClient.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "ServiceHttp.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/client/ServiceHttp.java",
              "line": 1,
              "found": true
            }
          }
        ],
        "config": [
          {
            "name": "OpenApiConfiguration.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/config/OpenApiConfiguration.java",
              "line": 1,
              "found": true
            }
          }
        ],
        "controller": [
          {
            "name": "ApiErrors.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/controller/ApiErrors.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "InternalReportsController.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/controller/InternalReportsController.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "InternalUsersController.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/controller/InternalUsersController.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "ProfileController.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/controller/ProfileController.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "SafetyController.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/controller/SafetyController.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "SettingsController.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/controller/SettingsController.java",
              "line": 1,
              "found": true
            }
          }
        ],
        "dto": [
          {
            "name": "AccessResponse.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/dto/AccessResponse.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "CoordinatesRequest.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/dto/CoordinatesRequest.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "CreateProfileRequest.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/dto/CreateProfileRequest.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "CreateReportRequest.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/dto/CreateReportRequest.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "InteractionAccessRequest.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/dto/InteractionAccessRequest.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "MediaPhotoValidationRequest.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/dto/MediaPhotoValidationRequest.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "MediaPhotoValidationResponse.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/dto/MediaPhotoValidationResponse.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "PhotoAccessRequest.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/dto/PhotoAccessRequest.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "PrivacySettingsRequest.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/dto/PrivacySettingsRequest.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "ProfileBatchRequest.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/dto/ProfileBatchRequest.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "PublicProfile.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/dto/PublicProfile.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "TokenStatusResponse.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/dto/TokenStatusResponse.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "UpdateInterestsRequest.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/dto/UpdateInterestsRequest.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "UpdateLocationRequest.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/dto/UpdateLocationRequest.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "UpdatePhotosRequest.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/dto/UpdatePhotosRequest.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "UpdatePreferencesRequest.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/dto/UpdatePreferencesRequest.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "UpdatePrivacyRequest.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/dto/UpdatePrivacyRequest.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "UpdateProfileRequest.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/dto/UpdateProfileRequest.java",
              "line": 1,
              "found": true
            }
          }
        ],
        "events": [
          {
            "name": "AccountDeletionWorker.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/events/AccountDeletionWorker.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "DeletionSchedule.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/events/DeletionSchedule.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "OutboxPublisher.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/events/OutboxPublisher.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "OutboxSchedule.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/events/OutboxSchedule.java",
              "line": 1,
              "found": true
            }
          }
        ],
        "model": [
          {
            "name": "Gender.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/model/Gender.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "Interest.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/model/Interest.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "Location.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/model/Location.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "PendingEvent.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/model/PendingEvent.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "Preferences.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/model/Preferences.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "Privacy.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/model/Privacy.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "Profile.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/model/Profile.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "ReportReason.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/model/ReportReason.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "SearchMode.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/model/SearchMode.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "UserEvent.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/model/UserEvent.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "UserReport.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/model/UserReport.java",
              "line": 1,
              "found": true
            }
          }
        ],
        "repository": [
          {
            "name": "DeletionRepository.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/repository/DeletionRepository.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "EventRepository.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/repository/EventRepository.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "ProfileRepository.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/repository/ProfileRepository.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "SafetyRepository.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/repository/SafetyRepository.java",
              "line": 1,
              "found": true
            }
          }
        ],
        "security": [
          {
            "name": "AuthenticationFilter.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/security/AuthenticationFilter.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "BoundedJsonRequest.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/security/BoundedJsonRequest.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "CorrelationFilter.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/security/CorrelationFilter.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "SecurityConfig.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/security/SecurityConfig.java",
              "line": 1,
              "found": true
            }
          }
        ],
        "service": [
          {
            "name": "ProfileService.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/service/ProfileService.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "SafetyService.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/service/SafetyService.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "SettingsService.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/service/SettingsService.java",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "VisibilityService.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/service/VisibilityService.java",
              "line": 1,
              "found": true
            }
          }
        ],
        "application": [
          {
            "name": "UsersApplication.java",
            "source": {
              "file": "server/users/src/main/java/app/bloom/users/UsersApplication.java",
              "line": 1,
              "found": true
            }
          }
        ]
      },
      "files": 56,
      "source": {
        "file": "compose.yml",
        "line": 106,
        "found": true
      }
    },
    {
      "id": "db-users",
      "title": "bloom_users",
      "type": "database",
      "state": "implemented",
      "description": "Отдельная база сервиса users в общем PostgreSQL-контейнере. Не отдельный сервер PostgreSQL.",
      "source": {
        "file": "compose.yml",
        "line": 118,
        "found": true
      },
      "endpoints": [],
      "layers": {}
    },
    {
      "id": "interactions",
      "title": "Interactions",
      "type": "java",
      "state": "stub",
      "port": 8083,
      "description": "Заготовка сервиса взаимодействий и совпадений.",
      "published": [],
      "inCompose": true,
      "profiles": [
        "stubs"
      ],
      "endpoints": [],
      "layers": {
        "config": [
          {
            "name": "OpenApiConfiguration.java",
            "source": {
              "file": "server/interactions/src/main/java/app/bloom/interactions/config/OpenApiConfiguration.java",
              "line": 1,
              "found": true
            }
          }
        ],
        "application": [
          {
            "name": "InteractionsApplication.java",
            "source": {
              "file": "server/interactions/src/main/java/app/bloom/interactions/InteractionsApplication.java",
              "line": 1,
              "found": true
            }
          }
        ]
      },
      "files": 2,
      "source": {
        "file": "compose.yml",
        "line": 223,
        "found": true
      }
    },
    {
      "id": "activities",
      "title": "Activities",
      "type": "java",
      "state": "stub",
      "port": 8084,
      "description": "Заготовка сервиса совместных активностей.",
      "published": [],
      "inCompose": true,
      "profiles": [
        "stubs"
      ],
      "endpoints": [],
      "layers": {
        "application": [
          {
            "name": "ActivitiesApplication.java",
            "source": {
              "file": "server/activities/src/main/java/app/bloom/activities/ActivitiesApplication.java",
              "line": 1,
              "found": true
            }
          }
        ],
        "config": [
          {
            "name": "OpenApiConfiguration.java",
            "source": {
              "file": "server/activities/src/main/java/app/bloom/activities/config/OpenApiConfiguration.java",
              "line": 1,
              "found": true
            }
          }
        ]
      },
      "files": 2,
      "source": {
        "file": "compose.yml",
        "line": 236,
        "found": true
      }
    },
    {
      "id": "chat",
      "title": "Chat",
      "type": "go",
      "state": "implemented",
      "port": 8091,
      "description": "Комнаты, история и WebSocket-сообщения. События о вложениях отправляются через outbox.",
      "published": [],
      "inCompose": true,
      "profiles": [],
      "endpoints": [
        {
          "method": "POST",
          "path": "/api/rooms",
          "internal": false,
          "origin": "Go @Router",
          "source": {
            "file": "server/chat/internal/delivery/http/room_handler.go",
            "line": 38,
            "found": true
          }
        },
        {
          "method": "GET",
          "path": "/api/rooms",
          "internal": false,
          "origin": "Go @Router",
          "source": {
            "file": "server/chat/internal/delivery/http/room_handler.go",
            "line": 74,
            "found": true
          }
        }
      ],
      "layers": {
        "application": [
          {
            "name": "main.go",
            "source": {
              "file": "server/chat/cmd/main.go",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "producer.go",
            "source": {
              "file": "server/chat/internal/broker/kafka/producer.go",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "remote_validator.go",
            "source": {
              "file": "server/chat/internal/pkg/auth/remote_validator.go",
              "line": 1,
              "found": true
            }
          }
        ],
        "delivery": [
          {
            "name": "middleware.go",
            "source": {
              "file": "server/chat/internal/delivery/http/middleware.go",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "room_handler.go",
            "source": {
              "file": "server/chat/internal/delivery/http/room_handler.go",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "client.go",
            "source": {
              "file": "server/chat/internal/delivery/websocket/client.go",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "handler.go",
            "source": {
              "file": "server/chat/internal/delivery/websocket/handler.go",
              "line": 1,
              "found": true
            }
          }
        ],
        "domain": [
          {
            "name": "message.go",
            "source": {
              "file": "server/chat/internal/domain/message.go",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "room.go",
            "source": {
              "file": "server/chat/internal/domain/room.go",
              "line": 1,
              "found": true
            }
          }
        ],
        "repository": [
          {
            "name": "message_repo.go",
            "source": {
              "file": "server/chat/internal/repository/postgres/message_repo.go",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "room_repo.go",
            "source": {
              "file": "server/chat/internal/repository/postgres/room_repo.go",
              "line": 1,
              "found": true
            }
          }
        ],
        "service": [
          {
            "name": "message_service.go",
            "source": {
              "file": "server/chat/internal/service/message_service.go",
              "line": 1,
              "found": true
            }
          }
        ],
        "worker": [
          {
            "name": "outbox.go",
            "source": {
              "file": "server/chat/internal/worker/outbox.go",
              "line": 1,
              "found": true
            }
          }
        ]
      },
      "files": 13,
      "source": {
        "file": "compose.yml",
        "line": 136,
        "found": true
      }
    },
    {
      "id": "db-chat",
      "title": "bloom_chat",
      "type": "database",
      "state": "implemented",
      "description": "Отдельная база сервиса chat в общем PostgreSQL-контейнере. Не отдельный сервер PostgreSQL.",
      "source": {
        "file": "compose.yml",
        "line": 141,
        "found": true
      },
      "endpoints": [],
      "layers": {}
    },
    {
      "id": "discovery",
      "title": "Discovery",
      "type": "go",
      "state": "stub",
      "port": 8092,
      "description": "Будущий поиск и подбор людей. Пока только Go-модуль.",
      "published": [],
      "inCompose": false,
      "profiles": [],
      "endpoints": [],
      "layers": {},
      "files": 0,
      "source": {
        "file": "server/discovery/go.mod",
        "line": 1,
        "found": true
      }
    },
    {
      "id": "media",
      "title": "Media",
      "type": "go",
      "state": "implemented",
      "port": 8093,
      "description": "Файлы на диске, метаданные в PostgreSQL. Kafka подтверждает использование вложений.",
      "published": [],
      "inCompose": true,
      "profiles": [],
      "endpoints": [
        {
          "method": "POST",
          "path": "/media/upload",
          "internal": false,
          "origin": "Go @Router",
          "source": {
            "file": "server/media/internal/delivery/http/media_handler.go",
            "line": 32,
            "found": true
          }
        },
        {
          "method": "GET",
          "path": "/media/{id}",
          "internal": false,
          "origin": "Go @Router",
          "source": {
            "file": "server/media/internal/delivery/http/media_handler.go",
            "line": 70,
            "found": true
          }
        }
      ],
      "layers": {
        "application": [
          {
            "name": "main.go",
            "source": {
              "file": "server/media/cmd/main.go",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "consumer.go",
            "source": {
              "file": "server/media/internal/broker/kafka/consumer.go",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "remote_validator.go",
            "source": {
              "file": "server/media/internal/pkg/auth/remote_validator.go",
              "line": 1,
              "found": true
            }
          }
        ],
        "delivery": [
          {
            "name": "media_handler.go",
            "source": {
              "file": "server/media/internal/delivery/http/media_handler.go",
              "line": 1,
              "found": true
            }
          },
          {
            "name": "middleware.go",
            "source": {
              "file": "server/media/internal/delivery/http/middleware.go",
              "line": 1,
              "found": true
            }
          }
        ],
        "domain": [
          {
            "name": "media.go",
            "source": {
              "file": "server/media/internal/domain/media.go",
              "line": 1,
              "found": true
            }
          }
        ],
        "repository": [
          {
            "name": "media_repo.go",
            "source": {
              "file": "server/media/internal/repository/postgres/media_repo.go",
              "line": 1,
              "found": true
            }
          }
        ],
        "service": [
          {
            "name": "media_service.go",
            "source": {
              "file": "server/media/internal/service/media_service.go",
              "line": 1,
              "found": true
            }
          }
        ],
        "worker": [
          {
            "name": "cron.go",
            "source": {
              "file": "server/media/internal/worker/cron.go",
              "line": 1,
              "found": true
            }
          }
        ]
      },
      "files": 9,
      "source": {
        "file": "compose.yml",
        "line": 162,
        "found": true
      }
    },
    {
      "id": "db-media",
      "title": "bloom_media",
      "type": "database",
      "state": "implemented",
      "description": "Отдельная база сервиса media в общем PostgreSQL-контейнере. Не отдельный сервер PostgreSQL.",
      "source": {
        "file": "compose.yml",
        "line": 167,
        "found": true
      },
      "endpoints": [],
      "layers": {}
    },
    {
      "id": "notifications",
      "title": "Notifications",
      "type": "go",
      "state": "stub",
      "port": 8094,
      "description": "Будущие уведомления. Пока только Go-модуль.",
      "published": [],
      "inCompose": false,
      "profiles": [],
      "endpoints": [],
      "layers": {},
      "files": 0,
      "source": {
        "file": "server/notifications/go.mod",
        "line": 1,
        "found": true
      }
    },
    {
      "id": "client",
      "title": "Приложение",
      "type": "client",
      "state": "concept",
      "description": "Условный мобильный клиент или Postman. В этой карте это участник сценария, а не проверенная реализация фронтенда.",
      "endpoints": [],
      "layers": {}
    },
    {
      "id": "kafka",
      "title": "Kafka",
      "type": "event",
      "state": "implemented",
      "description": "Асинхронная доставка событий. Топики и подписки показаны только там, где они найдены в просмотренном коде.",
      "source": {
        "file": "compose.yml",
        "line": 37,
        "found": true
      },
      "endpoints": [],
      "layers": {}
    },
    {
      "id": "files",
      "title": "Media storage",
      "type": "storage",
      "state": "implemented",
      "description": "Файлы в /app/uploads, Docker volume media-data. Это локальный диск, не S3.",
      "source": {
        "file": "compose.yml",
        "line": 178,
        "found": true
      },
      "endpoints": [],
      "layers": {}
    }
  ],
  "edges": [
    {
      "id": "gateway-identity",
      "from": "api-gateway",
      "to": "identity",
      "kind": "route",
      "label": "HTTP",
      "description": "Маршруты объявлены в Gateway. Наличие маршрута не проверяет доступность сервиса.",
      "routes": [
        {
          "id": "identity",
          "predicates": [
            "Path=/api/v1/auth/**"
          ],
          "filters": []
        }
      ],
      "source": {
        "file": "server/api-gateway/src/main/resources/application.yml",
        "line": 33,
        "found": true
      }
    },
    {
      "id": "identity-db",
      "from": "identity",
      "to": "db-identity",
      "kind": "sql",
      "label": "SQL",
      "description": "Подключение к собственной базе PostgreSQL из конфигурации Compose.",
      "source": {
        "file": "compose.yml",
        "line": 92,
        "found": true
      }
    },
    {
      "id": "gateway-users",
      "from": "api-gateway",
      "to": "users",
      "kind": "route",
      "label": "HTTP",
      "description": "Маршруты объявлены в Gateway. Наличие маршрута не проверяет доступность сервиса.",
      "routes": [
        {
          "id": "users",
          "predicates": [
            "Path=/api/v1/users/**,/api/v1/reports,/api/v1/interests"
          ],
          "filters": []
        }
      ],
      "source": {
        "file": "server/api-gateway/src/main/resources/application.yml",
        "line": 37,
        "found": true
      }
    },
    {
      "id": "users-db",
      "from": "users",
      "to": "db-users",
      "kind": "sql",
      "label": "SQL",
      "description": "Подключение к собственной базе PostgreSQL из конфигурации Compose.",
      "source": {
        "file": "compose.yml",
        "line": 118,
        "found": true
      }
    },
    {
      "id": "gateway-interactions",
      "from": "api-gateway",
      "to": "interactions",
      "kind": "planned",
      "label": "HTTP",
      "description": "Маршруты объявлены в Gateway. Сервис пока заготовка.",
      "routes": [
        {
          "id": "interactions",
          "predicates": [
            "Path=/api/v1/interactions/**,/api/v1/matches/**"
          ],
          "filters": []
        }
      ],
      "source": {
        "file": "server/api-gateway/src/main/resources/application.yml",
        "line": 41,
        "found": true
      }
    },
    {
      "id": "gateway-activities",
      "from": "api-gateway",
      "to": "activities",
      "kind": "planned",
      "label": "HTTP",
      "description": "Маршруты объявлены в Gateway. Сервис пока заготовка.",
      "routes": [
        {
          "id": "activities",
          "predicates": [
            "Path=/api/v1/activities/**"
          ],
          "filters": []
        }
      ],
      "source": {
        "file": "server/api-gateway/src/main/resources/application.yml",
        "line": 45,
        "found": true
      }
    },
    {
      "id": "gateway-chat",
      "from": "api-gateway",
      "to": "chat",
      "kind": "route",
      "label": "HTTP / WS",
      "description": "Маршруты объявлены в Gateway. Наличие маршрута не проверяет доступность сервиса.",
      "routes": [
        {
          "id": "chat-websocket",
          "predicates": [
            "Path=/api/v1/ws"
          ],
          "filters": [
            "SetPath=/ws"
          ]
        },
        {
          "id": "chat-rooms",
          "predicates": [
            "Path=/api/v1/conversations",
            "Method=GET,POST"
          ],
          "filters": [
            "SetPath=/api/rooms"
          ]
        },
        {
          "id": "chat",
          "predicates": [
            "Path=/api/v1/conversations/**"
          ],
          "filters": []
        },
        {
          "id": "chat-test-client",
          "predicates": [
            "Path=/test-client"
          ],
          "filters": [
            "SetPath=/"
          ]
        },
        {
          "id": "chat-test-token",
          "predicates": [
            "Path=/api/v1/test/token"
          ],
          "filters": [
            "SetPath=/api/test/token"
          ]
        }
      ],
      "source": {
        "file": "server/api-gateway/src/main/resources/application.yml",
        "line": 49,
        "found": true
      }
    },
    {
      "id": "chat-db",
      "from": "chat",
      "to": "db-chat",
      "kind": "sql",
      "label": "SQL",
      "description": "Подключение к собственной базе PostgreSQL из конфигурации Compose.",
      "source": {
        "file": "compose.yml",
        "line": 141,
        "found": true
      }
    },
    {
      "id": "gateway-discovery",
      "from": "api-gateway",
      "to": "discovery",
      "kind": "planned",
      "label": "HTTP",
      "description": "Маршруты объявлены в Gateway. Сервис пока заготовка.",
      "routes": [
        {
          "id": "discovery",
          "predicates": [
            "Path=/api/v1/discovery/**"
          ],
          "filters": []
        }
      ],
      "source": {
        "file": "server/api-gateway/src/main/resources/application.yml",
        "line": 78,
        "found": true
      }
    },
    {
      "id": "gateway-media",
      "from": "api-gateway",
      "to": "media",
      "kind": "route",
      "label": "HTTP",
      "description": "Маршруты объявлены в Gateway. Наличие маршрута не проверяет доступность сервиса.",
      "routes": [
        {
          "id": "media",
          "predicates": [
            "Path=/api/v1/media/**"
          ],
          "filters": []
        }
      ],
      "source": {
        "file": "server/api-gateway/src/main/resources/application.yml",
        "line": 82,
        "found": true
      }
    },
    {
      "id": "media-db",
      "from": "media",
      "to": "db-media",
      "kind": "sql",
      "label": "SQL",
      "description": "Подключение к собственной базе PostgreSQL из конфигурации Compose.",
      "source": {
        "file": "compose.yml",
        "line": 167,
        "found": true
      }
    },
    {
      "id": "gateway-notifications",
      "from": "api-gateway",
      "to": "notifications",
      "kind": "planned",
      "label": "HTTP",
      "description": "Маршруты объявлены в Gateway. Сервис пока заготовка.",
      "routes": [
        {
          "id": "notifications",
          "predicates": [
            "Path=/api/v1/notifications/**"
          ],
          "filters": []
        }
      ],
      "source": {
        "file": "server/api-gateway/src/main/resources/application.yml",
        "line": 86,
        "found": true
      }
    },
    {
      "id": "client-gateway",
      "from": "client",
      "to": "api-gateway",
      "kind": "route",
      "label": ":8080 · HTTP / WS",
      "description": "Публичная точка входа приложения. Прямой introspect из Postman использует отдельный опубликованный порт Identity.",
      "source": {
        "file": "compose.yml",
        "line": 214,
        "found": true
      }
    },
    {
      "id": "media-files",
      "from": "media",
      "to": "files",
      "kind": "sql",
      "label": "Файлы",
      "description": "Media сохраняет содержимое файла на диск.",
      "source": {
        "file": "server/media/internal/service/media_service.go",
        "line": 31,
        "found": true
      }
    },
    {
      "id": "users-identity",
      "from": "users",
      "to": "identity",
      "kind": "http",
      "label": "Проверка сессии",
      "description": "Users отправляет accessToken в POST /internal/identity/introspect. Identity возвращает active и accountId. Также клиент проверяет активные аккаунты и запрашивает удаление.",
      "source": {
        "file": "server/users/src/main/java/app/bloom/users/client/IdentityClient.java",
        "line": 33,
        "found": true
      }
    },
    {
      "id": "users-media",
      "from": "users",
      "to": "media",
      "kind": "gap",
      "label": "Контракт не совпадает",
      "description": "Users вызывает /internal/media/profile-photos/validate. В текущих Go-обработчиках Media этот метод не найден. Это незавершённая интеграция, а не рабочая связь.",
      "source": {
        "file": "server/users/src/main/java/app/bloom/users/client/MediaClient.java",
        "line": 31,
        "found": true
      }
    },
    {
      "id": "identity-kafka",
      "from": "identity",
      "to": "kafka",
      "kind": "event",
      "label": "bloom.identity.v1",
      "description": "OutboxPublisher публикует события аккаунта и отзыва сессий. Наличие события не означает, что все сервисы уже подписаны на него.",
      "source": {
        "file": "server/identity/src/main/java/app/bloom/identity/events/OutboxPublisher.java",
        "line": 36,
        "found": true
      }
    },
    {
      "id": "users-kafka",
      "from": "users",
      "to": "kafka",
      "kind": "event",
      "label": "bloom.users.v1",
      "description": "События изменений пользователей из transactional outbox.",
      "source": {
        "file": "server/users/src/main/java/app/bloom/users/events/OutboxPublisher.java",
        "line": 42,
        "found": true
      }
    },
    {
      "id": "chat-kafka",
      "from": "chat",
      "to": "kafka",
      "kind": "event",
      "label": "media.message_saved",
      "description": "После сохранения сообщения с вложениями outbox-воркер публикует событие для Media.",
      "source": {
        "file": "server/chat/internal/worker/outbox.go",
        "line": 33,
        "found": true
      }
    },
    {
      "id": "kafka-media",
      "from": "kafka",
      "to": "media",
      "kind": "event",
      "label": "media-group",
      "description": "Media читает media.message_saved, извлекает ID из URL вложений и помечает файлы постоянными.",
      "source": {
        "file": "server/media/cmd/main.go",
        "line": 91,
        "found": true
      }
    }
  ],
  "notes": [
    {
      "service": "chat",
      "title": "Авторизация пока отдельная",
      "text": "Chat генерирует собственный RSA-ключ. Проверка токенов через Identity в этом коде ещё не подключена.",
      "source": {
        "file": "server/chat/cmd/main.go",
        "line": 1,
        "found": false
      },
      "review": true
    },
    {
      "service": "media",
      "title": "Контроль доступа требует реализации",
      "text": "В просмотренных Upload и ServeMedia нет проверки пользователя и участника чата. Эта карта не обозначает файлы как приватные.",
      "source": {
        "file": "server/media/internal/delivery/http/media_handler.go",
        "line": 71,
        "found": true
      },
      "review": false
    },
    {
      "service": "users",
      "title": "Проверка фото не подключена",
      "text": "Клиент проверки фото существует в Users, но соответствующий внутренний endpoint Media пока не реализован.",
      "source": {
        "file": "server/users/src/main/java/app/bloom/users/client/MediaClient.java",
        "line": 31,
        "found": true
      },
      "review": false
    }
  ],
  "scenarios": [
    {
      "id": "login",
      "title": "Вход в приложение",
      "subtitle": "От телефона до access-токена",
      "icon": "key",
      "steps": [
        {
          "edge": "client-gateway",
          "title": "Приложение отправляет данные входа",
          "text": "POST /api/v1/auth/login. Пользователь передаёт телефон и пароль через публичный Gateway."
        },
        {
          "edge": "gateway-identity",
          "title": "Gateway направляет запрос",
          "text": "Убирает префикс /api/v1. Identity получает POST /auth/login."
        },
        {
          "edge": "identity-db",
          "title": "Identity проверяет аккаунт",
          "text": "Проверяет учётные данные и состояние аккаунта, сохраняет refresh-сессию в своей БД."
        },
        {
          "edge": "gateway-identity",
          "reverse": true,
          "title": "Identity выдаёт пару токенов",
          "text": "Ответ содержит accessToken, refreshToken и срок действия. Gateway передаёт его приложению."
        },
        {
          "edge": "client-gateway",
          "reverse": true,
          "title": "Токены получает приложение",
          "text": "Клиент сохраняет токены. Access-токен прикладывается к следующим запросам в Authorization: Bearer …"
        }
      ]
    },
    {
      "id": "profile",
      "title": "Открыть свою анкету",
      "subtitle": "Users проверяет токен через Identity",
      "icon": "user",
      "steps": [
        {
          "edge": "client-gateway",
          "title": "Запрос с токеном",
          "text": "GET /api/v1/users/me. Приложение прикладывает пользовательский access-токен."
        },
        {
          "edge": "gateway-users",
          "title": "Запрос приходит в Users",
          "text": "Gateway сохраняет Authorization и удаляет недоверенные служебные заголовки."
        },
        {
          "edge": "users-identity",
          "title": "Users проверяет сессию",
          "text": "Внутренний POST /internal/identity/introspect: access-токен в JSON, служебный токен в заголовке."
        },
        {
          "edge": "users-identity",
          "reverse": true,
          "title": "Identity возвращает accountId",
          "text": "При active=true Users знает, кто сделал запрос. При недоступной проверке нельзя считать пользователя авторизованным."
        },
        {
          "edge": "users-db",
          "title": "Чтение анкеты",
          "text": "Users читает профиль из своей БД по проверенному ID. Ответ возвращается через Gateway."
        }
      ]
    },
    {
      "id": "media",
      "title": "Фотография в сообщении",
      "subtitle": "Файл → сообщение → событие Kafka",
      "icon": "image",
      "steps": [
        {
          "edge": "gateway-media",
          "title": "Загрузка файла через Gateway",
          "text": "POST /api/v1/media/upload с multipart-файлом. В текущем Media проверка владельца ещё не реализована."
        },
        {
          "edge": "media-files",
          "title": "Файл записывается на диск",
          "text": "Media сохраняет содержимое в uploads; Docker сохраняет каталог в media-data."
        },
        {
          "edge": "media-db",
          "title": "Метаданные сохраняются в БД",
          "text": "Media возвращает URL файла. Приложение добавляет его во вложение сообщения."
        },
        {
          "edge": "gateway-chat",
          "title": "Приложение отправляет сообщение по WS",
          "text": "Gateway проксирует /api/v1/ws в /ws. Chat пока использует собственные тестовые JWT."
        },
        {
          "edge": "chat-db",
          "title": "Сообщение и событие — в транзакции",
          "text": "Chat сохраняет сообщение, вложения и запись media.message_saved в outbox_events."
        },
        {
          "edge": "chat-kafka",
          "title": "Outbox публикует событие",
          "text": "Фоновый воркер читает outbox и отправляет событие в Kafka."
        },
        {
          "edge": "kafka-media",
          "title": "Media подтверждает использование",
          "text": "Consumer группы media-group получает URL вложений и помечает файлы постоянными."
        }
      ]
    },
    {
      "id": "block",
      "title": "Блокировка аккаунта",
      "subtitle": "Сессии и надёжная отправка события",
      "icon": "shield",
      "steps": [
        {
          "edge": "identity-db",
          "title": "Внутренняя операция модерации",
          "text": "PUT /internal/identity/accounts/{id}/status вызывается напрямую в приватной сети со служебными полномочиями. Gateway не публикует этот метод."
        },
        {
          "edge": "identity-db",
          "title": "Изменение состояния аккаунта",
          "text": "Identity блокирует аккаунт, отзывает сессии и записывает account.blocked в outbox в одной транзакции."
        },
        {
          "edge": "identity-kafka",
          "title": "Событие доставляется в Kafka",
          "text": "OutboxPublisher отправляет событие в bloom.identity.v1. Потребители этого события в Chat пока не подключены; закрытие WS по нему не реализовано."
        }
      ]
    }
  ],
  "revision": "18b9a7bdb9ee",
  "generated": "2026-09-29T09:52:03.758781+00:00",
  "notice": "Статический анализ конфигурации и исходников; не мониторинг запущенных сервисов. Сценарии описаны вручную и требуют сверки после изменений логики."
};
