package com.whatsapp.ui;

import com.whatsapp.model.Usuario;
import com.whatsapp.service.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class LoginView {
    private final VBox root;
    private AuthService authService;
    private Usuario currentUser;
    private Consumer<LoginResult> onLoginSuccessCallback;
    private final ToggleGroup modeToggle;
    private final TextField serverHostField;
    private final TextField serverPortField;
    private final Label statusLabel;
    private NetworkFacade remoteNetworkFacade;

    public LoginView() {
        this.modeToggle = new ToggleGroup();
        this.serverHostField = new TextField("localhost");
        this.serverPortField = new TextField("8080");
        this.statusLabel = new Label();
        this.root = createLoginView();
        ServerRuntime.markAsServerProcess();
    }

    public void setOnLoginSuccess(Consumer<LoginResult> callback) {
        this.onLoginSuccessCallback = callback;
    }

    private VBox createLoginView() {
        VBox vbox = new VBox(20);
        vbox.setPadding(new Insets(40));
        vbox.setAlignment(Pos.TOP_CENTER);
        vbox.setStyle("-fx-background-color: #f0f0f0;");

        Text title = new Text("WhatsApp Clone");
        title.setFont(Font.font(28));
        title.setStyle("-fx-fill: #25D366;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Usuario");
        usernameField.setPrefWidth(260);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Contraseña");
        passwordField.setPrefWidth(260);

        RadioButton serverMode = new RadioButton("Iniciar como Servidor");
        serverMode.setToggleGroup(modeToggle);
        serverMode.setSelected(true);
        serverMode.setUserData(LoginResult.Mode.SERVER);

        RadioButton clientMode = new RadioButton("Cliente (conectar a servidor)");
        clientMode.setToggleGroup(modeToggle);
        clientMode.setUserData(LoginResult.Mode.CLIENT);

        VBox remoteConfigBox = new VBox(8);
        remoteConfigBox.setPadding(new Insets(10));
        remoteConfigBox.setStyle("-fx-background-color: #ffffff; -fx-border-color: #cccccc;");
        remoteConfigBox.getChildren().addAll(
            new Label("Host del servidor:"),
            serverHostField,
            new Label("Puerto:"),
            serverPortField
        );
        remoteConfigBox.setVisible(false);
        remoteConfigBox.setManaged(false);

        modeToggle.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            boolean isClient = getSelectedMode() == LoginResult.Mode.CLIENT;
            remoteConfigBox.setVisible(isClient);
            remoteConfigBox.setManaged(isClient);
            if (isClient) {
                ServerRuntime.markAsClientProcess();
            } else {
                ServerRuntime.markAsServerProcess();
            }
        });

        Button loginButton = new Button("Iniciar Sesión");
        loginButton.setPrefWidth(260);
        loginButton.setStyle("-fx-background-color: #25D366; -fx-text-fill: white; -fx-font-size: 14px;");
        loginButton.setOnAction(e -> handleLogin(usernameField.getText().trim(), passwordField.getText().trim()));

        Button registerButton = new Button("Registrarse");
        registerButton.setPrefWidth(260);
        registerButton.setStyle("-fx-background-color: #128C7E; -fx-text-fill: white; -fx-font-size: 14px;");
        registerButton.setOnAction(e -> handleRegister());

        statusLabel.setStyle("-fx-text-fill: red;");
        statusLabel.setWrapText(true);

        VBox modeBox = new VBox(5, new Label("Selecciona el modo:"), serverMode, clientMode, remoteConfigBox);

        vbox.getChildren().addAll(title, modeBox, usernameField, passwordField, loginButton, registerButton, statusLabel);
        return vbox;
    }

    private void handleLogin(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Por favor complete todos los campos");
            return;
        }

        if (getSelectedMode() == LoginResult.Mode.SERVER) {
            handleServerLogin(username, password);
        } else {
            handleClientLogin(username, password);
        }
    }

    private void handleServerLogin(String username, String password) {
        try {
            ServerRuntime.markAsServerProcess();
            // Inicializar DatabaseManager con el username del servidor
            com.whatsapp.database.DatabaseManager.getInstance(username);
            if (authService == null) {
                authService = new AuthService(username);
            }
            Optional<Usuario> usuario = authService.autenticar(username, password);
            if (usuario.isPresent()) {
                currentUser = usuario.get();
                statusLabel.setText("Login exitoso (modo servidor)");
                statusLabel.setStyle("-fx-text-fill: green;");
                notifyLoginSuccess(new LoginResult(LoginResult.Mode.SERVER, currentUser, null, null, 0));
            } else {
                statusLabel.setText("Usuario o contraseña incorrectos");
                statusLabel.setStyle("-fx-text-fill: red;");
            }
        } catch (Exception e) {
            statusLabel.setText("Error autenticando: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    private void handleClientLogin(String username, String password) {
        String host = serverHostField.getText().trim();
        String portText = serverPortField.getText().trim();
        if (host.isEmpty() || portText.isEmpty()) {
            statusLabel.setText("Ingrese host y puerto del servidor");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            statusLabel.setText("Puerto inválido");
            return;
        }

        try {
            ServerRuntime.markAsClientProcess();
            closeFacade(remoteNetworkFacade);
            remoteNetworkFacade = new NetworkFacade();
            remoteNetworkFacade.connectToServer(host, port);
            String serverConnectionId = remoteNetworkFacade.getPrimaryConnectionId();
            if (serverConnectionId == null) {
                throw new IOException("No se obtuvo conexión con el servidor");
            }

            try (RemoteAuthClient remoteAuthClient = new RemoteAuthClient()) {
                ControlService.OperationResultPayload result = remoteAuthClient.authenticate(serverConnectionId, username, password);
                if (result.isSuccess()) {
                    currentUser = buildUsuarioFromPayload(result);
                    statusLabel.setText("Login exitoso (modo cliente)");
                    statusLabel.setStyle("-fx-text-fill: green;");
                    notifyLoginSuccess(new LoginResult(LoginResult.Mode.CLIENT, currentUser, remoteNetworkFacade, host, port));
                } else {
                    statusLabel.setText(result.getMessage());
                    statusLabel.setStyle("-fx-text-fill: red;");
                    closeFacade(remoteNetworkFacade);
                    remoteNetworkFacade = null;
                }
            }
        } catch (IOException | TimeoutException | InterruptedException e) {
            statusLabel.setText("Error al conectarse: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
            if (remoteNetworkFacade != null) {
                closeFacade(remoteNetworkFacade);
                remoteNetworkFacade = null;
            }
        }
    }

    private void handleRegister() {
        if (getSelectedMode() == LoginResult.Mode.SERVER) {
            showLocalRegisterDialog();
        } else {
            showRemoteRegisterDialog();
        }
    }

    private void showLocalRegisterDialog() {
        ServerRuntime.markAsServerProcess();
        // Necesitamos obtener el username del servidor actual
        // Por ahora usamos el username del campo de texto si está disponible
        // En un caso real, esto debería venir del contexto del servidor
        if (authService == null) {
            // Si no hay servidor iniciado, usar AuthService sin username específico
            authService = new AuthService();
        }

        Dialog<Usuario> dialog = buildRegisterDialog("Registro (Servidor)");
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                TextField usernameField = (TextField) dialog.getDialogPane().lookup("#reg_username");
                PasswordField passwordField = (PasswordField) dialog.getDialogPane().lookup("#reg_password");
                TextField emailField = (TextField) dialog.getDialogPane().lookup("#reg_email");
                try {
                    return authService.registrar(
                        usernameField.getText(),
                        passwordField.getText(),
                        emailField.getText()
                    );
                } catch (Exception e) {
                    showError("Error de registro", e.getMessage());
                }
            }
            return null;
        });
        dialog.showAndWait().ifPresent(user -> showInfo("Registro exitoso", "Usuario registrado correctamente."));
    }

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
                        tempFacade.disconnectClients();
                        tempFacade.shutdown();
                    }
                } catch (Exception e) {
                    showError("Error de registro", e.getMessage());
                }
            }
            return null;
        });
        dialog.showAndWait();
    }

    private Dialog<Usuario> buildRegisterDialog(String title) {
        Dialog<Usuario> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));

        TextField usernameField = new TextField();
        usernameField.setId("reg_username");
        usernameField.setPromptText("Usuario");

        PasswordField passwordField = new PasswordField();
        passwordField.setId("reg_password");
        passwordField.setPromptText("Contraseña");

        TextField emailField = new TextField();
        emailField.setId("reg_email");
        emailField.setPromptText("Email");

        vbox.getChildren().addAll(
            new Label("Usuario:"), usernameField,
            new Label("Contraseña:"), passwordField,
            new Label("Email:"), emailField
        );

        dialog.getDialogPane().setContent(vbox);
        return dialog;
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private LoginResult.Mode getSelectedMode() {
        Toggle selected = modeToggle.getSelectedToggle();
        if (selected != null && selected.getUserData() instanceof LoginResult.Mode mode) {
            return mode;
        }
        return LoginResult.Mode.SERVER;
    }

    private void notifyLoginSuccess(LoginResult result) {
        if (onLoginSuccessCallback != null) {
            onLoginSuccessCallback.accept(result);
        }
    }

    private Usuario buildUsuarioFromPayload(ControlService.OperationResultPayload payload) {
        Usuario usuario = new Usuario();
        usuario.setId(payload.getUserId());
        usuario.setUsername(payload.getUsername());
        usuario.setEmail(payload.getEmail());
        usuario.setEstado(Usuario.EstadoUsuario.ACTIVO);
        return usuario;
    }

    public VBox getRoot() {
        return root;
    }

    public Usuario getCurrentUser() {
        return currentUser;
    }

    private void closeFacade(NetworkFacade facade) {
        if (facade == null) {
            return;
        }
        try {
            facade.disconnectClients();
        } catch (Exception ignored) {
        }
        facade.shutdown();
    }
}

