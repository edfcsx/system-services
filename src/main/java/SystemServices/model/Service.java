package SystemServices.model;

public class Service {
    private final String unit;
    private final String load;
    private final String active;
    private final String sub;
    private final String description;

    public Service(String unit, String load, String active, String sub, String description) {
        this.unit = unit;
        this.load = load;
        this.active = active;
        this.sub = sub;
        this.description = description;
    }

    public String getUnit() { return unit; }
    public String getLoad() { return load; }
    public String getActive() { return active; }
    public String getSub() { return sub; }
    public String getDescription() { return description; }

    public boolean isActive() { return "active".equals(active); }
    public boolean isFailed() { return "failed".equals(active); }
}