import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FarmManagement {

    static AtomicInteger idCounter =
            new AtomicInteger(1);

    static List<Map<String, String>> farms =
            new ArrayList<>();

    static List<Map<String, String>> crops =
            new ArrayList<>();

    static List<Map<String, String>> farmers =
            new ArrayList<>();

    static List<Map<String, String>> buyers =
            new ArrayList<>();


    public static void main(String[] args)
            throws Exception {

        HttpServer server =
                HttpServer.create(
                        new InetSocketAddress(8080), 0);

        server.createContext("/", FarmManagement::home);

        server.createContext(
                "/api/add",
                FarmManagement::addRecord);

        server.createContext(
                "/api/data",
                FarmManagement::getRecords);

        server.createContext(
                "/api/delete",
                FarmManagement::deleteRecord);

        server.createContext(
                "/api/stats",
                FarmManagement::getStats);

        server.setExecutor(null);

        System.out.println(
                "Farm Management System started.");

        System.out.println(
                "Open: http://localhost:8080");

        server.start();
    }


    // Serve HTML and CSS files
    static void home(HttpExchange exchange)
            throws IOException {

        String path =
                exchange.getRequestURI().getPath();

        String fileName;

        if (path.equals("/")) {
            fileName = "index.html";
        }
        else if (path.equals("/style.css")) {
            fileName = "style.css";
        }
        else {
            sendResponse(
                    exchange,
                    404,
                    "File not found");
            return;
        }

        File file = new File(fileName);

        if (!file.exists()) {
            sendResponse(
                    exchange,
                    404,
                    fileName + " not found");
            return;
        }

        byte[] data =
                java.nio.file.Files.readAllBytes(
                        file.toPath());

        String contentType =
                fileName.endsWith(".css")
                        ? "text/css"
                        : "text/html";

        exchange.getResponseHeaders()
                .set("Content-Type",
                        contentType);

        exchange.sendResponseHeaders(
                200, data.length);

        OutputStream output =
                exchange.getResponseBody();

        output.write(data);
        output.close();
    }


    // Add Farm/Crop/Farmer/Buyer
    static void addRecord(HttpExchange exchange)
            throws IOException {

        String body =
                new String(
                        exchange.getRequestBody()
                                .readAllBytes(),
                        StandardCharsets.UTF_8);

        Map<String, String> data =
                parseForm(body);

        String type =
                data.get("type");

        data.remove("type");

        data.put(
                "id",
                String.valueOf(
                        idCounter.getAndIncrement()));

        if ("farm".equals(type)) {
            farms.add(data);
        }

        else if ("crop".equals(type)) {
            crops.add(data);
        }

        else if ("farmer".equals(type)) {
            farmers.add(data);
        }

        else if ("buyer".equals(type)) {
            buyers.add(data);
        }

        else {
            sendResponse(
                    exchange,
                    400,
                    "Invalid record type");

            return;
        }

        sendResponse(
                exchange,
                200,
                "Record added successfully.");
    }


    // Return records
    static void getRecords(HttpExchange exchange)
            throws IOException {

        Map<String, String> query =
                parseQuery(
                        exchange.getRequestURI()
                                .getRawQuery());

        String type =
                query.get("type");

        List<Map<String, String>> records;

        if ("farm".equals(type)) {
            records = farms;
        }
        else if ("crop".equals(type)) {
            records = crops;
        }
        else if ("farmer".equals(type)) {
            records = farmers;
        }
        else if ("buyer".equals(type)) {
            records = buyers;
        }
        else {
            sendResponse(
                    exchange,
                    400,
                    "Invalid type");

            return;
        }

        String json =
                listToJson(records);

        exchange.getResponseHeaders()
                .set("Content-Type",
                        "application/json");

        sendResponse(
                exchange,
                200,
                json);
    }


    // Delete record
    static void deleteRecord(
            HttpExchange exchange)
            throws IOException {

        String body =
                new String(
                        exchange.getRequestBody()
                                .readAllBytes(),
                        StandardCharsets.UTF_8);

        Map<String, String> data =
                parseForm(body);

        String type =
                data.get("type");

        String id =
                data.get("id");

        List<Map<String, String>> records;

        if ("farm".equals(type)) {
            records = farms;
        }
        else if ("crop".equals(type)) {
            records = crops;
        }
        else if ("farmer".equals(type)) {
            records = farmers;
        }
        else if ("buyer".equals(type)) {
            records = buyers;
        }
        else {
            sendResponse(
                    exchange,
                    400,
                    "Invalid type");

            return;
        }

        records.removeIf(
                record ->
                        id.equals(record.get("id")));

        sendResponse(
                exchange,
                200,
                "Record deleted successfully.");
    }


    // Dashboard statistics
    static void getStats(
            HttpExchange exchange)
            throws IOException {

        String json =
                "{"
                + "\"farms\":" + farms.size() + ","
                + "\"crops\":" + crops.size() + ","
                + "\"farmers\":" + farmers.size() + ","
                + "\"buyers\":" + buyers.size()
                + "}";

        exchange.getResponseHeaders()
                .set("Content-Type",
                        "application/json");

        sendResponse(
                exchange,
                200,
                json);
    }


    // Parse URL encoded form
    static Map<String, String> parseForm(
            String input) {

        return parseQuery(input);
    }


    static Map<String, String> parseQuery(
            String query) {

        Map<String, String> map =
                new HashMap<>();

        if (query == null ||
                query.isEmpty()) {

            return map;
        }

        for (String pair :
                query.split("&")) {

            String[] parts =
                    pair.split("=", 2);

            String key =
                    URLDecoder.decode(
                            parts[0],
                            StandardCharsets.UTF_8);

            String value =
                    parts.length > 1
                            ? URLDecoder.decode(
                                parts[1],
                                StandardCharsets.UTF_8)
                            : "";

            map.put(key, value);
        }

        return map;
    }


    // Convert records to JSON
    static String listToJson(
            List<Map<String, String>> records) {

        StringBuilder json =
                new StringBuilder("[");

        for (int i = 0;
             i < records.size();
             i++) {

            Map<String, String> record =
                    records.get(i);

            json.append("{");

            int count = 0;

            for (Map.Entry<String, String> entry :
                    record.entrySet()) {

                if (count++ > 0) {
                    json.append(",");
                }

                json.append("\"")
                        .append(escape(entry.getKey()))
                        .append("\":\"")
                        .append(escape(entry.getValue()))
                        .append("\"");
            }

            json.append("}");

            if (i < records.size() - 1) {
                json.append(",");
            }
        }

        json.append("]");

        return json.toString();
    }


    static String escape(String value) {

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }


    static void sendResponse(
            HttpExchange exchange,
            int status,
            String response)
            throws IOException {

        byte[] data =
                response.getBytes(
                        StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(
                status,
                data.length);

        OutputStream output =
                exchange.getResponseBody();

        output.write(data);
        output.close();
    }
}