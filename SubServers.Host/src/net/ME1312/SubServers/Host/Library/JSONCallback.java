package net.ME1312.SubServers.Host.Library;

import org.json.JSONObject;

/**
 * JSON Callback Class
 */
public interface JSONCallback {
    /**
     * Run the Callback
     *
     * @param json JSON
     */
    void run(JSONObject json);
}
