package com.mauadev.code;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;

public class Handler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    // Gson é uma biblioteca para converter objetos Java para JSON e vice-versa.
    private static final Gson gson = new Gson();

    // "tags"
    private static final int
        // for array
        _up = 0,
        _down = 1,
        _right = 2,
        _left = 3,
        _notAvalible = -1,
        // comparison 
            //(boardMap)
            _empty = 4,
            _wall = 5,
            _fruit = 6,
            _head = 7,
            _tail = 8,
            _checked = -1,
            //(dangerMap)
            d_stantardWeigth = 10,
            d_weak = -2,
            d_closeToEqual = -1,
            d_equal = 0,
            d_dangerous = 1,
            d_avoid = 2,
            d_unAvalible = -999,
        // decisions
        medium_health = 60,
        low_health = 35,
        State_hunter = 0,
        State_looper = 1,
        State_tailChase = 2,
        State_openSpace = 3,
        State_criticalHealth = 4,
        State_foodHunger = 5;

    
    // board creation
    private int[][] boardMap;
    private int[][] dangerMap;
    private int width = 11, heigth = 11;
    private int widthLimit = 10, heigthLimit = 10; // 1-> [0 10]

    // request information
    private String requestBody;
    private GameState gameState;
    private Board board;
    private Snake you;
    private Coordinate youCabeca;
        // used for _head and _tail
    private Map<String, HeadEntity> headTail;
    private HeadEntity sCabeca, sCauda;
    // request response
    private Map<String, String> move;

    // check if the code decided the move
    // if not -> uses defaul move
    private boolean isDecided;

    // decision variables
        // uses _up,_down,_right,_left
    private int[] priorityMove; // uses pM[_index_]++; if founds _empty
    private boolean[] isAnOption;
        // used for the final move decision
    private int[] finalDecision = new int[4]; // is altered every round, but does not need reset

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
                    // clear board after decision has been made
                    clearBoard();
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
        info.put("color", "#5900ffff"); 
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

        // get information
        requestBody = request.getBody();
        
        if(!request.getBody().equals("{}")){
            gameState = gson.fromJson(requestBody, GameState.class);
            board = gameState.getBoard();

        // set global variables
        width = board.getWidth();
        heigth = board.getHeight();
        boardMap = new int[width][heigth];
        dangerMap = new int[width][heigth];
        widthLimit = width-1;
        heigthLimit = heigth-1;
        //initialize board map and danger
        clearBoard();
        }
    }

    /**
     * Chamado a cada turno para decidir o movimento. 🕹️
     */
    private Map<String, String> handleMove(APIGatewayProxyRequestEvent request, Context context) {

        // check parameters initialization
        isDecided = false;
        isAnOption = new boolean[]{true,true,true,true};
        priorityMove = new int[]{1000,1000,1000,1000};
        headTail = new HashMap<>();
        move = new HashMap<>();

        // get information
        requestBody = request.getBody();
        gameState = gson.fromJson(requestBody, GameState.class);
        board = gameState.getBoard();
        you = (Snake)gameState.getYou();
        youCabeca = you.getHead();

        setBoards();
        Coordinate closestFood = null;
        int foodDistance = 999;
        if(!board.getFood().isEmpty()){
            for(Coordinate food : board.getFood()){
                int newDistance = calculateDistance(youCabeca, food);
                if(newDistance<foodDistance){
                    closestFood = food;
                    foodDistance = newDistance;
                }
            }
        }
        // decision analysies
        Coordinate[] possibility = new Coordinate[]{
            new Coordinate(youCabeca.getX(),youCabeca.getY()+1), //up
            new Coordinate(youCabeca.getX()-1,youCabeca.getY()-1),    // down
            new Coordinate(youCabeca.getX()+1,youCabeca.getY()),    //rigth
            new Coordinate(youCabeca.getX()-1,youCabeca.getY())     //left
        };

        for(int i = 0; i< 4;i++){

            if(!isWalkable(possibility[i])){
                isAnOption[i] = false;
                priorityMove[i] = -999;
            }
            else{

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
                    else
                    if(cabeça.getY()==position.getY()-1 && cabeça.getX()==position.getX()+1){
                        // O | - | -
                        // - | Y | -
                        // - | - | -
                        mov.setPriority(dir);
                        mov.setPriority(baixo);
                    }
                    else
                    if(cabeça.getY()==position.getY()+1 && cabeça.getX()==position.getX()-1){
                        // - | - | -
                        // - | Y | -
                        // - | - | O
                        mov.setPriority(esq);
                        mov.setPriority(cima);
                    }
                    else
                    if(cabeça.getY()==position.getY()+1 && cabeça.getX()==position.getX()+1){
                        // - | - | -
                        // - | Y | -
                        // O | - | -
                        mov.setPriority(dir);
                        mov.setPriority(cima);
                    }
                    else
                    if(cabeça.getY()==position.getY()-2 && cabeça.getX()==position.getX()){
                        // - | - | O | - | -
                        // - | - | - | - | -
                        // - | - | Y | - | -
                        // - | - | - | - | -
                        // - | - | - | - | -
                        mov.setPriority(esq);
                        mov.setPriority(baixo);
                        mov.setPriority(dir);
                    }
                    else
                    if(cabeça.getY()==position.getY()+2 && cabeça.getX()==position.getX()){
                        // - | - | - | - | -
                        // - | - | - | - | -
                        // - | - | Y | - | -
                        // - | - | - | - | -
                        // - | - | O | - | -
                        mov.setPriority(esq);
                        mov.setPriority(cima);
                        mov.setPriority(dir);
                    }
                    else
                    if(cabeça.getY()==position.getY() && cabeça.getX()==position.getX()-2){
                        // - | - | - | - | -
                        // - | - | - | - | -
                        // - | - | Y | - | O
                        // - | - | - | - | -
                        // - | - | - | - | -
                        mov.setPriority(esq);
                        mov.setPriority(baixo);
                        mov.setPriority(cima);
                    }
                    else
                    if(cabeça.getY()==position.getY() && cabeça.getX()==position.getX()+2){
                        // - | - | - | - | -
                        // - | - | - | - | -
                        // O | - | Y | - | -
                        // - | - | - | - | -
                        // - | - | - | - | -
                        mov.setPriority(dir);
                        mov.setPriority(baixo);
                        mov.setPriority(cima);
                    }else
                    if(cabeça.getY()==position.getY()-3 && cabeça.getX()==position.getX()){
                        // - | - | - | 0 | - | - | -
                        // - | - | O | - | O | - | -
                        // - | O | - | - | - | O | -
                        // O | - | - | Y | - | - | O
                        // - | O | - | - | - | O | -
                        // - | - | O | - | O | - | -
                        // - | - | - | O | - | - | -
                        mov.setPriority(dir);
                        mov.setPriority(baixo);
                        mov.setPriority(esq);
                    }else
                    if(cabeça.getY()==position.getY()+3 && cabeça.getX()==position.getX()){
                        // - | - | - | O | - | - | -
                        // - | - | O | - | O | - | -
                        // - | O | - | - | - | O | -
                        // O | - | - | Y | - | - | O
                        // - | O | - | - | - | O | -
                        // - | - | O | - | O | - | -
                        // - | - | - | 0 | - | - | -
                        mov.setPriority(dir);
                        mov.setPriority(cima);
                        mov.setPriority(esq);
                    }else
                    if(cabeça.getY()==position.getY() && cabeça.getX()==position.getX()-3){
                        // - | - | - | O | - | - | -
                        // - | - | O | - | O | - | -
                        // - | O | - | - | - | O | -
                        // O | - | - | Y | - | - | 0
                        // - | O | - | - | - | O | -
                        // - | - | O | - | O | - | -
                        // - | - | - | O | - | - | -
                        mov.setPriority(dir);
                        mov.setPriority(baixo);
                        mov.setPriority(cima);
                    }else
                    if(cabeça.getY()==position.getY() && cabeça.getX()==position.getX()+3){
                        // - | - | - | O | - | - | -
                        // - | - | O | - | O | - | -
                        // - | O | - | - | - | O | -
                        // 0 | - | - | Y | - | - | o
                        // - | O | - | - | - | O | -
                        // - | - | O | - | O | - | -
                        // - | - | - | O | - | - | -
                        mov.setPriority(esq);
                        mov.setPriority(baixo);
                        mov.setPriority(cima);
                    }else
                    if((cabeça.getY()==position.getY()+1 && cabeça.getX()==position.getX()+2)||
                    (cabeça.getY()==position.getY()+2 && cabeça.getX()==position.getX()+1)){
                        // - | - | - | O | - | - | -
                        // - | - | O | - | O | - | -
                        // - | O | - | - | - | O | -
                        // O | - | - | Y | - | - | o
                        // - | 0 | - | - | - | O | -
                        // - | - | 0 | - | O | - | -
                        // - | - | - | O | - | - | -
                        mov.setPriority(dir);
                        mov.setPriority(cima);
                    }else
                    if((cabeça.getY()==position.getY()-1 && cabeça.getX()==position.getX()+2)||
                    cabeça.getY()==position.getY()-2 && cabeça.getX()==position.getX()+1){
                        // - | - | - | O | - | - | -
                        // - | - | 0 | - | O | - | -
                        // - | 0 | - | - | - | O | -
                        // O | - | - | Y | - | - | o
                        // - | O | - | - | - | O | -
                        // - | - | O | - | O | - | -
                        // - | - | - | O | - | - | -
                        mov.setPriority(baixo);
                        mov.setPriority(dir);
                    }else
                    if((cabeça.getY()==position.getY()+1 && cabeça.getX()==position.getX()-2)||
                    (cabeça.getY()==position.getY()+2 && cabeça.getX()==position.getX()-1)){
                        // - | - | - | O | - | - | -
                        // - | - | O | - | O | - | -
                        // - | O | - | - | - | O | -
                        // O | - | - | Y | - | - | o
                        // - | O | - | - | - | 0 | -
                        // - | - | O | - | 0 | - | -
                        // - | - | - | O | - | - | -
                        mov.setPriority(esq);
                        mov.setPriority(cima);
                    }else
                    if((cabeça.getY()==position.getY()-1 && cabeça.getX()==position.getX()-2)
                     ||(cabeça.getY()==position.getY()-2 && cabeça.getX()==position.getX()-1)){
                        // - | - | - | O | - | - | -
                        // - | - | O | - | 0 | - | -
                        // - | O | - | - | - | 0 | -
                        // O | - | - | Y | - | - | o
                        // - | O | - | - | - | O | -
                        // - | - | O | - | O | - | -
                        // - | - | - | O | - | - | -
                        mov.setPriority(baixo);
                        mov.setPriority(esq);
                    }
                    else
                    if(cabeça.getY()==position.getY()-4 && cabeça.getX()==position.getX()){
                        // - | - | - | - | 0 | - | - | - | -
                        // - | - | - | o | - | o | - | - | -
                        // - | - | o | - | - | - | o | - | -
                        // - | o | - | - | - | - | - | o | -
                        // o | - | - | - | Y | - | - | - | o
                        // - | o | - | - | - | - | - | o | -
                        // - | - | o | - | - | - | o | - | -
                        // - | - | - | o | - | o | - | - | -
                        // - | - | - | - | o | - | - | - | -

                        mov.setPriority(dir);
                        mov.setPriority(baixo);
                        mov.setPriority(esq);
                    
                    }
                    else
                    if(cabeça.getY()==position.getY()+4 && cabeça.getX()==position.getX()){
                        // - | - | - | - | o | - | - | - | -
                        // - | - | - | o | - | o | - | - | -
                        // - | - | o | - | - | - | o | - | -
                        // - | o | - | - | - | - | - | o | -
                        // o | - | - | - | Y | - | - | - | o
                        // - | o | - | - | - | - | - | o | -
                        // - | - | o | - | - | - | o | - | -
                        // - | - | - | o | - | o | - | - | -
                        // - | - | - | - | 0 | - | - | - | -

                        mov.setPriority(dir);
                        mov.setPriority(cima);
                        mov.setPriority(esq);
                    
                    }
                    else
                    if(cabeça.getY()==position.getY() && cabeça.getX()==position.getX()-4){
                        // - | - | - | - | o | - | - | - | -
                        // - | - | - | o | - | o | - | - | -
                        // - | - | o | - | - | - | o | - | -
                        // - | o | - | - | - | - | - | o | -
                        // o | - | - | - | Y | - | - | - | 0
                        // - | o | - | - | - | - | - | o | -
                        // - | - | o | - | - | - | o | - | -
                        // - | - | - | o | - | o | - | - | -
                        // - | - | - | - | o | - | - | - | -

                        mov.setPriority(cima);
                        mov.setPriority(baixo);
                        mov.setPriority(esq);
                    
                    }
                    else
                    if(cabeça.getY()==position.getY() && cabeça.getX()==position.getX()+4){
                        // - | - | - | - | o | - | - | - | -
                        // - | - | - | o | - | o | - | - | -
                        // - | - | o | - | - | - | o | - | -
                        // - | o | - | - | - | - | - | o | -
                        // 0 | - | - | - | Y | - | - | - | o
                        // - | o | - | - | - | - | - | o | -
                        // - | - | o | - | - | - | o | - | -
                        // - | - | - | o | - | o | - | - | -
                        // - | - | - | - | o | - | - | - | -

                        mov.setPriority(dir);
                        mov.setPriority(baixo);
                        mov.setPriority(cima);
                    
                    }
                    else
                    if((cabeça.getY()==position.getY()-1 && cabeça.getX()==position.getX()+3)||
                    (cabeça.getY()==position.getY()-2 && cabeça.getX()==position.getX()+2)||
                    (cabeça.getY()==position.getY()-3 && cabeça.getX()==position.getX()+1)){
                        // - | - | - | - | o | - | - | - | -
                        // - | - | - | 0 | - | o | - | - | -
                        // - | - | 0 | - | - | - | o | - | -
                        // - | 0 | - | - | - | - | - | o | -
                        // o | - | - | - | Y | - | - | - | o
                        // - | o | - | - | - | - | - | o | -
                        // - | - | o | - | - | - | o | - | -
                        // - | - | - | o | - | o | - | - | -
                        // - | - | - | - | o | - | - | - | -

                        mov.setPriority(dir);
                        mov.setPriority(baixo);
                    
                    }
                    else
                    if((cabeça.getY()==position.getY()+1 && cabeça.getX()==position.getX()+3)||
                    (cabeça.getY()==position.getY()+2 && cabeça.getX()==position.getX()+2)||
                    (cabeça.getY()==position.getY()+3 && cabeça.getX()==position.getX()+1)){
                        // - | - | - | - | o | - | - | - | -
                        // - | - | - | o | - | o | - | - | -
                        // - | - | o | - | - | - | o | - | -
                        // - | o | - | - | - | - | - | o | -
                        // o | - | - | - | Y | - | - | - | o
                        // - | 0 | - | - | - | - | - | o | -
                        // - | - | 0 | - | - | - | o | - | -
                        // - | - | - | 0 | - | o | - | - | -
                        // - | - | - | - | o | - | - | - | -

                        mov.setPriority(dir);
                        mov.setPriority(cima);
                    
                    }
                    else
                    if((cabeça.getY()==position.getY()-1 && cabeça.getX()==position.getX()-3)||
                    (cabeça.getY()==position.getY()-2 && cabeça.getX()==position.getX()-2)||
                    (cabeça.getY()==position.getY()-3 && cabeça.getX()==position.getX()-1)){
                        // - | - | - | - | o | - | - | - | -
                        // - | - | - | o | - | 0 | - | - | -
                        // - | - | o | - | - | - | 0 | - | -
                        // - | o | - | - | - | - | - | 0 | -
                        // o | - | - | - | Y | - | - | - | o
                        // - | o | - | - | - | - | - | o | -
                        // - | - | o | - | - | - | o | - | -
                        // - | - | - | o | - | o | - | - | -
                        // - | - | - | - | o | - | - | - | -

                        mov.setPriority(esq);
                        mov.setPriority(baixo);
                    
                    }
                    else
                    if((cabeça.getY()==position.getY()-1 && cabeça.getX()==position.getX()+3)||
                    (cabeça.getY()==position.getY()-2 && cabeça.getX()==position.getX()+2)||
                    (cabeça.getY()==position.getY()-3 && cabeça.getX()==position.getX()+1)){
                        // - | - | - | - | o | - | - | - | -
                        // - | - | - | o | - | o | - | - | -
                        // - | - | o | - | - | - | o | - | -
                        // - | o | - | - | - | - | - | o | -
                        // o | - | - | - | Y | - | - | - | o
                        // - | o | - | - | - | - | - | 0 | -
                        // - | - | o | - | - | - | 0 | - | -
                        // - | - | - | o | - | 0 | - | - | -
                        // - | - | - | - | o | - | - | - | -

                        mov.setPriority(esq);
                        mov.setPriority(cima);
                    
                    }
                    else
                    if(cabeça.getY()==position.getY() && cabeça.getX()==position.getX()-5){
                        // - | - | - | - | - | x | - | - | - | - | -
                        // - | - | - | - | x | - | x | - | - | - | -
                        // - | - | - | x | - | - | - | x | - | - | -
                        // - | - | x | - | - | - | - | - | x | - | -
                        // - | x | - | - | - | - | - | - | - | x | -
                        // x | - | - | - | - | Y | - | - | - | - | O
                        // - | x | - | - | - | - | - | - | - | x | -
                        // - | - | x | - | - | - | - | - | x | - | -
                        // - | - | - | x | - | - | - | x | - | - | -
                        // - | - | - | - | x | - | x | - | - | - | -
                        // - | - | - | - | - | x | - | - | - | - | -

                        mov.setPriority(cima);
                        mov.setPriority(baixo);
                        mov.setPriority(esq);
                    
                    }
                    else
                    if(cabeça.getY()==position.getY() && cabeça.getX()==position.getX()+5){
                        // - | - | - | - | - | x | - | - | - | - | -
                        // - | - | - | - | x | - | x | - | - | - | -
                        // - | - | - | x | - | - | - | x | - | - | -
                        // - | - | x | - | - | - | - | - | x | - | -
                        // - | x | - | - | - | - | - | - | - | x | -
                        // O | - | - | - | - | Y | - | - | - | - | x
                        // - | x | - | - | - | - | - | - | - | x | -
                        // - | - | x | - | - | - | - | - | x | - | -
                        // - | - | - | x | - | - | - | x | - | - | -
                        // - | - | - | - | x | - | x | - | - | - | -
                        // - | - | - | - | - | x | - | - | - | - | -

                        mov.setPriority(cima);
                        mov.setPriority(baixo);
                        mov.setPriority(dir);
                    
                    }
                    else
                    if(cabeça.getY()==position.getY()-5 && cabeça.getX()==position.getX()){
                        // - | - | - | - | - | O | - | - | - | - | -
                        // - | - | - | - | x | - | x | - | - | - | -
                        // - | - | - | x | - | - | - | x | - | - | -
                        // - | - | x | - | - | - | - | - | x | - | -
                        // - | x | - | - | - | - | - | - | - | x | -
                        // x | - | - | - | - | Y | - | - | - | - | x
                        // - | x | - | - | - | - | - | - | - | x | -
                        // - | - | x | - | - | - | - | - | x | - | -
                        // - | - | - | x | - | - | - | x | - | - | -
                        // - | - | - | - | x | - | x | - | - | - | -
                        // - | - | - | - | - | x | - | - | - | - | -

                        mov.setPriority(dir);
                        mov.setPriority(baixo);
                        mov.setPriority(esq);
                    
                    }
                    else
                    if(cabeça.getY()==position.getY()+5 && cabeça.getX()==position.getX()){
                        // - | - | - | - | - | x | - | - | - | - | -
                        // - | - | - | - | x | - | x | - | - | - | -
                        // - | - | - | x | - | - | - | x | - | - | -
                        // - | - | x | - | - | - | - | - | x | - | -
                        // - | x | - | - | - | - | - | - | - | x | -
                        // x | - | - | - | - | Y | - | - | - | - | x
                        // - | x | - | - | - | - | - | - | - | x | -
                        // - | - | x | - | - | - | - | - | x | - | -
                        // - | - | - | x | - | - | - | x | - | - | -
                        // - | - | - | - | x | - | x | - | - | - | -
                        // - | - | - | - | - | O | - | - | - | - | -

                        mov.setPriority(cima);
                        mov.setPriority(dir);
                        mov.setPriority(esq);
                    
                    }
                    else
                    if((cabeça.getY()==position.getY()+4 && cabeça.getX()==position.getX()-1)||
                    (cabeça.getY()==position.getY()+3 && cabeça.getX()==position.getX()-2)||
                    (cabeça.getY()==position.getY()+2 && cabeça.getX()==position.getX()-3)||
                    (cabeça.getY()==position.getY()+1 && cabeça.getX()==position.getX()-4)){
                        // - | - | - | - | - | x | - | - | - | - | -
                        // - | - | - | - | x | - | x | - | - | - | -
                        // - | - | - | x | - | - | - | x | - | - | -
                        // - | - | x | - | - | - | - | - | x | - | -
                        // - | x | - | - | - | - | - | - | - | x | -
                        // x | - | - | - | - | Y | - | - | - | - | x
                        // - | x | - | - | - | - | - | - | - | O | -
                        // - | - | x | - | - | - | - | - | O | - | -
                        // - | - | - | x | - | - | - | O | - | - | -
                        // - | - | - | - | x | - | O | - | - | - | -
                        // - | - | - | - | - | x | - | - | - | - | -

                        mov.setPriority(cima);
                        mov.setPriority(esq);
                    }
                    else
                    if((cabeça.getY()==position.getY()+4 && cabeça.getX()==position.getX()+1)||
                    (cabeça.getY()==position.getY()+3 && cabeça.getX()==position.getX()+2)||
                    (cabeça.getY()==position.getY()+2 && cabeça.getX()==position.getX()+3)||
                    (cabeça.getY()==position.getY()+1 && cabeça.getX()==position.getX()+4)){
                        // - | - | - | - | - | x | - | - | - | - | -
                        // - | - | - | - | x | - | x | - | - | - | -
                        // - | - | - | x | - | - | - | x | - | - | -
                        // - | - | x | - | - | - | - | - | x | - | -
                        // - | x | - | - | - | - | - | - | - | x | -
                        // x | - | - | - | - | Y | - | - | - | - | x
                        // - | O | - | - | - | - | - | - | - | x | -
                        // - | - | O | - | - | - | - | - | x | - | -
                        // - | - | - | O | - | - | - | x | - | - | -
                        // - | - | - | - | O | - | x | - | - | - | -
                        // - | - | - | - | - | x | - | - | - | - | -

                        mov.setPriority(cima);
                        mov.setPriority(dir);
                    }
                    else
                    if((cabeça.getY()==position.getY()-4 && cabeça.getX()==position.getX()-1)||
                    (cabeça.getY()==position.getY()-3 && cabeça.getX()==position.getX()-2)||
                    (cabeça.getY()==position.getY()-2 && cabeça.getX()==position.getX()-3)||
                    (cabeça.getY()==position.getY()-1 && cabeça.getX()==position.getX()-4)){
                        // - | - | - | - | - | x | - | - | - | - | -
                        // - | - | - | - | x | - | O | - | - | - | -
                        // - | - | - | x | - | - | - | O | - | - | -
                        // - | - | x | - | - | - | - | - | O | - | -
                        // - | x | - | - | - | - | - | - | - | O | -
                        // x | - | - | - | - | Y | - | - | - | - | x
                        // - | x | - | - | - | - | - | - | - | x | -
                        // - | - | x | - | - | - | - | - | x | - | -
                        // - | - | - | x | - | - | - | x | - | - | -
                        // - | - | - | - | x | - | x | - | - | - | -
                        // - | - | - | - | - | x | - | - | - | - | -

                        mov.setPriority(baixo);
                        mov.setPriority(esq);
                    }
                    else
                    if((cabeça.getY()==position.getY()-4 && cabeça.getX()==position.getX()+1)||
                    (cabeça.getY()==position.getY()-3 && cabeça.getX()==position.getX()+2)||
                    (cabeça.getY()==position.getY()-2 && cabeça.getX()==position.getX()+3)||
                    (cabeça.getY()==position.getY()-1 && cabeça.getX()==position.getX()+4)){
                        // - | - | - | - | - | x | - | - | - | - | -
                        // - | - | - | - | O | - | x | - | - | - | -
                        // - | - | - | O | - | - | - | x | - | - | -
                        // - | - | O | - | - | - | - | - | x | - | -
                        // - | O | - | - | - | - | - | - | - | x | -
                        // x | - | - | - | - | Y | - | - | - | - | x
                        // - | x | - | - | - | - | - | - | - | x | -
                        // - | - | x | - | - | - | - | - | x | - | -
                        // - | - | - | x | - | - | - | x | - | - | -
                        // - | - | - | - | x | - | x | - | - | - | -
                        // - | - | - | - | - | x | - | - | - | - | -

                        mov.setPriority(baixo);
                        mov.setPriority(dir);
                    }
                    else
                    if(cabeça.getY()==position.getY()-1 && cabeça.getX()==position.getX()){
                        mov.setUp(-1);
                    }else
                    if(cabeça.getY()==position.getY()+1 && cabeça.getX()==position.getX()){
                        mov.setDown(-1);
                    }else
                    if(cabeça.getX()==position.getX()-1 && cabeça.getY()==position.getY()){
                        mov.setRight(-1);
                    }else
                    if(cabeça.getX()==position.getX()+1 && cabeça.getY()==position.getY()){
                        mov.setLeft(-1);
                    }
                }
            }
            // path to fruit
            if(you.getHealth()<50){
                if(!board.getFood().isEmpty()){
                    Coordinate target = null;
                    int distance = Integer.MAX_VALUE;
                    for (Coordinate c:board.getFood()) {
                        int newDistance = Math.abs(c.getX()-you.getHead().getX())+Math.abs(c.getX()-you.getHead().getX());
                        if(newDistance < distance){
                            distance = newDistance;
                            target = c;
                        }
                    }
                    // mecanica para ditar qual movimento e prioridade
                    if(cabeça.getY()<target.getY()){
                        mov.setPriority(cima);
                        mov.setPriority(cima);
                        mov.setPriority(cima);
                    }
                }

                    if(cabeça.getY()>target.getY()){
                        mov.setPriority(baixo);
                        mov.setPriority(baixo);
                        mov.setPriority(baixo);
                    }

                    if(cabeça.getX()<target.getX()){
                        mov.setPriority(dir);
                        mov.setPriority(dir);
                        mov.setPriority(dir);
                    }

                    if(cabeça.getX()>target.getX()){
                        mov.setPriority(esq);
                        mov.setPriority(esq);
                        mov.setPriority(esq);
                    }
                }
            }
            else{
                if(!board.getFood().isEmpty()){
                    Coordinate target = null;
                    int distance = Integer.MAX_VALUE;
                    for (Coordinate c:board.getFood()) {
                        int newDistance = Math.abs(c.getX()-you.getHead().getX())+Math.abs(c.getX()-you.getHead().getX());
                        if(newDistance < distance){
                            distance = newDistance;
                            target = c;
                        }
                    }
                    // mecanica para ditar qual movimento e prioridade
                    if(cabeça.getY()<target.getY()){
                        mov.setPriority(cima);
                    }

                    if(cabeça.getY()>target.getY()){
                        mov.setPriority(baixo);
                    }

                    if(cabeça.getX()<target.getX()){
                        mov.setPriority(dir);
                    }

                    if(cabeça.getX()>target.getX()){
                        mov.setPriority(esq);
                    }
                }
            }
        int[] decision = mov.finalPriority();
        for(int i:decision){
            if(i != -1){
                switch (i) {
                    case _up:
                        move.put("move", "up"); isDecided = true; break;
                    case _down:
                        move.put("move", "down"); isDecided = true; break;
                    case _right:
                        move.put("move", "right"); isDecided = true; break;
                    case _left:
                        move.put("move", "left"); isDecided = true; break;
                }//switch end
                // break for if is avalible
                break;}//if end
        }//for end
        if(!isDecided){move.put("move", "up");} // error decision

        return move;
    }

    /**
     * Chamado no final de cada jogo. Não precisa retornar nada.
     */
    private void handleEnd(APIGatewayProxyRequestEvent request, Context context) {
        // Você pode analisar a requisição para saber se venceu ou perdeu.
        context.getLogger().log("Game Ended!");
    }


    private Map<Coordinate,Coordinate> Astar(Coordinate start,Coordinate goal){
        PriorityQueue<Node> fila = new PriorityQueue<>((Node o1, Node o2) -> o1.getPriority() - o2.getPriority());

        fila.add(new Node(start,0));
        Map<Coordinate,Coordinate> deOndeveio = new HashMap<>();
        Map<Coordinate,Integer> preço = new HashMap<>();
        deOndeveio.putIfAbsent(start,null);
        preço.putIfAbsent(start,0);
        
        while(!fila.isEmpty()){
            Coordinate agora = fila.poll().getCoord();
            if(agora.equals(goal)){
                break;
            }
            for (Coordinate c : possibilities(agora)) {
                int value = boardMap[c.getX()][c.getY()];
                boolean podePisar = (value != _wall);
                int novoCusto = preço.get((agora))+dangerMap[c.getX()][c.getY()];

                if(value == _tail){
                    Coordinate tail = you.getBody().get(you.getLength() - 1);
                    boolean isOwnTail = tail.equals(c);
                    boolean notGrowing = you.getHealth() < 100;
                    podePisar = isOwnTail && notGrowing;
                }
                if((!deOndeveio.containsKey(c)|| novoCusto<preço.get(c) ) && (podePisar)){
                    preço.putIfAbsent(c, novoCusto);
                    int prioridade = novoCusto + calculateDistance(c, goal);
                    fila.add(new Node(c, prioridade));
                    deOndeveio.putIfAbsent(c,agora);
                }
            }
        }
        return deOndeveio;
    }
    /**
     * Sets the board to the defaul state
     */
    private void clearBoard(){
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < heigth; j++) {
                boardMap[i][j] = _empty;
                dangerMap[i][j] = d_stantardWeigth;}
        }
    }

    /**
     * set each boards point it's value
     */
    private void setBoards(){

        // set board Map
        //List<Coordinate> trabCoordinate;
        List<Snake> trabSnake;

            /*
             * //fruits
            trabCoordinate = board.getFood();
            if(!trabCoordinate.isEmpty()){
                for (Coordinate food : trabCoordinate) {
                    boardMap[food.getX()][food.getY()] = _fruit;}}//end if
             */
            
            // snakes
            trabSnake = board.getSnakes();
            for (Snake s : trabSnake) {
                // head is the last set
                //sCabeca = new HeadEntity(_head, s.getHead(), s.getLength());
                sCabeca = new HeadEntity(_head, s.getHead(),s.getLength());
                sCauda = new HeadEntity(_tail, s.getBody().get(sCabeca.getLength()-1));
                boardMap[sCauda.getX()][sCauda.getY()] = _tail;

                sCabeca.setNode(sCauda);
                sCauda.setNode(sCabeca);

                headTail.putIfAbsent(sCabeca.toString(), sCabeca);
                headTail.putIfAbsent(sCauda.toString(),sCauda);
                

                for (int i = 1; i < sCabeca.getLength()-1; i++) {
                        boardMap[s.getBody().get(i).getX()][s.getBody().get(i).getY()] = _wall;
                        dangerMap[s.getBody().get(i).getX()][s.getBody().get(i).getY()] = d_unAvalible;
                    }
                
                if(!s.equals(you)){
                    boardMap[sCabeca.getX()][sCabeca.getY()] = _head;
                    suroundingDanger(s);
                }

            }

    }

    /**
     * Calculates the width and length from current location to analysed spot
     */
    private int calculateDistance(Coordinate current, Coordinate analysed){
        return (int)Math.abs(current.getX()-analysed.getX())+Math.abs(current.getY()-analysed.getY());
    }
    /**
     * fiels dangerMap with the level of danger sorounding an enemy head
     */
    private void suroundingDanger(Snake enemyHead){
        int dangerLevel = dangerLevel(enemyHead);
        int iX = enemyHead.getHead().getX();
        int iY = enemyHead.getHead().getY();
        int[][] possibilities_v1 = new int[][]{
            {0, 1},
            {0, -1},
            {1, 0},
            {-1, 0},
            {1, 1},
            {1, -1},
            {-1, 1},
            {-1, -1},
            {0, 2},
            {0, -2},
            {2, 0},
            {-2, 0}
        };

        for (int[] i: possibilities_v1) {
            int newIX = i[0]+iX, newIY = i[1]+iY;
            Coordinate aux = new Coordinate(newIX,newIY);
            if(isWalkable(aux)){
                dangerMap[newIX][newIY]+=dangerLevel;
            }
        }
    }
    /**
     * @return the level of treat of the snake
     */
    private int dangerLevel(Snake enemy){
        int diference = enemy.getLength() - you.getLength();
        if(diference<=d_weak) return d_weak;
        else if(diference==d_closeToEqual) return d_closeToEqual;
        else if(diference==d_equal) return d_equal;
        else if(diference<=d_dangerous) return d_dangerous;
        else return d_avoid;
    }

    /**
     * @return the level of treat of the snake
     */
    private int dangerLevel(int length){
        int diference = length - you.getLength();
        if(diference<=d_weak) return d_weak;
        else if(diference==d_closeToEqual) return d_closeToEqual;
        else if(diference==d_equal) return d_equal;
        else if(diference<=d_dangerous) return d_dangerous;
        else return d_avoid;
    }
    /**
     * Funtion that will travel across the board 
     * collecting information based of start position
     * (floodFill but i wanted to call it Radar)
     * @return spaces avalible from the start direction
     */
    private int radar(Coordinate direction){
        // logic

        Boolean[][] visited = new Boolean[width][heigth];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < heigth; j++) {
                visited[i][j] = false;
            }
        }

        int count = 0;
        Queue<Coordinate> sequence = new LinkedList<>();
        
        sequence.add(direction);
        visited[direction.getX()][direction.getY()] = true;

        while(!sequence.isEmpty()){

            Coordinate analysed = sequence.poll();
            count++;

            for (Coordinate inspect : RadarPossibilities(analysed)) {
                if(!visited[inspect.getX()][inspect.getY()]){
                    sequence.add(inspect);
                    visited[inspect.getX()][inspect.getY()] = true;
                }
            }
        }
        return count;
    }

    private List<Coordinate> RadarPossibilities(Coordinate from){
        int[][] directions = new int[][]{
        {0, 1}, {0, -1}, {1, 0}, {-1, 0}
        };

        List<Coordinate> result = new LinkedList<>();
    for (int[] d : directions) {
        int x = from.getX() + d[0];
        int y = from.getY() + d[1];
        Coordinate c = new Coordinate(x, y);

        if (isInBounds(c)) {
            int value = boardMap[x][y];

            if (value == _tail) {
                Coordinate tail = you.getBody().get(you.getLength() - 1);
                if (tail.equals(c)) {
                    result.add(c);
                }
            } else if (value != _wall) {
                result.add(c);
            }
        }
    }
    return result;
    }
    /**
     * caculates possible paths
     * @param analyse
     * @return
     */
    private List<Coordinate> possibilities(Coordinate analyse){
        int[][] possibilities = new int[][]{
            {0, 1},
            {0, -1},
            {1, 0},
            {-1, 0}
        };

        List<Coordinate> awnser = new LinkedList<>();
        for (int[] i: possibilities) {
            Coordinate aux = new Coordinate(i[0]+analyse.getX(),i[1]+analyse.getY());
            if(isWalkable(aux)){
                awnser.add(aux);
            }
        }
        return awnser;
    }
    /**
     * checks if you can walk on that path
     */
    private boolean isWalkable(Coordinate analyse){ 
        if(analyse.getX()>=0 && analyse.getX()<width
        && analyse.getY()>=0 && analyse.getY()<heigth){
            int value = boardMap[analyse.getX()][analyse.getY()];
            if (value == _tail) {
            if (you.getHealth() < 100) return true; 
            else return false; 
            } else return value != _wall;
        } else return false;
    }
    /**
     * checks if the new coordinate goes out of bounds
     * @param analyse
     * @return
     */
    private boolean isInBounds(Coordinate analyse){
        return analyse.getX()>=0 && analyse.getX()<width && analyse.getY()>=0 && analyse.getY()<heigth;
    }
    /** 
     * makes the final decision
     * by ordering the pssibilities in priority order
     *  */
    public void finalPriority(){
        for (int i = 0; i < 4; i++) {
            int index = 0;
            for (int j=0;j<4;j++) {
                if(priorityMove[j]>priorityMove[index]){
                    index = j;
                }
            }
            if (isAnOption[index]) {
                switch (index) {
                case _up:
                    finalDecision[i] = _up;
                    break;
                case _down:
                    finalDecision[i] = _down;
                    break;
                case _right:
                    finalDecision[i] = _right;
                    break;
                case _left:
                    finalDecision[i] = _left;
                    break;
                }    
            } else {
                finalDecision[i] = _notAvalible;
            }
            
            priorityMove[index] = -1;
        }
    }

    /**
     * used to visually if the board map was correctly field 
     * or if radar checked the whole board map
     * for the parameters:
     * @param when = before
     * @param when = after
     */
    private void printBoardMap(String when){
        System.out.println("\nBoard "+when+" radar:");
        for (int j = heigthLimit; j >=0 ; j--) {
            for (int i = 0; i < width; i++) {switch (boardMap[i][j]) {
                case _wall:
                    System.out.print("| 🏢 ");
                    break;
                case _fruit:
                    System.out.print("| 🍎 ");
                    break;
                case _empty:
                    System.out.print("|    ");
                    break;
                case _head:
                    System.out.print("| 😀 ");
                    break;
                case _tail:
                    System.out.print("| 🐍 ");
                    break;
                case _checked:
                    System.out.print("| ✨ ");
                    break;
            }}
            System.out.print("\n"+"-".repeat(5*11)+"\n");
        }
    }

    }