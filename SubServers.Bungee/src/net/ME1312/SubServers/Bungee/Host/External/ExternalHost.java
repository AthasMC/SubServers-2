package net.ME1312.SubServers.Bungee.Host.External;

import com.google.common.collect.Range;
import net.ME1312.SubData.Server.ClientHandler;
import net.ME1312.SubData.Server.DataClient;
import net.ME1312.SubData.Server.Protocol.PacketOut;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Event.SubAddServerEvent;
import net.ME1312.SubServers.Bungee.Event.SubRemoveServerEvent;
import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.SubServers.Bungee.Host.SubCreator;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubServers.Bungee.Library.Compatibility.Logger;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketExAddServer;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketExDeleteServer;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketExRemoveServer;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketOutExReset;
import net.ME1312.SubServers.Bungee.SubProxy;
import net.md_5.bungee.BungeeCord;

import java.net.InetAddress;
import java.util.*;

/**
 * External Host Class
 */
public class ExternalHost extends Host implements ClientHandler {
    private HashMap<Integer, SubDataClient> subdata = new HashMap<Integer, SubDataClient>();
    private HashMap<String, SubServer> servers = new HashMap<String, SubServer>();
    private String name;
    boolean available;
    private boolean enabled;
    private InetAddress address;
    private SubCreator creator;
    private String directory;
    private LinkedList<PacketOut> queue;
    private boolean clean;
    SubProxy plugin;

    /**
     * Creates an External Host
     *
     * @param plugin SubServers Internals
     * @param name The Name of your Host
     * @param ports The range of ports to auto-select from
     * @param log Whether apps like SubCreator should log to console (does not apply to servers)
     * @param enabled If your host is Enabled
     * @param address The address of your Host
     * @param directory The runtime directory of your Host
     * @param gitBash The Git Bash directory
     */
    public ExternalHost(SubProxy plugin, String name, boolean enabled, Range<Integer> ports, boolean log, InetAddress address, String directory, String gitBash) {
        super(plugin, name, enabled, ports, log, address, directory, gitBash);
        this.plugin = plugin;
        this.name = name;
        this.available = false;
        this.enabled = enabled;
        this.address = address;
        this.creator = new ExternalSubCreator(this, ports, log, gitBash);
        this.directory = directory;
        this.queue = new LinkedList<PacketOut>();
        this.clean = false;

        subdata.put(0, null);
    }

    @Override
    public DataClient[] getSubData() {
        LinkedList<Integer> keys = new LinkedList<Integer>(subdata.keySet());
        LinkedList<SubDataClient> channels = new LinkedList<SubDataClient>();
        Collections.sort(keys);
        for (Integer channel : keys) channels.add(subdata.get(channel));
        return channels.toArray(new DataClient[0]);
    }

    public void setSubData(DataClient client, int channel) {
        if (channel < 0) throw new IllegalArgumentException("Subchannel ID cannot be less than zero");
        if (client != null || channel == 0) {
            if (!subdata.keySet().contains(channel) || (channel == 0 && (client == null || subdata.get(channel) == null))) {
                subdata.put(channel, (SubDataClient) client);
                if (client != null && (client.getHandler() == null || !equals(client.getHandler()))) ((SubDataClient) client).setHandler(this);
            }
        } else {
            subdata.remove(channel);
        }
    }

    @Override
    public void removeSubData(DataClient client) {
        for (Integer channel : Util.getBackwards(subdata, (SubDataClient) client)) setSubData(null, channel);
    }

    void queue(PacketOut... packet) {
        for (PacketOut p : packet) if (getSubData()[0] == null || !available) {
            queue.add(p);
        } else {
            ((SubDataClient) getSubData()[0]).sendPacket(p);
        }
    }
    private void requeue() {
        SubDataClient client = (SubDataClient) getSubData()[0];
        if (!clean) {
            client.sendPacket(new PacketOutExReset("Prevent Desync"));
            clean = true;
        }
        for (SubServer server : servers.values()) {
            client.sendPacket(new PacketExAddServer(server.getName(), server.isEnabled(), server.getAddress().getPort(), server.isLogging(), server.getPath(), ((ExternalSubServer) server).exec, server.getStopCommand(), (server.isRunning())?((ExternalSubLogger) server.getLogger()).getExternalAddress():null, data -> {
                if (data.contains(0x0002)) ((ExternalSubServer) server).started(data.getUUID(0x0002));
            }));
        }
        while (queue.size() != 0) {
            client.sendPacket(queue.get(0));
            queue.remove(0);
        }
        available = true;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean value) {
        this.enabled = value;
    }

    @Override
    public InetAddress getAddress() {
        return address;
    }

