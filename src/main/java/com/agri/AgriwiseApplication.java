// package com.agri;

// import org.springframework.boot.SpringApplication;
// import org.springframework.boot.autoconfigure.SpringBootApplication;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;
// import com.agri.model.CropData;
// import com.agri.service.ZAIService;

// import java.nio.charset.StandardCharsets;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.beans.factory.annotation.Value;

// import java.io.*;
// import java.util.*;

// @SpringBootApplication
// @RestController
// @RequestMapping("/api/crops")
// public class AgriwiseApplication {

//     private static final String CSV_FILE_PATH = "crop_database.csv";

//     public static void main(String[] args) {
//         SpringApplication.run(AgriwiseApplication.class, args);
//         System.out.println("🚀 AGRI-ANALYST BACKEND IS RUNNING!");
//     }

//    @Autowired
// private ZAIService zaiService;

// @PostMapping("/chat")
// public ResponseEntity<Map<String, String>> handleChat(@RequestBody Map<String, String> payload) {

//     String userMsg = payload.getOrDefault("message", "");
//     String crop = payload.getOrDefault("cropName", "Unknown");

//     // Load datasets
//     String news = readDataset("/field_intelligence.txt");
//     String market = readDataset("/AgriWise_Crop_Dataset.csv");

//     // 🔥 Call AI instead of manual logic
//     String aiReply = zaiService.getAIResponse(userMsg, crop, market, news);

//     Map<String, String> response = new HashMap<>();
//     response.put("reply", aiReply);

//     return ResponseEntity.ok(response);
// }

// // Z.AI refers to the file
// private String getPlotDataSummary() {
//     try (BufferedReader br = new BufferedReader(new FileReader("crop_database.csv"))) {
//         String line = br.readLine(); // Just get the first/latest plot info
//         if (line != null) return "Active Plot: " + line.split(",")[1];
//     } catch (Exception e) { return "No active plots found"; }
//     return "No data";
// }

//     // 1. Updated Register: Returns the AI values needed for the card
//     @PostMapping("/register")
//     public ResponseEntity<Map<String, Object>> registerPlot(@RequestBody Map<String, Object> payload) {
//         saveToCsv(payload);
//         Map<String, Object> ai = new HashMap<>();
//         ai.put("expectedYield", "4.2 MT/Acre"); // Static demo data
//         ai.put("waterReq", "Moderate");
//         return ResponseEntity.ok(ai);
//     }

//     // 2. Updated Loader: Strictly reads the 4 columns you enter in the UI
//     @GetMapping("/all")
//     public ResponseEntity<List<Map<String, String>>> getAllPlots() {
//         List<Map<String, String>> plots = new ArrayList<>();
//         File file = new File("crop_database.csv");
        
//         if (!file.exists()) return ResponseEntity.ok(plots);

//         try (BufferedReader br = new BufferedReader(new FileReader(file))) {
//             String line;
//             while ((line = br.readLine()) != null) {
//                 String[] v = line.split(",");
//                 // We match the 4 columns: PlotID, CropName, LandSize, Date
//                 if (v.length >= 4) {
//                     Map<String, String> p = new HashMap<>();
//                     p.put("plotId", v[0].trim());
//                     p.put("cropName", v[1].trim());
//                     p.put("landSize", v[2].trim());
//                     p.put("plantingDate", v[3].trim());
//                     // Static demo values for the AI analytics
//                     p.put("expectedYield", "4.2 MT/Acre"); 
//                     p.put("waterReq", "Moderate");
//                     plots.add(p);
//                 }
//             }
//         } catch (IOException e) {
//             e.printStackTrace();
//         }
//         return ResponseEntity.ok(plots);
//     }

//     // 3. Updated CSV Writer: Ensures clean formatting
//     private void saveToCsv(Map<String, Object> plot) {
//         try (FileWriter fw = new FileWriter("crop_database.csv", true); 
//              PrintWriter pw = new PrintWriter(fw)) {
//             pw.println(plot.get("plotId") + "," + 
//                        plot.get("cropName") + "," + 
//                        plot.get("landSize") + "," + 
//                        plot.get("plantingDate"));
//         } catch (IOException e) { e.printStackTrace(); }
//     }

//     private String readDataset(String path) {
//     StringBuilder content = new StringBuilder();
//     try (InputStream is = getClass().getResourceAsStream(path);
//          BufferedReader br = new BufferedReader(new InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8))) {
//         if (is == null) return "Dataset " + path + " not found.";
//         String line;
//         while ((line = br.readLine()) != null) {
//             content.append(line).append("\n");
//         }
//     } catch (Exception e) {
//         return "Error reading dataset.";
//     }
//     return content.toString();
// }
// }

package com.agri;
import com.agri.model.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.agri.engine.*;
import com.agri.model.AnalysisResult;
import com.agri.model.CropData;
import com.agri.model.FarmerProfile;
import com.agri.model.SimulationRequest;
import com.agri.sandbox.ScenarioSolver;
import com.agri.sandbox.SimulationController;
import com.agri.sandbox.SimulationRunner;
import com.agri.service.ZAIService;
import org.springframework.context.event.EventListener;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.util.*;

@SpringBootApplication
@RestController
@RequestMapping("/api/crops")
@CrossOrigin(origins = "*") // 🔥 CRITICAL FIX: Allows your frontend to send data here
public class AgriwiseApplication {

