package net.ME1312.SubServers.Bungee.Host.Internal;

import net.ME1312.SubServers.Bungee.Event.*;
import net.ME1312.SubServers.Bungee.Host.Executable;
import net.ME1312.SubServers.Bungee.Library.Container;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.Library.NamedContainer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.UUID;

public class InternalSubServer extends SubServer {
    private InternalHost host;
    private boolean enabled;
    private Container<Boolean> log;
    private File directory;
    private Executable executable;
    private String stopcmd;
    private Process process;
    private Thread thread;
    private BufferedWriter command;
    private boolean restart;
    private boolean allowrestart;
    private boolean temporary;

    public InternalSubServer(Host host, String name, boolean enabled, int port, String motd, boolean log, String directory, Executable executable, String stopcmd, boolean start, boolean restart, boolean hidden, boolean restricted, boolean temporary) throws InvalidServerException {
        super(host, name, port, motd, hidden, restricted);
        this.host = (InternalHost) host;
        this.enabled = enabled;
        this.log = new Container<Boolean>(log);
        this.directory = new File(host.getDirectory(), directory);
        this.executable = executable;
        this.stopcmd = stopcmd;
        this.process = null;
        this.thread = null;
        this.command = null;
        this.restart = restart;
        this.temporary = temporary;

        if (start || temporary) start();
    }

    private void run() {
        (thread = new Thread(() -> {
            allowrestart = true;
            try {
                process = Runtime.getRuntime().exec(executable.toString(), null, directory);
                System.out.println("SubServers > Now starting " + getName());
                final InternalSubLogger read = new InternalSubLogger(process, getName(), log, null);
                read.start();
                command = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

                try {
                    process.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
                allowrestart = false;
            }

            SubStoppedEvent event = new SubStoppedEvent(this);
            host.plugin.getPluginManager().callEvent(event);
            System.out.println("SubServers > " + getName() + " has stopped");
            process = null;
            command = null;

            if (temporary) {
                try {
                    host.removeSubServer(getName());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                if (restart && allowrestart) {
                    try {
                        Thread.sleep(2500);
                        start();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        })).start();
    }

    @Override
    public boolean start(UUID player) {
        if (enabled && !isRunning()) {
            SubStartEvent event = new SubStartEvent(player, this);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                run();
                return true;
            } else return false;
        } else return false;
    }

    @Override
    public boolean stop(UUID player) {
        if (isRunning()) {
            SubStopEvent event = new SubStopEvent(player, this, false);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                try {
                    allowrestart = false;
                    command.write(stopcmd);
                    command.newLine();
                    command.flush();
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            } else return false;
        } else return false;
    }

    @Override
    public boolean terminate(UUID player) {
        if (isRunning()) {
            SubStopEvent event = new SubStopEvent(player, this, true);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                allowrestart = false;
                process.destroyForcibly();
                return true;
            } else return false;
        } else return false;
    }

    @Override
    public boolean command(UUID player, String command) {
        if (isRunning()) {
            SubSendCommandEvent event = new SubSendCommandEvent(player, this, command);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                try {
                    if (event.getCommand().equalsIgnoreCase(stopcmd)) allowrestart = false;
                    this.command.write(event.getCommand());
                    this.command.newLine();
                    this.command.flush();
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            } else return false;
        } else return false;
    }

    @Override
    public void waitFor() throws InterruptedException {
        while (thread != null && thread.isAlive()) {
            Thread.sleep(250);
        }
    }

    @Override
    public boolean isRunning() {
        return process != null && process.isAlive();
    }

    @Override
    public Host getHost() {
        return host;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean value) {
        enabled = value;
    }

    @Override
    public boolean isLogging() {
        return log.get();
    }

    @Override
    public void setLogging(boolean value) {
        log.set(value);
    }

    @Override
    public String getStopCommand() {
        return stopcmd;
    }

    @Override
    public void setStopCommand(String value) {
        stopcmd = value;
    }

    @Override
    public boolean willAutoRestart() {
        return restart;
    }

    @Override
    public void setAutoRestart(boolean value) {
        restart = value;
    }

    @Override
    public boolean isTemporary() {
        return temporary;
    }
}