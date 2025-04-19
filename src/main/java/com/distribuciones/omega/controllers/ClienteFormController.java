package com.distribuciones.omega.controllers;

import com.distribuciones.omega.model.Cliente;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ClienteFormController {

    @FXML private TextField tfId;
    @FXML private TextField tfNombre;
    @FXML private TextField tfEmail;
    @FXML private TextField tfTelefono;
    @FXML private TextField tfDireccion;

    private Stage dialogStage;
    private Cliente cliente;
    private boolean okClicked = false;

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public void setCliente(Cliente c, boolean isEdit) {
        this.cliente = c;
        if (isEdit) {
            tfId.setText(c.getId());
            tfId.setDisable(true);
            tfNombre.setText(c.getNombre());
            tfEmail.setText(c.getEmailString());
            tfTelefono.setText(c.getTelefono());
            tfDireccion.setText(c.getDireccion());
        }
    }

    @FXML
    private void onSave() {
        cliente.setId(tfId.getText());
        cliente.setNombre(tfNombre.getText());
        cliente.setEmailString(tfEmail.getText());
        cliente.setTelefono(tfTelefono.getText());
        cliente.setDireccion(tfDireccion.getText());
        okClicked = true;
        dialogStage.close();
    }

    @FXML
    private void onCancel() {
        dialogStage.close();
    }

    public boolean isOkClicked() {
        return okClicked;
    }

    public Cliente getCliente() {
        return cliente;
    }
}