    private static final String CSV_FILE_PATH = "crop_database.csv";

    
    public static void main(String[] args) {
        SpringApplication.run(AgriwiseApplication.class, args);
        System.out.println("🚀 AGRI-ANALYST BACKEND IS RUNNING!");
    }

    @Autowired
    private ZAIService zaiService;

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> handleChat(@RequestBody Map<String, String> payload) {
        String userMsg = payload.getOrDefault("message", "");
        String crop = payload.getOrDefault("cropName", "Unknown");

        // Load datasets
        String news = readDataset("/field_intelligence.txt");
        String market = readDataset("/AgriWise_Crop_Dataset.csv");

        // Call AI instead of manual logic
        String aiReply = zaiService.getAIResponse(userMsg, crop, market, news);

        Map<String, String> response = new HashMap<>();
        response.put("reply", aiReply);

        return ResponseEntity.ok(response);
    }

    private String getPlotDataSummary() {
        try (BufferedReader br = new BufferedReader(new FileReader("crop_database.csv"))) {
            String line = br.readLine(); // Just get the first/latest plot info
            if (line != null) return "Active Plot: " + line.split(",")[1];
        } catch (Exception e) { return "No active plots found"; }
        return "No data";
    }

    // 1. Updated Register: Receives data from HTML and saves it
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerPlot(@RequestBody Map<String, Object> payload) {
        saveToCsv(payload);
        
        // Return AI values to immediately populate the new card on the dashboard
        Map<String, Object> ai = new HashMap<>();
        ai.put("expectedYield", "4.2 MT/Acre"); // You can connect this to real AI logic later
        ai.put("waterReq", "Moderate");
        
        return ResponseEntity.ok(ai);
    }

    // 2. Updated Loader: Strictly reads the 4 columns you enter in the UI
    @GetMapping("/all")
    public ResponseEntity<List<Map<String, String>>> getAllPlots() {
        List<Map<String, String>> plots = new ArrayList<>();
        File file = new File("crop_database.csv");
        
        if (!file.exists()) return ResponseEntity.ok(plots);

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            // Skip header row if it exists
            br.readLine(); 
            
            while ((line = br.readLine()) != null) {
                String[] v = line.split(",");
                // We match the 4 columns: PlotID, CropName, LandSize, Date
                if (v.length >= 4) {
                    Map<String, String> p = new HashMap<>();
                    p.put("plotId", v[0].trim());
                    p.put("cropName", v[1].trim());
                    p.put("landSize", v[2].trim());
                    p.put("plantingDate", v[3].trim());
                    
                    // Static demo values for the AI analytics
                    p.put("expectedYield", "4.2 MT/Acre"); 
                    p.put("waterReq", "Moderate");
                    plots.add(p);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok(plots);
    }

    // 3. Robust CSV Writer: Creates file and headers if missing
    private void saveToCsv(Map<String, Object> plot) {
        File file = new File("crop_database.csv");
        boolean isNewFile = !file.exists();

        try (FileWriter fw = new FileWriter(file, true); 
             PrintWriter pw = new PrintWriter(fw)) {
             
            // If the file didn't exist, write the header row first
            if (isNewFile) {
                pw.println("PlotID,CropName,LandSize,Date");
            }
             
            // Safely write the new plot data
            pw.println(plot.getOrDefault("plotId", "N/A") + "," + 
                       plot.getOrDefault("cropName", "Unknown") + "," + 
                       plot.getOrDefault("landSize", "0") + "," + 
                       plot.getOrDefault("plantingDate", "Unknown"));
                       
        } catch (IOException e) { 
            System.err.println("Failed to write to CSV: " + e.getMessage());
        }
    }

    private String readDataset(String path) {
        StringBuilder content = new StringBuilder();
        try (InputStream is = getClass().getResourceAsStream(path);
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            if (is == null) return "Dataset " + path + " not found.";
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (Exception e) {
            return "Error reading dataset.";
        }
        return content.toString();
}

@Autowired
private ScenarioSolver scenarioSolver; // You'll need to create this service class

// Inside AgriwiseApplication class...

@Autowired
private SimulationController simulationController;

@Autowired
private DecisionService decisionService; // Assuming you have this service

/**
 * This method runs once the application starts. 
 * Use this for testing the console simulation logic.
 */
@EventListener(ApplicationReadyEvent.class)
public void runSimulationTest() {
    FarmerProfile profile = new FarmerProfile(); 
    List<CropData> market = new ArrayList<>();

    try {
        // This is where the red line was
        AnalysisResult baseline = decisionService.analyze(profile, market, "Sunny");
        simulationController.setOriginalResult(baseline);
        System.out.println("✅ Baseline loaded successfully.");
    } catch (IOException e) {
        System.err.println("❌ Failed to load simulation baseline: " + e.getMessage());
    }
}

@PostMapping("/simulate")
public ResponseEntity<AnalysisResult> runSimulation(@RequestBody SimulationRequest request) {
    try {
        // This calls the logic we built
        AnalysisResult result = simulationController.handleSimulationRequest(request);
        return ResponseEntity.ok(result);
    } catch (IOException e) {
        return ResponseEntity.internalServerError().build();
    }
}
}