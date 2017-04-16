package net.ME1312.SubServers.Host.Executable;

import net.ME1312.SubServers.Host.Library.Config.YAMLSection;
import net.ME1312.SubServers.Host.Library.Container;
import net.ME1312.SubServers.Host.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Host.Library.UniversalFile;
import net.ME1312.SubServers.Host.Library.Util;
import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.Network.Packet.PacketExUpdateServer;
import net.ME1312.SubServers.Host.ExHost;

import java.io.*;
import java.util.LinkedList;
import java.util.UUID;
import java.util.jar.JarFile;

/**
 * Internal SubServer Class
 */
public class SubServer {
    private ExHost host;
    private String name;
    private boolean enabled;
    private Container<Boolean> log;
    private String dir;
    private File directory;
    private Executable executable;
    private Process process;
    private SubLogger logger;
    private Thread thread;
    private BufferedWriter command;
    private LinkedList<String> queue;
    private String stopcmd;
    private boolean allowrestart;

    /**
     * Creates a SubServer
     *
     * @param host SubServers.Host
     * @param name Name
     * @param enabled Enabled Status
     * @param log Logging Status
     * @param directory Directory
     * @param executable Executable String
     * @param stopcmd Stop Command
     * @throws InvalidServerException
     */
    public SubServer(ExHost host, String name, boolean enabled, boolean log, String directory, Executable executable, String stopcmd) throws InvalidServerException {
        if (Util.isNull(host, name, enabled, log, directory, executable)) throw new NullPointerException();
        this.host = host;
        this.name = name;
        this.enabled = enabled;
        this.log = new Container<Boolean>(log);
        this.dir = directory;
        this.directory = new File(host.host.getRawString("Directory"), directory);
        this.executable = executable;
        this.process = null;
        this.logger = new SubLogger(null, this, name, null, this.log, null);
        this.thread = null;
        this.command = null;
        this.queue = new LinkedList<String>();
        this.stopcmd = stopcmd;

        if (new UniversalFile(this.directory, "plugins:SubServers.Client.jar").exists()) {
            try {
                JarFile jar = new JarFile(new UniversalFile(this.directory, "plugins:SubServers.Client.jar"));
                YAMLSection plugin = new YAMLSection(Util.readAll(new InputStreamReader(jar.getInputStream(jar.getJarEntry("plugin.yml")))));
                YAMLSection bplugin = new YAMLSection(Util.readAll(new InputStreamReader(ExHost.class.getResourceAsStream("/net/ME1312/SubServers/Host/Library/Files/bukkit.yml"))));
                if (new Version(plugin.getString("version")).compareTo(new Version(bplugin.getString("version"))) < 0) {
                    new UniversalFile(this.directory, "plugins:SubServers.Client.jar").delete();
                    Util.copyFromJar(ExHost.class.getClassLoader(), "net/ME1312/SubServers/Host/Library/Files/bukkit.jar", new UniversalFile(this.directory, "plugins:SubServers.Client.jar").getPath());
                }
            } catch (Throwable e) {
                host.log.info.println("Couldn't auto-update SubServers.Client.jar");
                host.log.error.println(e);
            }
        }
    }

    private void run() {
        boolean falsestart = true;
        allowrestart = true;
        try {
            process = Runtime.getRuntime().exec(executable.toString(), null, directory);
            falsestart = false;
            host.log.info.println("Now starting " + name);
            logger.process = process;
            logger.start();
            command = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            for (String command : queue) if (process.isAlive()) {
                this.command.write(command);
                this.command.newLine();
                this.command.flush();
            }
            queue.clear();

            if (process.isAlive()) process.waitFor();
        } catch (IOException | InterruptedException e) {
            host.log.error.println(e);
            allowrestart = false;
            if (falsestart) host.subdata.sendPacket(new PacketExUpdateServer(this, PacketExUpdateServer.UpdateType.LAUNCH_EXCEPTION));
        }

        host.subdata.sendPacket(new PacketExUpdateServer(this, PacketExUpdateServer.UpdateType.STOPPED, (Integer) process.exitValue(), (Boolean) allowrestart));
        host.log.info.println(name + " has stopped");
        process = null;
        command = null;
    }

    /**
     * Starts the Server
     *
     * @param address External Logging Address
     */
    public void start(UUID address) {
        if (isEnabled() && !(thread != null && thread.isAlive())) {
            logger.address = address;
            (thread = new Thread(this::run)).start();
        }
    }

    /**
     * Stops the Server
     */
    public void stop() {
        if (thread != null && thread.isAlive()) {
            try {
                allowrestart = false;
                if (process != null && process.isAlive()) {
                    command.write(stopcmd);
                    command.newLine();
                    command.flush();
                }
            } catch (IOException e) {
                host.log.error.println(e);
            }
        }
    }
    /**
     * Terminates the Server
     */
    public void terminate() {
        allowrestart = false;
        if (process != null && process.isAlive()) process.destroyForcibly();
    }

    /**
     * Commands the Server
     *
     * @param command Command to Send
     */
    public void command(String command) {
        if (Util.isNull(command)) throw new NullPointerException();
        if (thread != null && thread.isAlive()) {
            try {
                if (command.equalsIgnoreCase(stopcmd)) allowrestart = false;
                if (process != null && process.isAlive()) {
                    this.command.write(command);
                    this.command.newLine();
                    this.command.flush();
                }
            } catch (IOException e) {
                host.log.error.println(e);
            }
        }
    }

    /**
     * Waits for the Server to Stop
     *
     * @throws InterruptedException
     */
    public void waitFor() throws InterruptedException {
        while (thread != null && thread.isAlive()) {
            Thread.sleep(250);
        }
    }

    /**
     * Gets the name of the Server
     *
     * @return Server Name
     */
    public String getName() {
        return name;
    }

    /**
     * If the Server is Running
     *
     * @return Running Status
     */
    public boolean isRunning() {
        return process != null && process.isAlive();
    }

    /**
     * If the Server is Enabled
     *
     * @return Enabled Status
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Set if the Server is Enabled
     *
     * @param value Value
     */
    public void setEnabled(boolean value) {
        if (Util.isNull(value)) throw new NullPointerException();
        enabled = value;
    }

    /**
     * If the Server is Logging
     *
     * @return Logging Status
     */
    public boolean isLogging() {
        return log.get();
    }

    /**
     * Set if the Server is Logging
     *
     * @param value Value
     */
    public void setLogging(boolean value) {
        if (Util.isNull(value)) throw new NullPointerException();
        log.set(value);
    }

    /**
     * Get Process Logger
     */
    public SubLogger getLogger() {
        return logger;
    }

    /**
     * Get the Server Directory
     *
     * @return Server Directory
     */
    public String getDirectory() {
        return dir;
    }

    /**
     * Grab the Command to Stop the Server
     *
     * @return Stop Command
     */
    public String getStopCommand() {
        return stopcmd;
    }

    /**
     * Set the Command that Stops the Server
     *
     * @param value Value
     */
    public void setStopCommand(String value) {
        if (Util.isNull(value)) throw new NullPointerException();
        stopcmd = value;
    }
}
