package org.example.services.evenement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour l'API Nominatim d'OpenStreetMap
 * Permet la recherche de lieux et l'autocomplétion
 */
public class EventNominatimService {

    private static final String NOMINATIM_API = "https://nominatim.openstreetmap.org/search";
    private static final String NOMINATIM_REVERSE = "https://nominatim.openstreetmap.org/reverse";
    private static final String USER_AGENT = "UniMindEventApp/1.0";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public EventNominatimService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Recherche des lieux avec autocomplétion
     * @param query Le texte à rechercher
     * @return Liste des lieux suggérés
     */
    public List<EventLocationSuggestion> searchLocations(String query) {
        List<EventLocationSuggestion> suggestions = new ArrayList<>();
        
        if (query == null || query.trim().isEmpty()) {
            return suggestions;
        }

        try {
            String url = NOMINATIM_API + "?format=json&q=" + 
                        java.net.URLEncoder.encode(query, "UTF-8") + 
                        "&limit=5&addressdetails=1";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                
                for (JsonNode node : root) {
                    String displayName = node.get("display_name").asText();
                    String lat = node.get("lat").asText();
                    String lon = node.get("lon").asText();
                    
                    // Extraire le nom principal et la ville
                    String name = extractPrimaryName(displayName);
                    String city = extractCity(node);
                    
                    suggestions.add(new EventLocationSuggestion(
                            displayName,
                            name,
                            city,
                            Double.parseDouble(lat),
                            Double.parseDouble(lon)
                    ));
                }
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Erreur lors de la recherche Nominatim: " + e.getMessage());
        }

        return suggestions;
    }

    /**
     * Recherche asynchrone des lieux (pour ne pas bloquer l'UI)
     */
    public Task<List<EventLocationSuggestion>> searchLocationsAsync(String query) {
        return new Task<>() {
            @Override
            protected List<EventLocationSuggestion> call() {
                return searchLocations(query);
            }
        };
    }

    /**
     * Reverse geocoding : obtenir l'adresse à partir de coordonnées
     */
    public String reverseGeocode(double lat, double lon) {
        try {
            String url = NOMINATIM_REVERSE + "?format=json&lat=" + lat + "&lon=" + lon + "&addressdetails=1";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                if (root.has("display_name")) {
                    return root.get("display_name").asText();
                }
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Erreur lors du reverse geocoding: " + e.getMessage());
        }
        return "";
    }

    private String extractPrimaryName(String displayName) {
        // Extraire le premier élément (généralement le nom du lieu)
        String[] parts = displayName.split(",");
        return parts.length > 0 ? parts[0].trim() : displayName;
    }

    private String extractCity(JsonNode node) {
        if (node.has("address")) {
            JsonNode address = node.get("address");
            if (address.has("city")) {
                return address.get("city").asText();
            } else if (address.has("town")) {
                return address.get("town").asText();
            } else if (address.has("village")) {
                return address.get("village").asText();
            } else if (address.has("municipality")) {
                return address.get("municipality").asText();
            }
        }
        return "";
    }

    /**
     * Classe pour représenter une suggestion de lieu
     */
    public static class EventLocationSuggestion {
        private final String displayName;
        private final String name;
        private final String city;
        private final double latitude;
        private final double longitude;

        public EventLocationSuggestion(String displayName, String name, String city, 
                                       double latitude, double longitude) {
            this.displayName = displayName;
            this.name = name;
            this.city = city;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getName() {
            return name;
        }

        public String getCity() {
            return city;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        @Override
        public String toString() {
            return name + (city != null && !city.isEmpty() ? " (" + city + ")" : "");
        }
    }
}
