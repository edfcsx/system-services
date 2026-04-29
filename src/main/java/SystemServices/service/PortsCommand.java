package SystemServices.service;

import SystemServices.model.PortEntry;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PortsCommand {

    private static final Pattern PROCESS_PATTERN =
            Pattern.compile("users:\\(\\(\"([^\"]+)\",pid=(\\d+)");

    public List<PortEntry> listPorts() throws Exception {
        List<PortEntry> ports = new ArrayList<>();

        ProcessBuilder pb = new ProcessBuilder("ss", "-tunlp");
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                PortEntry entry = parseLine(line);
                if (entry != null) ports.add(entry);
            }
        }

        process.waitFor();
        ports.sort(Comparator.comparingInt(PortEntry::getPort)
                              .thenComparing(PortEntry::getProtocol));
        return ports;
    }

    private PortEntry parseLine(String line) {
        if (line == null || line.isBlank()) return null;
        String trimmed = line.trim();

        // Skip header
        if (trimmed.startsWith("Netid")) return null;

        // Split into at most 7 parts: netid state recv-q send-q local peer [process]
        String[] parts = trimmed.split("\\s+", 7);
        if (parts.length < 6) return null;

        String netid      = parts[0].toLowerCase();
        String state      = parts[1];
        String localAddr  = parts[4];
        String processInfo = parts.length > 6 ? parts[6] : "";

        // Extract port from "address:port" (lastIndexOf handles IPv6 "[::]:port")
        int port = extractPort(localAddr);
        if (port <= 0) return null;

        // Strip ":port" and "%interface" to get clean address
        String address = extractAddress(localAddr);

        // Extract process name and PID
        String processName = "";
        int pid = 0;
        Matcher m = PROCESS_PATTERN.matcher(processInfo);
        if (m.find()) {
            processName = m.group(1);
            pid = Integer.parseInt(m.group(2));
        }

        return new PortEntry(netid, port, address, state, pid, processName);
    }

    private int extractPort(String addrPort) {
        int colon = addrPort.lastIndexOf(':');
        if (colon < 0) return 0;
        try {
            return Integer.parseInt(addrPort.substring(colon + 1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String extractAddress(String addrPort) {
        int colon = addrPort.lastIndexOf(':');
        String addr = colon >= 0 ? addrPort.substring(0, colon) : addrPort;
        // Strip interface suffix like %lo or %wlp0s20f3
        int pct = addr.indexOf('%');
        if (pct >= 0) addr = addr.substring(0, pct);
        return addr.isBlank() ? "*" : addr;
    }

    public void killProcess(int pid) throws Exception {
        String pidStr = String.valueOf(pid);
        try {
            // Try without privileges first (own processes)
            execute("kill", "-15", pidStr);
        } catch (Exception e1) {
            try {
                execute("pkexec", "kill", "-15", pidStr);
            } catch (Exception e2) {
                execute("sudo", "kill", "-15", pidStr);
            }
        }
    }

    private void execute(String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder out = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) out.append(line).append("\n");
        }

        int code = process.waitFor();
        if (code != 0) {
            throw new RuntimeException("Falhou (código " + code + "): " + out.toString().trim());
        }
    }
}
