package net.ME1312.SubServers.Host;

import net.ME1312.Galaxi.Engine.GalaxiEngine;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.Galaxi.Plugin.Command;
import net.ME1312.SubServers.Host.Library.TextColor;
import net.ME1312.SubServers.Host.Network.API.*;
import net.ME1312.SubServers.Host.Network.Packet.*;

import java.util.*;

/**
 * Command Class
 */
public class SubCommand {
    private SubCommand() {}
    protected static void load(ExHost host) {
        new Command(host.info) {
            @Override
            public void command(String handle, String[] args) {
                if (args.length > 0) {
                    int i = 0;
                    String str = args[0];
                    if (args.length > 1) {
                        do {
                            i++;
                            str = str + " " + args[i].replace(" ", "\\ ");
                        } while ((i + 1) != args.length);
                    }
                    GalaxiEngine.getInstance().getConsoleReader().runCommand(str);
                }
            }
        }.usage("<Command>", "[Args...]").description("An alias for commands").help(
                "This command is an alias for all registered commands for ease of use.",
                "",
                "Examples:",
                "  /sub help -> /help",
                "  /sub version ExamplePlugin -> /version ExamplePlugin"
        ).register("sub", "subserver", "subservers");
        new Command(host.info) {
            @Override
            public void command(String handle, String[] args) {
                host.api.getGroups(groups -> host.api.getHosts(hosts -> host.api.getServers(servers -> host.api.getMasterProxy(proxymaster -> host.api.getProxies(proxies -> {
                    int i = 0;
                    boolean sent = false;
                    String div = TextColor.RESET + ", ";
                    if (groups.keySet().size() > 0) {
                        host.log.message.println("Group/Server List:");
                        for (String group : groups.keySet()) {
                            String message = "  ";
                            message += TextColor.GOLD + group + TextColor.RESET + ": ";
                            for (Server server : groups.get(group)) {
                                if (i != 0) message += div;
                                if (!(server instanceof SubServer)) {
                                    message += TextColor.WHITE;
                                } else if (((SubServer) server).isRunning()) {
                                    if (((SubServer) server).getStopAction() == SubServer.StopAction.REMOVE_SERVER || ((SubServer) server).getStopAction() == SubServer.StopAction.DELETE_SERVER) {
                                        message += TextColor.AQUA;
                                    } else {
                                        message += TextColor.GREEN;
                                    }
                                } else if (((SubServer) server).isEnabled() && ((SubServer) server).getCurrentIncompatibilities().size() == 0) {
                                    message += TextColor.YELLOW;
                                } else {
                                    message += TextColor.RED;
                                }
                                message += server.getDisplayName() + " (" + server.getAddress().getAddress().getHostAddress()+':'+server.getAddress().getPort() + ((server.getName().equals(server.getDisplayName())) ? "" : TextColor.stripColor(div) + server.getName()) + ")";
                                i++;
                            }
                            if (i == 0) message += TextColor.RESET + "(none)";
                            host.log.message.println(message);
                            i = 0;
                            sent = true;
                        }
                        if (!sent) host.log.message.println(TextColor.RESET + "(none)");
                        sent = false;
                    }
                    ExHost h = host;
                    host.log.message.println("Host/SubServer List:");
                    for (Host host : hosts.values()) {
                        String message = "  ";
                        if (host.isAvailable() && host.isEnabled()) {
                            message += TextColor.AQUA;
                        } else {
                            message += TextColor.RED;
                        }
                        message += host.getDisplayName() + " (" + host.getAddress().getHostAddress() + ((host.getName().equals(host.getDisplayName()))?"":TextColor.stripColor(div)+host.getName()) + ")" + TextColor.RESET + ": ";
                        for (SubServer subserver : host.getSubServers().values()) {
                            if (i != 0) message += div;
                            if (subserver.isRunning()) {
                                if (subserver.getStopAction() == SubServer.StopAction.REMOVE_SERVER || subserver.getStopAction() == SubServer.StopAction.DELETE_SERVER) {
                                    message += TextColor.AQUA;
                                } else {
                                    message += TextColor.GREEN;
                                }
                            } else if (subserver.isEnabled() && subserver.getCurrentIncompatibilities().size() == 0) {
                                message += TextColor.YELLOW;
                            } else {
                                message += TextColor.RED;
                            }
                            message += subserver.getDisplayName() + " (" + subserver.getAddress().getPort() + ((subserver.getName().equals(subserver.getDisplayName()))?"":TextColor.stripColor(div)+subserver.getName()) + ")";
                            i++;
                        }
                        if (i == 0) message += TextColor.RESET + "(none)";
                        h.log.message.println(message);
                        i = 0;
                        sent = true;
                    }
                    if (!sent) host.log.message.println(TextColor.RESET + "(none)");
                    host.log.message.println("Server List:");
                    String message = "  ";
                    for (Server server : servers.values()) if (!(server instanceof SubServer)) {
                        if (i != 0) message += div;
                        message += TextColor.WHITE + server.getDisplayName() + " (" + server.getAddress().getAddress().getHostAddress()+':'+server.getAddress().getPort() + ((server.getName().equals(server.getDisplayName()))?"":TextColor.stripColor(div)+server.getName()) + ")";
                        i++;
                    }
                    if (i == 0) message += TextColor.RESET + "(none)";
                    host.log.message.println(message);
                    if (proxies.keySet().size() > 0) {
                        host.log.message.println("Proxy List:");
                        message = "  (master)";
                        for (Proxy proxy : proxies.values()) {
                            message += div;
                            if (proxy.getSubData() != null && proxy.isRedis()) {
                                message += TextColor.GREEN;
                            } else if (proxy.getSubData() != null) {
                                message += TextColor.AQUA;
                            } else if (proxy.isRedis()) {
                                message += TextColor.WHITE;
                            } else {
                                message += TextColor.RED;
                            }
                            message += proxy.getDisplayName() + ((proxy.getName().equals(proxy.getDisplayName()))?"":" ("+proxy.getName()+')');
                        }
                        host.log.message.println(message);
                    }
                })))));
            }
        }.description("Lists the available Hosts and Servers").help(
                "This command will print a list of the available Hosts and Servers.",
                "You can then use these names in commands where applicable.",
                "",
                "Example:",
                "  /list"
        ).register("list");
        new Command(host.info) {
            @Override
            public void command(String handle, String[] args) {
                if (args.length > 0) {
                    String type = (args.length > 1)?args[0]:null;
                    String name = args[(type != null)?1:0];

                    Runnable getServer = () -> host.api.getServer(name, server -> {
                        if (server != null) {
                            host.log.message.println("SubServers > Info on " + ((server instanceof SubServer)?"Sub":"") + "Server: " + TextColor.WHITE + server.getDisplayName());
                            if (!server.getName().equals(server.getDisplayName())) host.log.message.println(" -> System Name: " + TextColor.WHITE  + server.getName());
                            if (server instanceof SubServer) {
                                host.log.message.println(" -> Enabled: " + ((((SubServer) server).isEnabled())?TextColor.GREEN+"yes":TextColor.RED+"no"));
                                if (!((SubServer) server).isEditable()) host.log.message.println(" -> Editable: " + TextColor.RED + "no");
                                host.log.message.println(" -> Host: " + TextColor.WHITE  + ((SubServer) server).getHost());
                            }
                            if (server.getGroups().size() > 0) host.log.message.println(" -> Group" + ((server.getGroups().size() > 1)?"s:":": " + TextColor.WHITE + server.getGroups().get(0)));
                            if (server.getGroups().size() > 1) for (String group : server.getGroups()) host.log.message.println("      - " + TextColor.WHITE + group);
                            host.log.message.println(" -> Address: " + TextColor.WHITE + server.getAddress().getAddress().getHostAddress()+':'+server.getAddress().getPort());
                            if (server instanceof SubServer) host.log.message.println(" -> Running: " + ((((SubServer) server).isRunning())?TextColor.GREEN+"yes":TextColor.RED+"no"));
                            if (!(server instanceof SubServer) || ((SubServer) server).isRunning()) {
                                host.log.message.println(" -> Connected: " + ((server.getSubData() != null)?TextColor.GREEN+"yes":TextColor.RED+"no"));
                                host.log.message.println(" -> Players: " + TextColor.AQUA + server.getPlayers().size() + " online");
                            }
                            host.log.message.println(" -> MOTD: " + TextColor.WHITE + TextColor.stripColor(server.getMotd()));
                            if (server instanceof SubServer && ((SubServer) server).getStopAction() != SubServer.StopAction.NONE) host.log.message.println(" -> Stop Action: " + TextColor.WHITE + ((SubServer) server).getStopAction().toString());
                            host.log.message.println(" -> Signature: " + TextColor.AQUA + server.getSignature());
                            if (server instanceof SubServer) host.log.message.println(" -> Logging: " + ((((SubServer) server).isLogging())?TextColor.GREEN+"yes":TextColor.RED+"no"));
                            host.log.message.println(" -> Restricted: " + ((server.isRestricted())?TextColor.GREEN+"yes":TextColor.RED+"no"));
                            if (server instanceof SubServer && ((SubServer) server).getIncompatibilities().size() > 0) {
                                List<String> current = new ArrayList<String>();
                                for (String other : ((SubServer) server).getCurrentIncompatibilities()) current.add(other.toLowerCase());
                                host.log.message.println(" -> Incompatibilities:");
                                for (String other : ((SubServer) server).getIncompatibilities()) host.log.message.println("      - " + ((current.contains(other.toLowerCase()))?TextColor.WHITE:TextColor.GRAY) + other);
                            }
                            host.log.message.println(" -> Hidden: " + ((server.isHidden())?TextColor.GREEN+"yes":TextColor.RED+"no"));
                        } else {
                            if (type == null) {
                                host.log.message.println("SubServers > There is no object with that name");
                            } else {
                                host.log.message.println("SubServers > There is no server with that name");
                            }
                        }
                    });
                    Runnable getGroup = () -> host.api.getGroup(name, group -> {
                        if (group != null) {
                            host.log.message.println("SubServers > Info on Group: " + TextColor.WHITE + name);
                            host.log.message.println(" -> Servers: " + ((group.size() <= 0)?TextColor.GRAY + "(none)":TextColor.AQUA.toString() + group.size()));
                            for (Server server : group) host.log.message.println("      - " + TextColor.WHITE + server.getDisplayName() + ((server.getName().equals(server.getDisplayName()))?"":" ("+server.getName()+')'));
                        } else {
                            if (type == null) {
                                getServer.run();
                            } else {
                                host.log.message.println("SubServers > There is no group with that name");
                            }
                        }
                    });
                    ExHost h = host;
                    Runnable getHost = () -> host.api.getHost(name, host -> {
                        if (host != null) {
                            h.log.message.println("SubServers > Info on Host: " + TextColor.WHITE + host.getDisplayName());
                            if (!host.getName().equals(host.getDisplayName())) h.log.message.println(" -> System Name: " + TextColor.WHITE  + host.getName());
                            h.log.message.println(" -> Available: " + ((host.isAvailable())?TextColor.GREEN+"yes":TextColor.RED+"no"));
                            h.log.message.println(" -> Enabled: " + ((host.isEnabled())?TextColor.GREEN+"yes":TextColor.RED+"no"));
                            h.log.message.println(" -> Address: " + TextColor.WHITE + host.getAddress().getHostAddress());
                            if (host.getSubData() != null) h.log.message.println(" -> Connected: " + TextColor.GREEN + "yes");
                            h.log.message.println(" -> SubServers: " + ((host.getSubServers().keySet().size() <= 0)?TextColor.GRAY + "(none)":TextColor.AQUA.toString() + host.getSubServers().keySet().size()));
                            for (SubServer subserver : host.getSubServers().values()) h.log.message.println("      - " + ((subserver.isEnabled())?TextColor.WHITE:TextColor.GRAY) + subserver.getDisplayName() + ((subserver.getName().equals(subserver.getDisplayName()))?"":" ("+subserver.getName()+')'));
                            h.log.message.println(" -> Templates: " + ((host.getCreator().getTemplates().keySet().size() <= 0)?TextColor.GRAY + "(none)":TextColor.AQUA.toString() + host.getCreator().getTemplates().keySet().size()));
                            for (SubCreator.ServerTemplate template : host.getCreator().getTemplates().values()) h.log.message.println("      - " + ((template.isEnabled())?TextColor.WHITE:TextColor.GRAY) + template.getDisplayName() + ((template.getName().equals(template.getDisplayName()))?"":" ("+template.getName()+')'));
                            h.log.message.println(" -> Signature: " + TextColor.AQUA + host.getSignature());
                        } else {
                            if (type == null) {
                                getGroup.run();
                            } else {
                                h.log.message.println("SubServers > There is no host with that name");
                            }
                        }
                    });
                    Runnable getProxy = () -> host.api.getProxy(name, proxy -> {
                        if (proxy != null) {
                            host.log.message.println("SubServers > Info on Proxy: " + TextColor.WHITE + proxy.getDisplayName());
                            if (!proxy.getName().equals(proxy.getDisplayName())) host.log.message.println(" -> System Name: " + TextColor.WHITE  + proxy.getName());
                            host.log.message.println(" -> Connected: " + ((proxy.getSubData() != null)?TextColor.GREEN+"yes":TextColor.RED+"no"));
                            host.log.message.println(" -> Redis: "  + ((proxy.isRedis())?TextColor.GREEN:TextColor.RED+"un") + "available");
                            if (proxy.isRedis()) host.log.message.println(" -> Players: " + TextColor.AQUA + proxy.getPlayers().size() + " online");
                            host.log.message.println(" -> Signature: " + TextColor.AQUA + proxy.getSignature());
                        } else {
                            if (type == null) {
                                getHost.run();
                            } else {
                                host.log.message.println("SubServers > There is no proxy with that name");
                            }
                        }
                    });

                    if (type == null) {
                        getProxy.run();
                    } else {
                        switch (type.toLowerCase()) {
                            case "p":
                            case "proxy":
                                getProxy.run();
                                break;
                            case "h":
                            case "host":
                                getHost.run();
                                break;
                            case "g":
                            case "group":
                                getGroup.run();
                                break;
                            case "s":
                            case "server":
                            case "subserver":
                                getServer.run();
                                break;
                            default:
                                host.log.message.println("SubServers > There is no object type with that name");
                        }
                    }
                } else {
                    host.log.message.println("SubServers > Usage: /" + handle + " [proxy|host|group|server] <Name>");
                }
            }
        }.usage("[proxy|host|group|server]", "<Name>").description("Gets information about an Object").help(
                "This command will print a list of information about",
                "the specified Object.",
                "",
                "If the [proxy|host|group|server] option is provided,",
                "it will only include objects of the type specified in the search.",
                "",
                "The <Name> argument is required, and should be the name of",
                "the Object you want to obtain information about.",
                "",
                "Examples:",
                "  /info ExampleServer",
                "  /info server ExampleServer"
        ).register("info", "status");
        new Command(host.info) {
            @Override
            public void command(String handle, String[] args) {
                if (args.length > 0) {
                    host.subdata.sendPacket(new PacketStartServer(null, args[0], data -> {
                        switch (data.getInt("r")) {
                            case 3:
                                host.log.message.println("There is no server with that name");
                                break;
                            case 4:
                                host.log.message.println("That Server is not a SubServer");
                                break;
                            case 5:
                                host.log.message.println("That SubServer's Host is not available");
                                break;
                            case 6:
                                host.log.message.println("That SubServer's Host is not enabled");
                                break;
                            case 7:
                                host.log.message.println("That SubServer is not enabled");
                                break;
                            case 8:
                                host.log.message.println("That SubServer is already running");
                                break;
                            case 9:
                                host.log.message.println("That SubServer cannot start while these server(s) are running:", data.getRawString("m").split(":\\s")[1]);
                                break;
                            case 0:
                            case 1:
                                host.log.message.println("Server was started successfully");
                                break;
                            default:
                                host.log.warn.println("PacketStartServer(null, " + args[0] + ") responded with: " + data.getRawString("m"));
                                host.log.message.println("Server was started successfully");
                                break;
                        }
                    }));
                } else {
                    host.log.message.println("Usage: /" + handle + " <SubServer>");
                }
            }
        }.usage("<SubServer>").description("Starts a SubServer").help(
                "This command is used to start a SubServer on the network.",
                "Once it has been started, you can control it via the other commands",
                "",
                "The <SubServer> argument is required, and should be the name of",
                "the SubServer you want to start.",
                "",
                "Example:",
                "  /start ExampleServer"
        ).register("start");
        new Command(host.info) {
            @Override
            public void command(String handle, String[] args) {
                if (args.length > 0) {
                    host.subdata.sendPacket(new PacketStopServer(null, args[0], false, data -> {
                        switch (data.getInt("r")) {
                            case 3:
                                host.log.message.println("There is no server with that name");
                                break;
                            case 4:
                                host.log.message.println("That Server is not a SubServer");
                                break;
                            case 5:
                                host.log.message.println("That SubServer is not running");
                                break;
                            case 0:
                            case 1:
                                host.log.message.println("Server was stopped successfully");
                                break;
                            default:
                                host.log.warn.println("PacketStopServer(null, " + args[0] + ", false) responded with: " + data.getRawString("m"));
                                host.log.message.println("Server was stopped successfully");
                                break;
                        }
                    }));
                } else {
                    host.log.message.println("Usage: /" + handle + " <SubServer>");
                }
            }
        }.usage("<SubServer>").description("Stops a SubServer").help(
                "This command is used to request a SubServer to stop via the network.",
                "Stopping a SubServer in this way will run the stop command",
                "specified in the server's configuration",
                "",
                "The <SubServer> argument is required, and should be the name of",
                "the SubServer you want to stop.",
                "",
                "Example:",
                "  /stop ExampleServer"
        ).register("stop");
        new Command(host.info) {
            @Override
            public void command(String handle, String[] args) {
                if (args.length > 0) {
                    host.subdata.sendPacket(new PacketStopServer(null, args[0], true, data -> {
                        switch (data.getInt("r")) {
                            case 3:
                                host.log.message.println("There is no server with that name");
                                break;
                            case 4:
                                host.log.message.println("That Server is not a SubServer");
                                break;
                            case 5:
                                host.log.message.println("That SubServer is not running");
                                break;
                            case 0:
                            case 1:
                                host.log.message.println("Server was terminated successfully");
                                break;
                            default:
                                host.log.warn.println("PacketStopServer(null, " + args[0] + ", true) responded with: " + data.getRawString("m"));
                                host.log.message.println("Server was terminated successfully");
                                break;
                        }
                    }));
                } else {
                    host.log.message.println("Usage: /" + handle + " <SubServer>");
                }
            }
        }.usage("<SubServer>").description("Terminates a SubServer").help(
                "This command is used to forcefully stop a SubServer on the network.",
                "Stopping a SubServer in this way can make you lose unsaved data though,",
                "so it is generally recommended to use this command only when it stops responding.",
                "",
                "The <SubServer> argument is required, and should be the name of",
                "the SubServer you want to terminate.",
                "",
                "Example:",
                "  /kill ExampleServer"
        ).register("kill", "terminate");
        new Command(host.info) {
            @Override
            public void command(String handle, String[] args) {
                if (args.length > 1) {
                    int i = 1;
                    String str = args[1];
                    if (args.length > 2) {
                        do {
                            i++;
                            str = str + " " + args[i];
                        } while ((i + 1) != args.length);
                    }
                    final String cmd = str;
                    host.subdata.sendPacket(new PacketCommandServer(null, args[0], cmd, data -> {
                        switch (data.getInt("r")) {
                            case 3:
                                host.log.message.println("There is no server with that name");
                                break;
                            case 4:
                                host.log.message.println("That Server is not a SubServer");
                                break;
                            case 5:
                                host.log.message.println("That SubServer is not running");
                                break;
                            case 0:
                            case 1:
                                host.log.message.println("Command was sent successfully");
                                break;
                            default:
                                host.log.warn.println("PacketCommandServer(null, " + args[0] + ", /" + cmd + ") responded with: " + data.getRawString("m"));
                                host.log.message.println("Command was sent successfully");
                                break;
                        }
                    }));
                } else {
                    host.log.message.println("Usage: /" + handle + " <SubServer> <Command> [Args...]");
                }
            }
        }.usage("<SubServer>", "<Command>", "[Args...]").description("Sends a Command to a SubServer").help(
                "This command is used to send a command to a SubServer's Console via the network.",
                "",
                "The <SubServer> argument is required, and should be the name of",
                "the SubServer you want to send a command to.",
                "",
                "The <Command> argument is required, and should be the command you",
                "want to send, the following [Args...] will be passed to that command.",
                "",
                "Examples:",
                "  /cmd ExampleServer help",
                "  /cmd ExampleServer say Hello World!"
        ).register("cmd", "command");
        new Command(host.info) {
            @Override
            public void command(String handle, String[] args) {
                if (args.length > 3) {
                    if (args.length > 4 && Util.isException(() -> Integer.parseInt(args[4]))) {
                        host.log.message.println("Invalid Port Number");
                    } else {
                        host.subdata.sendPacket(new PacketCreateServer(null, args[0], args[1], args[2], new Version(args[3]), (args.length > 4)?Integer.parseInt(args[4]):null, data -> {
                            switch (data.getInt("r")) {
                                case 3:
                                    host.log.message.println("Server names cannot use spaces");
                                case 4:
                                    host.log.message.println("There is already a SubServer with that name");
                                    break;
                                case 5:
                                    host.log.message.println("There is no host with that name");
                                    break;
                                case 6:
                                    host.log.message.println("That Host is not available");
                                    break;
                                case 7:
                                    host.log.message.println("That Host is not enabled");
                                    break;
                                case 8:
                                    host.log.message.println("There is no template with that name");
                                    break;
                                case 9:
                                    host.log.message.println("That Template is not enabled");
                                    break;
                                case 10:
                                    host.log.message.println("SubCreator cannot create servers before Minecraft 1.8");
                                    break;
                                case 11:
                                    host.log.message.println("Invalid Port Number");
                                    break;
                                case 0:
                                case 1:
                                    host.log.message.println("Launching SubCreator...");
                                    break;
                                default:
                                    host.log.warn.println("PacketCreateServer(null, " + args[0] + ", " + args[1] + ", " + args[2] + ", " + args[3] + ", " + ((args.length > 4)?args[4]:"null") + ") responded with: " + data.getRawString("m"));
                                    host.log.message.println("Launching SubCreator...");
                                    break;
                            }
                        }));
                    }
                } else {
                    host.log.message.println("Usage: /" + handle + " <Name> <Host> <Template> <Version> <Port>");
                }
            }
        }.usage("<Name>", "<Host>", "<Template>", "<Version>", "[Port]").description("Creates a SubServer").help(
                "This command is used to create and launch a SubServer on the specified host via the network.",
                "Templates are downloaded from SubServers.Bungee to ~/Templates.",
                "",
                "The <Name> argument is required, and should be the name of",
                "the SubServer you want to create.",
                "",
                "The <Host> argument is required, and should be the name of",
                "the host you want to the server to run on.",
                "",
                "The <Template> argument is required, and should be the name of",
                "the template you want to create your server with.",
                "",
                "The <Version> argument is required, and should be a version",
                "string of the type of server that you want to create",
                "",
                "When the <Port> argument is provided, it will set the port number",
                "the server will listen on after it has been created.",
                "",
                "Examples:",
                "  /create ExampleServer ExampleHost Spigot 1.13.1",
                "  /create ExampleServer ExampleHost Spigot 1.13.1 25565"
        ).register("create");
    }
}
