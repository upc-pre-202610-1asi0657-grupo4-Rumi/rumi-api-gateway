@US02 @QA-04
Feature: Gateway authentication and authorization
  As the single entry point of Rumi
  The gateway validates the token issued by IAM at sign-in (US02 Inicio de sesión)
  So that only authenticated users with the right role reach the services (QA-04)

  Scenario: A request without token is rejected
    Given the client has no token
    When the client sends a GET request to "/api/v1/alerts"
    Then the response status is 401
    And the response is a problem detail with the message "A valid bearer token is required to access this resource."

  Scenario Outline: An invalid token is rejected
    Given the client holds <article> "<token>" token
    When the client sends a GET request to "/api/v1/alerts"
    Then the response status is 401
    And the response is a problem detail with the message "A valid bearer token is required to access this resource."

    Examples:
      | article | token                      |
      | a       | malformed                  |
      | a       | signed with another secret |
      | an      | expired                    |

  Scenario: A resident cannot reach an administrator resource
    Given the client holds a "resident" token
    When the client sends a GET request to "/api/v1/readings"
    Then the response status is 403
    And the response is a problem detail with the message "Your role is not allowed to access this resource."

  Scenario: An administrator reaches an administrator resource
    Given the client holds an "administrator" token
    When the client sends a GET request to "/api/v1/readings"
    Then the request reaches the service

  Scenario: An administrator without buildings yet is accepted
    Given the client holds an "administrator without buildings" token
    When the client sends a GET request to "/api/v1/buildings"
    Then the request reaches the service
    And the service receives the header "X-Building-Id" with ""

  Scenario: A resident can read buildings but cannot change them
    Given the client holds a "resident" token
    When the client sends a GET request to "/api/v1/buildings"
    Then the request reaches the service
    When the client sends a POST request to "/api/v1/buildings"
    Then the response status is 403

  Scenario Outline: Public routes do not need a token
    Given the client has no token
    When the client sends a GET request to "<path>"
    Then the response status is 200

    Examples:
      | path                           |
      | /api/v1/authentication/sign-in |
      | /api/v1/gateway/health         |
      | /v3/api-docs                   |

  Scenario: User headers sent by the client are removed
    Given the client has no token
    And the client sends the header "X-User-Id" with "00000000-0000-0000-0000-000000000000"
    And the client sends the header "X-Role" with "ADMINISTRATOR"
    And the client sends the header "X-Building-Id" with "9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d"
    When the client sends a GET request to "/api/v1/authentication/sign-in"
    Then the request reaches the service
    And the service does not receive the header "X-User-Id"
    And the service does not receive the header "X-Role"
    And the service does not receive the header "X-Building-Id"

  Scenario: The user of the token is forwarded to the service
    Given the client holds a "resident" token
    And the client sends the header "X-Role" with "ADMINISTRATOR"
    When the client sends a GET request to "/api/v1/buildings"
    Then the request reaches the service
    And the service receives the header "X-User-Id" with "7d3c5b1e-2f4a-4c8e-9b6d-1a2b3c4d5e6f"
    And the service receives the header "X-Role" with "RESIDENT"
    And the service receives the header "X-Building-Id" with "3f2c8a10-6b7d-4e9f-a1b2-c3d4e5f60718"
