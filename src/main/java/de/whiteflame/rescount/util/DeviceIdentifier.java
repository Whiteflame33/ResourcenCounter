package de.whiteflame.rescount.util;

import de.whiteflame.rescount.api.log.ILogger;
import de.whiteflame.rescount.api.log.LoggerFactory;

import java.net.InetAddress;

public final class DeviceIdentifier {
    private static final ILogger LOGGER = LoggerFactory.getLogger(DeviceIdentifier.class);

    public static String generateDeviceKey() {
        try {
            LOGGER.trace("Generating device key");
            InetAddress[] mac = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName());

            LOGGER.trace("Found {} MAC address devices that will be used in creating the key", mac.length);

            var sb = new StringBuilder();

            for (int i = 0; i < mac.length; i++) {
                sb.append("%02X%s".formatted(mac[i].hashCode(), (i < mac.length - 1) ? "-" : ""));
            }

            LOGGER.trace("Generated key = {}", sb.toString());

            return sb.toString();
        } catch (Exception e) {
            LOGGER.error("Failed to generate device key", e);
        }

        return "";
    }

    private DeviceIdentifier() {}
}
