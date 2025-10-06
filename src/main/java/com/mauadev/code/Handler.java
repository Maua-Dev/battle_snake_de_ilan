package com.mauadev.code;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;

public class Handler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    // Gson é uma biblioteca para converter objetos Java para JSON e vice-versa.
    private static final Gson gson = new Gson();
    private int esq, dir, cima, baixo;
    private boolean test;
    private String requestBody;
    private GameState gameState;
    private Board board;
    private Snake you;
    private Direction mov;

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        String path = request.getPath();
        Object responseBody = null;

        try {
            // Roteador para os diferentes endpoints da API BattleSnake
            switch (path) {
                case "/":
                    // Informações da sua cobra
                    responseBody = handleInfo();
                    break;
                case "/start":
                    // Lógica para o início do jogo
                    handleStart(request, context);
                    break;
                case "/move":
                    // Lógica para decidir o próximo movimento
                    responseBody = handleMove(request, context);
                    break;
                case "/end":
                    // Lógica para o fim do jogo
                    handleEnd(request, context);
                    break;
                default:
                    // Se a rota não for encontrada, retorna um erro 404
                    // Precisamos passar \ antes das aspas para não dar erro quando convertemos pra json
                    return response.withStatusCode(404).withBody("{\"error\": \"Path not found\"}");
            }

            // Configura a resposta de sucesso
            response.setStatusCode(200);
            response.setHeaders(Collections.singletonMap("Content-Type", "application/json"));
            if (responseBody != null) {
                // Converte o objeto de resposta para uma string JSON
                response.setBody(gson.toJson(responseBody));
            }

        } catch (Exception e) {
            // Em caso de erro em qualquer parte da lógica
            context.getLogger().log("ERROR: " + e.getMessage());
            response.setStatusCode(500);
            response.setBody(String.format("{\"error\": \"%s\"}", e.getMessage()));
        }

        return response;
    }

    /**
     * Responde ao endpoint / com as informações da sua cobra. 🐍
     */
    private Map<String, String> handleInfo() {
        Map<String, String> info = new HashMap<>();
        info.put("apiversion", "1");
        info.put("author", "Ilan");
        info.put("color", "#5b23c3ff"); 
        info.put("head", "sand-worm");
        info.put("tail", "mlh-gene");
        return info;
    }

    /**
     * Chamado no início de cada jogo. Não precisa retornar nada.
     */
    private void handleStart(APIGatewayProxyRequestEvent request, Context context) {
        // Você pode usar o corpo da requisição (request.getBody()) para obter o estado inicial do jogo.
        context.getLogger().log("Game Started!");
    }

    /**
     * Chamado a cada turno para decidir o movimento. 🕹️
     */
    private Map<String, String> handleMove(APIGatewayProxyRequestEvent request, Context context) {
        // AQUI VAI A LÓGICA DA SUA COBRA!
        // O corpo da requisição (request.getBody()) contém o estado atual do tabuleiro.
        // Você deve analisá-lo para tomar uma decisão inteligente.
        // Exemplo de lógica muito simples: sempre se mover para cima.
        // CUIDADO: Isso fará sua cobra bater na parede rapidamente!

        // "up", "down", "left", "right"
        // health (integer) : 0 - 100
        // body (array) : position on board (head to tail)
        // head (object) : body(0)
        // lenght (integer) : = body.length
        // board: 11x11 -> [0 10] (0,0) Y cima+, X direita+
            // heigh : numero de linhas em y
            // widht : numero de colunas em X
        // hazards (array) : com localizacao dos perigos
        // food (array) : com localizacao das comidas
        // snakes (array) : com quais snakes permanecem em jogo
        Map<String, String> move = new HashMap<>();
        mov = new Direction();

        requestBody = request.getBody();
        gameState = gson.fromJson(requestBody, GameState.class);
        board = gameState.getBoard();
        you = (Snake)gameState.getYou();

        //
        
        cima = 1;
        baixo = 2;
        dir = 3;
        esq = 4;
        

        // movimentacao padrao

        test = false;
        Coordinate cabeça = you.getHead();

        if(cabeça != null){
        // not colide with board
            if(cabeça.getY()==board.getHeight()-1){
                mov.setUp(-1);
            }
            if(cabeça.getY()==0){
                mov.setDown(-1);
            }
            if(cabeça.getX()==board.getWidth() -1){
                mov.setRight(-1);
            }
            if(cabeça.getX()==0){
                mov.setLeft(-1);
            }
        // not colide with body

            for(int i = 1; i <you.getBody().size()-1;i++){
                Coordinate position = you.getBody().get(i);
                if(cabeça.getY()==position.getY()-1 && cabeça.getX()==position.getX()){
                    mov.setUp(-1);
                }
                if(cabeça.getY()==position.getY()+1 && cabeça.getX()==position.getX()){
                    mov.setDown(-1);
                }
                if(cabeça.getX()==position.getX()-1 && cabeça.getY()==position.getY()){
                    mov.setRight(-1);
                }
                if(cabeça.getX()==position.getX()+1 && cabeça.getY()==position.getY()){
                    mov.setLeft(-1);
                }
            }
        // not colide with other snakes
            for(Snake s:board.getSnakes()){
                for(int i = 0; i <s.getBody().size();i++){
                    Coordinate position = s.getBody().get(i);
                    if(i == 0){
                    // prioridades
                    if(cabeça.getY()==position.getY()-1 && cabeça.getX()==position.getX()-1){
                        // - | - | O
                        // - | Y | -
                        // - | - | -
                        mov.setPriority(esq);
                        mov.setPriority(baixo);
                    }

                    if(cabeça.getY()==position.getY()-1 && cabeça.getX()==position.getX()+1){
                        // O | - | -
                        // - | Y | -
                        // - | - | -
                        mov.setPriority(dir);
                        mov.setPriority(baixo);
                    }

                    if(cabeça.getY()==position.getY()+1 && cabeça.getX()==position.getX()-1){
                        // - | - | -
                        // - | Y | -
                        // - | - | O
                        mov.setPriority(esq);
                        mov.setPriority(cima);
                    }

                    if(cabeça.getY()==position.getY()+1 && cabeça.getX()==position.getX()+1){
                        // - | - | -
                        // - | Y | -
                        // O | - | -
                        mov.setPriority(dir);
                        mov.setPriority(cima);
                    }
                    }
                    if(cabeça.getY()==position.getY()-1 && cabeça.getX()==position.getX()){
                        mov.setUp(-1);
                    }
                    if(cabeça.getY()==position.getY()+1 && cabeça.getX()==position.getX()){
                        mov.setDown(-1);
                    }
                    if(cabeça.getX()==position.getX()-1 && cabeça.getY()==position.getY()){
                        mov.setRight(-1);
                    }
                    if(cabeça.getX()==position.getX()+1 && cabeça.getY()==position.getY()){
                        mov.setLeft(-1);
                    }
                }
            }
            // path to fruit
            if(you.getHealth()<50){
                if(!board.getFood().isEmpty()){
                    Coordinate target = board.getFood().get(0);
                    // mecanica para ditar qual movimento e prioridade
                    if(cabeça.getY()<target.getY()){
                        if(mov.getUp()!=-1){
                            move.put("move", "up");
                            test = true;
                        }
                    }

                    if(cabeça.getY()>target.getY()){
                        if(mov.getDown()!=-1){
                            move.put("move", "down");
                            test = true;
                        }
                    }

                    if(cabeça.getX()<target.getX()){
                        if(mov.getRight()!=-1){
                            move.put("move", "right");
                            test = true;
                        }
                    }

                    if(cabeça.getX()>target.getX()){
                        if(mov.getLeft()!=-1){
                            move.put("move", "left");
                            test = true;
                        }
                    }
                }
            }
            else{
                int[] decision = mov.finalPriority();
        for(int i:decision){
            System.out.println("Prioridade: "+i);
            if(i != -1){
                switch (i) {
                    case 1:
                        move.put("move", "up");
                        test = true;
                        break;
                    case 2:
                        move.put("move", "down");
                        test = true;
                        break;
                    case 3:
                        move.put("move", "right");
                        test = true;
                        break;
                    case 4:
                        move.put("move", "left");
                        test = true;
                        break;
                }
                break;
            }
        }
            }
        
        }
        //
        if(!test){
            move.put("move", "down");

        }

        move.put("shout", "Estou indo para cima!"); // Opcional

        return move;
    }

    /*
     * flood fill
     * 
     */
    /**
     * Chamado no final de cada jogo. Não precisa retornar nada.
     */
    private void handleEnd(APIGatewayProxyRequestEvent request, Context context) {
        // Você pode analisar a requisição para saber se venceu ou perdeu.
        context.getLogger().log("Game Ended!");
    }
}