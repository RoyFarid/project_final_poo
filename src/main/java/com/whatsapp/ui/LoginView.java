package com.whatsapp.ui;

import com.whatsapp.model.Usuario;
import com.whatsapp.service.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.Optional;

public class LoginView {
    private final VBox root;
    private final AuthService authService;
    private Usuario currentUser;
    private Runnable onLoginSuccessCallback;

    public LoginView() {
        this.authService = new AuthService();
        this.root = createLoginView();
    }

    public void setOnLoginSuccess(Runnable callback) {
        this.onLoginSuccessCallback = callback;
    }

    private VBox createLoginView() {
        VBox vbox = new VBox(20);
        vbox.setPadding(new Insets(40));
        vbox.setAlignment(Pos.CENTER);
        vbox.setStyle("-fx-background-color: #f0f0f0;");

        // Título
        Text title = new Text("WhatsApp Clone");
        title.setFont(Font.font(28));
        title.setStyle("-fx-fill: #25D366;");

        // Campos de entrada
        TextField usernameField = new TextField();
        usernameField.setPromptText("Usuario");
        usernameField.setPrefWidth(250);
        usernameField.setPrefHeight(35);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Contraseña");
        passwordField.setPrefWidth(250);
        passwordField.setPrefHeight(35);

        // Botones
        Button loginButton = new Button("Iniciar Sesión");
        loginButton.setPrefWidth(250);
        loginButton.setPrefHeight(40);
        loginButton.setStyle("-fx-background-color: #25D366; -fx-text-fill: white; -fx-font-size: 14px;");
        
        Button registerButton = new Button("Registrarse");
        registerButton.setPrefWidth(250);
        registerButton.setPrefHeight(40);
        registerButton.setStyle("-fx-background-color: #128C7E; -fx-text-fill: white; -fx-font-size: 14px;");

        // Label de estado
        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: red;");

        // Acciones
        loginButton.setOnAction(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();
            
            if (username.isEmpty() || password.isEmpty()) {
                statusLabel.setText("Por favor complete todos los campos");
                return;
            }

            Optional<Usuario> usuario = authService.autenticar(username, password);
            if (usuario.isPresent()) {
                currentUser = usuario.get();
                statusLabel.setText("Login exitoso!");
                statusLabel.setStyle("-fx-text-fill: green;");
                // Notificar al controlador principal
                if (onLoginSuccessCallback != null) {
                    onLoginSuccessCallback.run();
                }
            } else {
                statusLabel.setText("Usuario o contraseña incorrectos");
                statusLabel.setStyle("-fx-text-fill: red;");
            }
        });

<<<<<<< Updated upstream
        registerButton.setOnAction(e -> {
            showRegisterDialog();
=======
    private void showRemoteRegisterDialog() {
        String host = serverHostField.getText().trim();
        String portText = serverPortField.getText().trim();
        if (host.isEmpty() || portText.isEmpty()) {
            showError("Error", "Ingrese host y puerto del servidor antes de registrarse.");
            return;
        }
        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            showError("Error", "Puerto inválido");
            return;
        }

        Dialog<Usuario> dialog = buildRegisterDialog("Registro (Cliente)");
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                TextField usernameField = (TextField) dialog.getDialogPane().lookup("#reg_username");
                PasswordField passwordField = (PasswordField) dialog.getDialogPane().lookup("#reg_password");
                TextField emailField = (TextField) dialog.getDialogPane().lookup("#reg_email");
                try {
                    NetworkFacade tempFacade = new NetworkFacade();
                    try {
                        tempFacade.connectToServer(host, port);
                        String connectionId = tempFacade.getPrimaryConnectionId();
                        if (connectionId == null) {
                            showError("Error", "No se pudo conectar al servidor.");
                            return null;
                        }
                        try (RemoteAuthClient remoteAuthClient = new RemoteAuthClient()) {
                            ControlService.OperationResultPayload result = remoteAuthClient.register(
                                connectionId,
                                usernameField.getText(),
                                passwordField.getText(),
                                emailField.getText()
                            );
                            if (result.isSuccess()) {
                                showInfo("Registro exitoso", "Usuario creado en el servidor.");
                            } else {
                                showError("Error de registro", result.getMessage());
                            }
                        }
                    } finally {
                        tempFacade.disconnect();
                        tempFacade.shutdown();
                    }
                } catch (Exception e) {
                    showError("Error de registro", e.getMessage());
                }
            }
            return null;
>>>>>>> Stashed changes
        });

        vbox.getChildren().addAll(title, usernameField, passwordField, loginButton, registerButton, statusLabel);
        return vbox;
    }

    private void showRegisterDialog() {
        Dialog<Usuario> dialog = new Dialog<>();
        dialog.setTitle("Registro de Usuario");
        dialog.setHeaderText("Crear nueva cuenta");

        ButtonType registerButtonType = new ButtonType("Registrarse", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(registerButtonType, ButtonType.CANCEL);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Usuario");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Contraseña");
        TextField emailField = new TextField();
        emailField.setPromptText("Email");

        vbox.getChildren().addAll(
            new Label("Usuario:"), usernameField,
            new Label("Contraseña:"), passwordField,
            new Label("Email:"), emailField
        );

        dialog.getDialogPane().setContent(vbox);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == registerButtonType) {
                try {
                    return authService.registrar(
                        usernameField.getText(),
                        passwordField.getText(),
                        emailField.getText()
                    );
                } catch (Exception e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error de Registro");
                    alert.setHeaderText(null);
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                    return null;
                }
            }
            return null;
        });

        Optional<Usuario> result = dialog.showAndWait();
        if (result.isPresent()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Registro Exitoso");
            alert.setHeaderText(null);
            alert.setContentText("Usuario registrado correctamente. Ahora puede iniciar sesión.");
            alert.showAndWait();
        }
    }


    public VBox getRoot() {
        return root;
    }

    public Usuario getCurrentUser() {
        return currentUser;
    }
<<<<<<< Updated upstream
=======

    private void closeFacade(NetworkFacade facade) {
        if (facade == null) {
            return;
        }
        facade.disconnect();
        facade.shutdown();
    }
>>>>>>> Stashed changes
}

