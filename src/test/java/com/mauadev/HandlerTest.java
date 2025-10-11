package com.mauadev;

import java.lang.reflect.Type;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mauadev.code.Handler;

/**
 * Testes unitários para a classe Handler.
 */
public class HandlerTest {

    private Handler handler;
    private Context testContext;
    private Gson gson;

    // @BeforeEach garante que este método seja executado antes de cada @Test
    @BeforeEach
    public void setUp() {
        handler = new Handler();
        gson = new Gson();
        // Criamos um Context de teste "mockado" para passar para o handler.
        testContext = new TestContext();
    }

    @Test
    @DisplayName("Teste da rota / (Info) - Deve retornar informações da cobra com status 200")
    public void testHandleInfo_shouldReturnSnakeInfo() {
        // Arrange: Prepara a requisição para a rota "/"
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/");

        // Act: Executa o método a ser testado
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, testContext);

        // Assert: Verifica os resultados
        assertEquals(200, response.getStatusCode());
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
        assertNotNull(response.getBody());

        // Analisa o corpo JSON para verificar seu conteúdo
        Type mapType = new TypeToken<Map<String, String>>() {}.getType();
        Map<String, String> body = gson.fromJson(response.getBody(), mapType);

        assertEquals("1", body.get("apiversion"));
        assertNotNull(body.get("author"));
        assertNotNull(body.get("color"));
        assertNotNull(body.get("head"));
        assertNotNull(body.get("tail"));
    }

    @Test
    @DisplayName("Teste da rota /start - Deve retornar status 200 OK sem corpo")
    public void testHandleStart_shouldReturn200OK() {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/start");
        // O corpo da requisição pode ser um JSON com o estado do jogo, mas para este teste não é necessário
        request.setBody("{}"); 

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, testContext);

        // Assert
        assertEquals(200, response.getStatusCode());
        // O handler para /start não define um corpo de resposta, então ele pode ser nulo ou vazio
        assertTrue(response.getBody() == null || response.getBody().isEmpty());
    }

    @Test
    @DisplayName("Teste da rota /move - Deve retornar um movimento com status 200")
    public void testHandleMove_shouldReturnMoveResponse() {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/move");
        request.setBody("{\"game\": {}, \"turn\": 1, \"board\": {}, \"you\": {}}"); // Exemplo de corpo

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, testContext);

        // Assert
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getBody());

        Type mapType = new TypeToken<Map<String, String>>() {}.getType();
        Map<String, String> body = gson.fromJson(response.getBody(), mapType);

        //assertEquals("up", body.get("move")); // Verifica a lógica simples atual
        assertEquals("Estou indo para cima!", body.get("shout"));
    }

    @Test
    @DisplayName("Teste da rota /end - Deve retornar status 200 OK sem corpo")
    public void testHandleEnd_shouldReturn200OK() {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/end");
        request.setBody("{}");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, testContext);

        // Assert
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody() == null || response.getBody().isEmpty());
    }

    @Test
    @DisplayName("Teste de rota inválida - Deve retornar erro 404 Not Found")
    public void testInvalidPath_shouldReturn404NotFound() {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/caminho-que-nao-existe");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, testContext);

        // Assert
        assertEquals(404, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Type mapType = new TypeToken<Map<String, String>>() {}.getType();
        Map<String, String> body = gson.fromJson(response.getBody(), mapType);

        assertEquals("Path not found", body.get("error"));
    }

    /**
     * Uma implementação simples da interface Context para fins de teste.
     * Ela fornece um logger que imprime no console, evitando NullPointerException.
     */
    private static class TestContext implements Context {
        @Override
        public String getAwsRequestId() { return "test-request-id"; }
        @Override
        public String getLogGroupName() { return "test-log-group"; }
        @Override
        public String getLogStreamName() { return "test-log-stream"; }
        @Override
        public String getFunctionName() { return "test-function"; }
        @Override
        public String getFunctionVersion() { return "1.0"; }
        @Override
        public String getInvokedFunctionArn() { return "arn:aws:lambda:us-east-1:123456789012:function:test-function"; }
        @Override
        public com.amazonaws.services.lambda.runtime.CognitoIdentity getIdentity() { return null; }
        @Override
        public com.amazonaws.services.lambda.runtime.ClientContext getClientContext() { return null; }
        @Override
        public int getRemainingTimeInMillis() { return 30000; }
        @Override
        public int getMemoryLimitInMB() { return 512; }
        @Override
        public LambdaLogger getLogger() {
            // Retorna um logger simples que imprime no console durante o teste.
            return new TestLogger();
        }
    }

    private static class TestLogger implements LambdaLogger {
        @Override
        public void log(String message) {
            System.out.println(message);
        }
        @Override
        public void log(byte[] message) {
            System.out.println(new String(message));
        }
    }
    
    @Test
    @DisplayName("Teste da rota/move")
    public void testHandleMove_withoutBoardData(){
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/move");

        String jsonRequestBody = """
{
  "game": {
    "id": "totally-unique-game-id",
    "ruleset": {
      "name": "standard",
      "version": "v1.1.15",
      "settings": {
        "foodSpawnChance": 15,
        "minimumFood": 1,
        "hazardDamagePerTurn": 14
      }
    },
    "map": "standard",
    "source": "league",
    "timeout": 500
  },
  "turn": 14,
  "board": {
    "height": 11,
    "width": 11,
    "food": [
      {"x": 1, "y": 2},
      {"x": 2, "y": 1},
      {"x": 2, "y": 9},
      {"x": 5, "y": 9}
    ],
    "hazards": [
      {"x": 3, "y": 2}
    ],
    "snakes": [
      {
        "id": "snake-508e96ac-94ad-11ea-bb37",
        "name": "My Snake",
        "health": 100,
        "body": [
          {"x": 4, "y": 7},
          {"x": 3, "y": 7},
          {"x": 2, "y": 7},
          {"x": 2, "y": 6}
        ],
        "latency": "111",
        "head": {"x": 4, "y": 7},
        "length": 4,
        "shout": "why are we shouting??",
        "customizations":{
          "color":"#FF0000",
          "head":"pixel",
          "tail":"pixel"
        }
      },
      {
        "id": "snake-b67f4906-94ae-11ea-bb37",
        "name": "Another Snake",
        "health": 61,
        "body": [
          {"x": 0, "y": 5},
          {"x": 1, "y": 5},
          {"x": 1, "y": 4},
          {"x": 0, "y": 4}
        ],
        "latency": "222",
        "head": {"x": 0, "y": 5},
        "length": 4,
        "shout": "I'm not really sure...",
        "customizations":{
          "color":"#26CF04",
          "head":"silly",
          "tail":"curled"
        }
      }
    ]
  },
  "you": {
    "id": "snake-508e96ac-94ad-11ea-bb37",
    "name": "My Snake",
    "health": 93,
    "body": [
      {"x": 2, "y": 5},
      {"x": 2, "y": 4},
      {"x": 3, "y": 4},
      {"x": 3, "y": 3},
      {"x": 3, "y": 2},
      {"x": 3, "y": 1},
      {"x": 3, "y": 0}
    ],
    "latency": "111",
    "head": {"x": 2, "y": 5},
    "length": 7,
    "shout": "why are we shouting??",
    "customizations": {
      "color":"#FF0000",
      "head":"pixel",
      "tail":"pixel"
    }
  }
}
                """;
        
        request.setBody(jsonRequestBody);

        // act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, testContext);

        // Assert
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getBody());

        Type mapType = new TypeToken<Map<String, String>>() {}.getType();
        Map<String, String> body = gson.fromJson(response.getBody(), mapType);

        //assertEquals("up", body.get("move")); // Verifica a lógica simples atual
        assertEquals("Estou indo para cima!", body.get("shout"));
    }
}