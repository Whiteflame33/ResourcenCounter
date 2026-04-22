package de.whiteflame.rescount.api.io.network;

import java.io.IOException;
import java.util.List;


public interface INetworkClient {
    boolean send(byte[] data) throws IOException;
    List<byte[]> getNewMessages();
    void rediscover() throws IOException;
    void close() throws IOException;
}
