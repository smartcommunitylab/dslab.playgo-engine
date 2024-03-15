package dslab.playandgo.engine.tests;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.playandgo.engine.geolocation.model.Geolocation;
import it.smartcommunitylab.playandgo.engine.util.GamificationHelper;

public class ShapeGenerator {
    

    public void generate() throws StreamReadException, DatabindException, IOException {
        Map<String, String> json = new ObjectMapper().readValue(ShapeGenerator.class.getResourceAsStream("/polylines.json"), Map.class);    
        StringBuilder sb = new StringBuilder();
        sb.append("shape_id,shape_pt_lat,shape_pt_lon,shape_pt_sequence\n");
        // generate direct/return shapes with the specified name
        for (Map.Entry<String, String> entry : json.entrySet()) {
            String name = entry.getKey();
            List<Geolocation> points = GamificationHelper.decodePoly(entry.getValue());
            for (int i = 0; i < points.size(); i++) {
                sb.append(name+"A").append(",").append(points.get(i).getLatitude()).append(",").append(points.get(i).getLongitude()).append(",").append(i+1).append("\n");
            }
            Collections.reverse(points);
            for (int i = 0; i < points.size(); i++) {
                sb.append(name+"R").append(",").append(points.get(i).getLatitude()).append(",").append(points.get(i).getLongitude()).append(",").append(i+1).append("\n");
            }
        }
        Files.writeString(Paths.get("./shapes.txt"), sb);
    }
}
