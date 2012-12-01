package com.facebook.swift.service.async;

import com.facebook.nifty.client.HttpClientChannel;
import com.facebook.nifty.client.NiftyClient;
import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.service.ThriftClient;
import com.facebook.swift.service.ThriftClientConfig;
import com.facebook.swift.service.ThriftClientManager;
import com.facebook.swift.service.ThriftServer;
import com.facebook.swift.service.ThriftServerConfig;
import com.facebook.swift.service.ThriftServiceProcessor;
import com.google.common.base.Function;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.airlift.units.DataSize;
import io.airlift.units.Duration;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class AsyncTestBase
{
    public static final int MAX_FRAME_SIZE = 0x3fffffff;
    protected ThriftCodecManager codecManager;
    protected ThriftClientManager clientManager;

    protected <T> ListenableFuture<T> createClient(Class<T> clientClass, ThriftServer server)
            throws TTransportException, InterruptedException, ExecutionException
    {
        return createClient(clientClass, server.getPort());
    }

    protected <T> ListenableFuture<T> createClient(Class<T> clientClass, int serverPort)
            throws TTransportException, InterruptedException, ExecutionException
    {
        HostAndPort address = HostAndPort.fromParts("localhost", serverPort);
        ThriftClientConfig config = new ThriftClientConfig().setConnectTimeout(new Duration(1, TimeUnit.SECONDS))
                                                            .setReadTimeout(new Duration(1, TimeUnit.SECONDS))
                                                            .setWriteTimeout(new Duration(1, TimeUnit.SECONDS));
        return new ThriftClient<>(clientManager, clientClass, config, "asyncTestClient").open(address);
    }

    protected <T> ListenableFuture<T> createHttpClient(Class<T> clientClass, int serverPort)
            throws TTransportException, InterruptedException, ExecutionException
    {
        HostAndPort address = HostAndPort.fromParts("localhost", serverPort);
        ThriftClientConfig config = new ThriftClientConfig().setConnectTimeout(new Duration(1, TimeUnit.SECONDS))
                                                            .setReadTimeout(new Duration(1, TimeUnit.SECONDS))
                                                            .setWriteTimeout(new Duration(1, TimeUnit.SECONDS));
        HttpClientChannel.Factory channelFactory =
                new HttpClientChannel.Factory("localhost:4567", "/thrift/");
        return new ThriftClient<>(clientManager, clientClass, channelFactory, config, "asyncTestClient").open(address);
    }

    protected ThriftServer createAsyncServer()
            throws InstantiationException, IllegalAccessException, TException
    {
        DelayedMapAsyncHandler handler = new DelayedMapAsyncHandler();
        handler.putValueSlowly(0, TimeUnit.MILLISECONDS, "testKey", "default");
        return createServerFromHandler(handler);
    }

    protected ThriftServer createServerFromHandler(Object handler)
            throws IllegalAccessException, InstantiationException
    {
        ThriftServiceProcessor processor = new ThriftServiceProcessor(codecManager, handler);
        ThriftServerConfig config = new ThriftServerConfig();
        config.setMaxFrameSize(new DataSize(MAX_FRAME_SIZE, DataSize.Unit.BYTE));

        return new ThriftServer(processor, config).start();
    }
}