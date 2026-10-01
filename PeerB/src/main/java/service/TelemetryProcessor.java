package service;

import model.TelemetryData;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TelemetryProcessor {

    private final Map<String, TelemetryData> lastReadings = new ConcurrentHashMap<>();

    public String process(String rawMessage) {
        // Paso 1.1: nulo o vacío -> error
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            return "ERROR;INVALID_FORMAT";
        }

        // Paso 1.2: separar por ";"
        String[] parts = rawMessage.trim().split(";");
        if (parts.length == 0) {
            return "ERROR;INVALID_FORMAT";
        }

        // Paso 1.3: consulta de estado
        if (parts[0].trim().equalsIgnoreCase("STATUS")) {
            if (parts.length != 2 || parts[1].trim().isEmpty()) {
                return "ERROR;INVALID_FORMAT";
            }
            String statusId = parts[1].trim();
            TelemetryData data = lastReadings.get(statusId);
            if (data == null) {
                return "ERROR;DEVICE_NOT_FOUND";
            }
            return "STATUS_OK;" + data.getDeviceId() + ";" + data.getSensorType() + ";" + data.getValue();
        }

        // Paso 1.4: validar 3 partes no vacías y convertir el valor
        if (parts.length != 3
                || parts[0].trim().isEmpty()
                || parts[1].trim().isEmpty()
                || parts[2].trim().isEmpty()) {
            return "ERROR;INVALID_FORMAT";
        }
        String deviceId = parts[0].trim();
        String sensorType = parts[1].trim();
        String valueStr = parts[2].trim();

        double value;
        try {
            value = Double.parseDouble(valueStr);
        } catch (NumberFormatException e) {
            return "ERROR;INVALID_FORMAT";
        }

        // Paso 1.5: guardar la lectura
        lastReadings.put(deviceId, new TelemetryData(deviceId, sensorType, value));

        // Paso 1.6: validar tipo y evaluar rangos
        switch (sensorType) {
            case "TEMP":
                if (value > 40.0) return "ALERT;HIGH_TEMPERATURE;" + value;
                if (value < 0.0)  return "ALERT;FREEZING_TEMPERATURE;" + value;
                return "OK;TEMP_RECORDED;" + value;
            case "HUMIDITY":
                if (value > 90.0) return "ALERT;HIGH_HUMIDITY;" + value;
                if (value < 20.0) return "ALERT;LOW_HUMIDITY;" + value;
                return "OK;HUMIDITY_RECORDED;" + value;
            case "BATTERY":
                if (value < 20.0) return "ALERT;LOW_BATTERY;" + value;
                return "OK;BATTERY_OK;" + value;
            default:
                return "ERROR;UNKNOWN_SENSOR_TYPE";
        }
    }

    public Map<String, TelemetryData> getLastReadings() {
        return lastReadings;
    }

    public void clear() {
        lastReadings.clear();
    }
}