    @Override
    public String getPath() {
        return directory;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public SubCreator getCreator() {
        return creator;
    }

    @Override
    public Map<String, ? extends SubServer> getSubServers() {
        return new TreeMap<String, SubServer>(servers);
    }

    @Override
    public SubServer getSubServer(String name) {
        if (Util.isNull(name)) throw new NullPointerException();
        return getSubServers().get(name.toLowerCase());
    }

    @Override
    public SubServer addSubServer(UUID player, String name, boolean enabled, int port, String motd, boolean log, String directory, String executable, String stopcmd, boolean hidden, boolean restricted) throws InvalidServerException {
        if (plugin.api.getServers().keySet().contains(name.toLowerCase())) throw new InvalidServerException("A Server already exists with this name!");
        ExternalSubServer server = ExternalSubServer.construct(this, name, enabled, port, motd, log, directory, executable, stopcmd, hidden, restricted);
        SubAddServerEvent event = new SubAddServerEvent(player, this, server);
        plugin.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            queue(new PacketExAddServer(name, enabled, port, log, directory, executable, stopcmd, (server.isRunning())?((ExternalSubLogger) server.getLogger()).getExternalAddress():null, data -> {
                if (data.contains(0x0002)) server.started(data.getUUID(0x0002));
            }));
            servers.put(name.toLowerCase(), server);
            return server;
        } else {
            return null;
        }
    }

    @Override
    protected boolean removeSubServer(UUID player, String name, boolean forced) throws InterruptedException {
        if (Util.isNull(name)) throw new NullPointerException();
        SubServer server = servers.get(name.toLowerCase());

        SubRemoveServerEvent event = new SubRemoveServerEvent(player, this, server);
        plugin.getPluginManager().callEvent(event);
        if (forced || !event.isCancelled()) {
            if (server.isRunning()) {
                server.stop();
                server.waitFor();
            }
            servers.remove(name.toLowerCase());
            queue(new PacketExRemoveServer(name.toLowerCase(), data -> {
                if (data.getInt(0x0001) != 0 && data.getInt(0x0001) != 1) {
                    servers.put(name.toLowerCase(), server);
                }
            }));
            return true;
        } else return false;
    }

    @Override
    protected boolean recycleSubServer(UUID player, String name, boolean forced) throws InterruptedException {
        if (Util.isNull(name)) throw new NullPointerException();
        SubServer s = servers.get(name.toLowerCase());
        String server = s.getName();

        SubRemoveServerEvent event = new SubRemoveServerEvent(player, this, s);
        plugin.getPluginManager().callEvent(event);
        if (forced || !event.isCancelled()) {
            if (s.isRunning()) {
                s.stop();
                s.waitFor();
            }

            Logger.get("SubServers").info("Saving...");
            ObjectMap<String> info = (plugin.servers.get().getMap("Servers").getKeys().contains(server))?plugin.servers.get().getMap("Servers").getMap(server).clone():new ObjectMap<String>();
            info.set("Name", server);
            info.set("Timestamp", Calendar.getInstance().getTime().getTime());
            try {
                if (plugin.servers.get().getMap("Servers").getKeys().contains(server)) {
                    plugin.servers.get().getMap("Servers").remove(server);
                    plugin.servers.save();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Logger.get("SubServers").info("Moving Files...");
            queue(new PacketExDeleteServer(server, info, true, data -> {
                if (data.getInt(0x0001) == 0 || data.getInt(0x0001) == 1) {
                    servers.remove(server.toLowerCase());
                    Logger.get("SubServers").info("Deleted SubServer: " + server);
                } else {
                    Logger.get("SubServers").info("Couldn't remove " + server + " from memory. See " + getName() + " console for more details");
                }
            }));
            return true;
        } else return false;
    }

    @Override
    protected boolean deleteSubServer(UUID player, String name, boolean forced) throws InterruptedException {
        if (Util.isNull(name)) throw new NullPointerException();
        SubServer s = servers.get(name.toLowerCase());
        String server = s.getName();

        SubRemoveServerEvent event = new SubRemoveServerEvent(player, this, getSubServer(server));
        plugin.getPluginManager().callEvent(event);
        if (forced || !event.isCancelled()) {
            if (s.isRunning()) {
                s.stop();
                s.waitFor();
            }

            Logger.get("SubServers").info("Saving...");
            ObjectMap<String> info = (plugin.servers.get().getMap("Servers").getKeys().contains(server))?plugin.servers.get().getMap("Servers").getMap(server).clone():new ObjectMap<String>();
            info.set("Name", server);
            info.set("Timestamp", Calendar.getInstance().getTime().getTime());
            try {
                if (plugin.servers.get().getMap("Servers").getKeys().contains(server)) {
                    plugin.servers.get().getMap("Servers").remove(server);
                    plugin.servers.save();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Logger.get("SubServers").info("Removing Files...");
            queue(new PacketExDeleteServer(server, info, false, data -> {
                if (data.getInt(0x0001) == 0 || data.getInt(0x0001) == 1) {
                    servers.remove(server.toLowerCase());
                    Logger.get("SubServers").info("Deleted SubServer: " + server);
                } else {
                    Logger.get("SubServers").info("Couldn't remove " + server + " from memory. See " + getName() + " console for more details");
                }
            }));
            return true;
        } else return false;
    }

    @Override
    public boolean destroy() {
        if (Util.getDespiteException(() -> Util.reflect(BungeeCord.class.getDeclaredField("isRunning"), plugin), true)) {
            return super.destroy();
        }
        return true;
    }

    @Override
    public ObjectMap<String> forSubData() {
        ObjectMap<String> hinfo = super.forSubData();
        ObjectMap<Integer> subdata = new ObjectMap<Integer>();
        for (int channel : this.subdata.keySet()) subdata.set(channel, (this.subdata.get(channel) == null)?null:this.subdata.get(channel).getID());
        hinfo.set("subdata", subdata);
        return hinfo;
    }
}
