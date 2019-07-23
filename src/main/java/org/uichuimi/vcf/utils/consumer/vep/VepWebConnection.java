package org.uichuimi.vcf.utils.consumer.vep;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.uichuimi.vcf.utils.consumer.vep.json.JSONArray;
import org.uichuimi.vcf.utils.consumer.vep.json.JSONObject;
import org.uichuimi.vcf.utils.consumer.vep.model.VepResponse;
import org.uichuimi.vcf.variant.Variant;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by uichuimi on 24/11/16.
 */
public class VepWebConnection {

    public static final String PATH = "vep/human/region";
    private final String url;

    public VepWebConnection(String url) {
        this.url = String.format("%s%s%s", url, url.endsWith("/") ? "" : "/", PATH);
    }

    public Collection<VepResponse> getAnnotations(Collection<Variant> variants) {
        final String response;
        try {
            response = makeHttpRequest(variants);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
        if (response != null) {
            try {
                final ObjectMapper mapper = new ObjectMapper();
                mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
                return mapper.readValue(response, new TypeReference<Collection<VepResponse>>(){});
            } catch (IOException e) {
                System.out.println(response);
                e.printStackTrace();
            }
        }
        return null;
    }

    private String makeHttpRequest(Collection<Variant> variants) throws MalformedURLException {
        final URL url = new URL(this.url);
        final Map<String, String> header = getHeader();
        final JSONObject message = getJsonMessage(variants);
        return Web.httpRequest(url, header, message, 10000, 6);  // 1 minute (10 sec per try)
    }

    private static Map<String, String> getHeader() {
        final Map<String, String> requestMap = new HashMap<>();
        requestMap.put("Content-Type", "application/json");
        requestMap.put("Accept", "application/json");
        return requestMap;
    }

    private static JSONObject getJsonMessage(Collection<Variant> variants) {
        final JSONArray array = getJsonVariantArray(variants);
        final JSONObject message = new JSONObject();
        // {"variants":array}
        message.put("variants", array);
        return message;
    }

    private static JSONArray getJsonVariantArray(Collection<Variant> variants) {
        // Translate list into JSON
        // ["1 156897 156897 A/C","2 3547966 3547968 TCC/T"]
        final JSONArray array = new JSONArray();
        for (Variant v : variants) {
            int start = v.getCoordinate().getPosition();
            final String ref = v.getReferences().get(0);
            for (String alternative : v.getAlternatives()) {
                int end = v.getCoordinate().getPosition() + ref.length() - 1;
                // 1 156897 156897 A/C
                // 2 3547966 3547968 TCC/T
                array.put(String.format("%s %d %d %s/%s", v.getCoordinate().getChrom(), start, end, ref, alternative));
            }
        }
        return array;
    }
}
