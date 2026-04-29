package SystemServices.model;

public class PortEntry {
    private final String protocol;
    private final int port;
    private final String localAddress;
    private final String state;
    private final int pid;
    private final String processName;

    public PortEntry(String protocol, int port, String localAddress,
                     String state, int pid, String processName) {
        this.protocol    = protocol;
        this.port        = port;
        this.localAddress = localAddress;
        this.state       = state;
        this.pid         = pid;
        this.processName = processName;
    }

    public String getProtocol()     { return protocol; }
    public int    getPort()         { return port; }
    public String getLocalAddress() { return localAddress; }
    public String getState()        { return state; }
    public int    getPid()          { return pid; }
    public String getProcessName()  { return processName; }

    public boolean hasProcess() { return pid > 0; }
    public boolean isTcp()      { return protocol.startsWith("tcp"); }
}
