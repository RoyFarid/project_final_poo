package com.whatsapp;

import com.whatsapp.model.Usuario;
import com.whatsapp.ui.ClientView;
import com.whatsapp.ui.LoginView;
import com.whatsapp.ui.ServerView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
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
            primaryStage.setTitle("WhatsApp Clone - Login");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.setOnCloseRequest(e -> {
                Platform.exit();
                System.exit(0);
            });
            primaryStage.show();

            // Configurar callback para cuando el usuario haga login exitoso
            loginView.setOnLoginSuccess(() -> {
                currentUser = loginView.getCurrentUser();
                Platform.runLater(() -> showModeSelection());
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showModeSelection() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Seleccionar Modo");
        alert.setHeaderText("¿Cómo desea conectarse?");
        alert.setContentText("Seleccione su modo de operación:");

        ButtonType serverButton = new ButtonType("Servidor");
        ButtonType clientButton = new ButtonType("Cliente");
        ButtonType cancelButton = new ButtonType("Cancelar", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(serverButton, clientButton, cancelButton);

        alert.showAndWait().ifPresent(buttonType -> {
            if (buttonType == serverButton) {
                showServerView();
            } else if (buttonType == clientButton) {
                showClientView();
            } else {
                showLoginView();
            }
        });
    }

    private void showServerView() {
        ServerView serverView = new ServerView(currentUser);
        Scene scene = new Scene(serverView, 800, 600);
        primaryStage.setTitle("WhatsApp Clone - Servidor - " + currentUser.getUsername());
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.show();
    }

    private void showClientView() {
        ClientView clientView = new ClientView(currentUser);
        Scene scene = new Scene(clientView, 800, 600);
        primaryStage.setTitle("WhatsApp Clone - Cliente - " + currentUser.getUsername());
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

