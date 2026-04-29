package SystemServices.service;

import SystemServices.model.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class SystemctlCommand {

    public List<Service> listServices() throws Exception {
        List<Service> services = new ArrayList<>();
        ProcessBuilder pb = new ProcessBuilder(
                "systemctl", "list-units", "--type=service", "--all",
                "--no-pager", "--no-legend", "--plain"
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Service s = parseLine(line);
                if (s != null) services.add(s);
            }
        }

        process.waitFor();
        return services;
    }

    private Service parseLine(String line) {
        if (line == null || line.isBlank()) return null;

        String trimmed = line.trim();

        // Remove bullet/dot indicator prefix
        if (!trimmed.isEmpty() && !Character.isLetterOrDigit(trimmed.charAt(0))) {
            trimmed = trimmed.substring(1).trim();
        }

        String[] parts = trimmed.split("\\s+", 5);
        if (parts.length < 4) return null;

        String unit = parts[0];
        if ("UNIT".equals(unit)) return null;

        String load = parts[1];
        String active = parts[2];
        String sub = parts[3];
        String description = parts.length > 4 ? parts[4] : "";

        return new Service(unit, load, active, sub, description);
    }

    private String execute(String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Comando falhou (código " + exitCode + "): " + output.toString().trim());
        }
        return output.toString();
    }

    public void startService(String unit) throws Exception {
        tryWithPrivileges("start", unit);
    }

    public void stopService(String unit) throws Exception {
        tryWithPrivileges("stop", unit);
    }

    public void restartService(String unit) throws Exception {
        tryWithPrivileges("restart", unit);
    }

    private void tryWithPrivileges(String action, String unit) throws Exception {
        // Try pkexec first (shows polkit GUI dialog), then sudo
        try {
            execute("pkexec", "systemctl", action, unit);
        } catch (Exception e) {
            execute("sudo", "systemctl", action, unit);
        }
    }
}