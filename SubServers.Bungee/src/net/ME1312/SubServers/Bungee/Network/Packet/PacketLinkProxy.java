package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubData.Server.Protocol.Initial.InitialPacket;
import net.ME1312.SubServers.Bungee.Event.SubAddProxyEvent;
import net.ME1312.SubServers.Bungee.Host.Proxy;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubServers.Bungee.Host.ServerContainer;
import net.ME1312.SubServers.Bungee.SubPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Link Proxy Packet
 */
public class PacketLinkProxy implements InitialPacket, PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private SubPlugin plugin;
    private int response;
    private String message;
    private String name;

    /**
     * New PacketLinkProxy (In)
     *
     * @param plugin SubPlugin
     */
    public PacketLinkProxy(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    /**
     * New PacketLinkProxy (Out)
     *
     * @param name The name that was generated
     * @param response Response ID
     */
    public PacketLinkProxy(String name, int response, String message) {
        this.name = name;
        this.response = response;
        this.message = message;
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> json = new ObjectMap<Integer>();
        json.set(0x0000, name);
        json.set(0x0001, response);
        if (message != null) json.set(0x0002, message);
        return json;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        try {
            Map<String, Proxy> proxies = plugin.api.getProxies();
            String name = ((data.contains(0x0000))?data.getRawString(0x0000):null);
            Integer channel = data.getInt(0x0001);

            boolean isnew = false;
            Proxy proxy;
            if (name != null && proxies.keySet().contains(name.toLowerCase())) {
                proxy = proxies.get(name.toLowerCase());
            } else {
                proxy = new Proxy((name != null && !proxies.keySet().contains(name.toLowerCase()))?name:null);
                isnew = true;
                plugin.proxies.put(proxy.getName().toLowerCase(), proxy);
            }
            HashMap<Integer, SubDataClient> subdata = Util.getDespiteException(() -> Util.reflect(Proxy.class.getDeclaredField("subdata"), proxy), null);
            if (!subdata.keySet().contains(channel) || (channel == 0 && subdata.get(0) == null)) {
                proxy.setSubData(client, channel);
                if (isnew) plugin.getPluginManager().callEvent(new SubAddProxyEvent(proxy));
                System.out.println("SubData > " + client.getAddress().toString() + " has been defined as Proxy: " + proxy.getName() + ((channel > 0)?" (Sub-"+channel+")":""));
                client.sendPacket(new PacketLinkProxy(proxy.getName(), 0, null));
                setReady(client, true);
            } else {
                client.sendPacket(new PacketLinkProxy(proxy.getName(), 2, "Proxy already linked"));

            }
        } catch (Throwable e) {
            client.sendPacket(new PacketLinkProxy(null, 1, null));
            e.printStackTrace();
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
