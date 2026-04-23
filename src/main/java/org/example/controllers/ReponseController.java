package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.entities.Reponsequestionnaire;
import org.example.services.ReponseQuestionnaireServices;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class ReponseController implements Initializable {

    @FXML private TableView<Reponsequestionnaire> tableReponse;
    @FXML private TableColumn<Reponsequestionnaire, Integer> colId, colQId, colUserId, colDuree;
    @FXML private TableColumn<Reponsequestionnaire, Double>  colScore;
    @FXML private TableColumn<Reponsequestionnaire, String>  colNiveau, colPsy, colDate;

    private final ReponseQuestionnaireServices service = new ReponseQuestionnaireServices();
    private final ObservableList<Reponsequestionnaire> data = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(new PropertyValueFactory<>("reponseQuestionnaireId"));
        colScore.setCellValueFactory(new PropertyValueFactory<>("scoreTotale"));
        colNiveau.setCellValueFactory(new PropertyValueFactory<>("niveau"));
        colQId.setCellValueFactory(new PropertyValueFactory<>("questionnaireId"));
        colUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colDuree.setCellValueFactory(new PropertyValueFactory<>("dureePassage"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        // Colonne besoin psy
        colPsy.setCellValueFactory(new PropertyValueFactory<>("aBesoinPsy"));
        colPsy.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    Reponsequestionnaire r = (Reponsequestionnaire) getTableRow().getItem();
                    setText(r.isaBesoinPsy() ? "✅ Oui" : "❌ Non");
                }
            }
        });

        tableReponse.setItems(data);
        loadData();
    }

    private void loadData() {
        try {
            List<Reponsequestionnaire> list = service.afficher();
            data.setAll(list);
        } catch (SQLException e) {
            System.out.println("Erreur chargement réponses: " + e.getMessage());
        }
    }
}