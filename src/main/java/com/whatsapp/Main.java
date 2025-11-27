package com.whatsapp;

import com.whatsapp.model.Usuario;
import com.whatsapp.ui.ClientView;
import com.whatsapp.ui.LoginResult;
import com.whatsapp.ui.LoginView;
import com.whatsapp.ui.ServerView;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    private Stage primaryStage;
    private Usuario currentUser;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        showLoginView();
    }

    private void showLoginView() {
        try {
            LoginView loginView = new LoginView();
            Scene scene = new Scene(loginView.getRoot(), 400, 500);
            primaryStage.setTitle("RoomWave - Login");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.setOnCloseRequest(e -> {
                Platform.exit();
                System.exit(0);
            });
            primaryStage.show();

            // Configurar callback para cuando el usuario haga login exitoso
            loginView.setOnLoginSuccess(result -> {
                currentUser = result.getUsuario();
                if (result.getMode() == LoginResult.Mode.SERVER) {
                    Platform.runLater(this::showServerView);
                } else {
                    Platform.runLater(() -> showClientView(result));
                }
            });
        } catch (Exception e) {
            com.whatsapp.service.LogService.getInstance()
                .logError("Error mostrando LoginView: " + e.getMessage(), "Main", null, null);
        }
    }

    private void showServerView() {
        ServerView serverView = new ServerView(currentUser);
        Scene scene = new Scene(serverView, 800, 600);
        primaryStage.setTitle("RoomWave - Servidor - " + currentUser.getUsername());
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.show();
    }

    private void showClientView(LoginResult result) {
        ClientView clientView = new ClientView(currentUser, result.getNetworkFacade(), result.getServerHost(), result.getServerPort());
        Scene scene = new Scene(clientView, 800, 600);
        primaryStage.setTitle("RoomWave - Cliente - " + currentUser.getUsername());
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

