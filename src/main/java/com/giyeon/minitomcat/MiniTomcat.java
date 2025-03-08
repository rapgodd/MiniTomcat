package com.giyeon.minitomcat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class MiniTomcat {


    public static void main(String[] args) throws IOException {

        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(9000));

        Selector selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("Server started on port 9000");

        while (true) {
            selector.select();

            Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                keyIterator.remove();

                try {

                    if (key.isAcceptable()) {
                        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                        SocketChannel clientChannel = ssc.accept();
                        clientChannel.configureBlocking(false);


                        clientChannel.register(selector, SelectionKey.OP_READ);
                        System.out.println("client connected : " + clientChannel.getRemoteAddress());
                    }

                    else if (key.isReadable()) {
                        SocketChannel clientChannel = (SocketChannel) key.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(1024);

                        int bytesRead = clientChannel.read(buffer);
                        if (bytesRead > 0) {
                            buffer.flip();
                            String msg = new String(buffer.array(), 0, bytesRead);
                            System.out.println("client request : " + msg);


                            String response = "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: text/plain\r\n" +
                                    "Content-Length: 5\r\n" +
                                    "Connection: close\r\n"+
                                    "\r\n" +
                                    "hello";


                            byte[] responseBytes = response.getBytes();
                            ByteBuffer responseBuffer = ByteBuffer.wrap(responseBytes);
                            clientChannel.write(responseBuffer);
                            clientChannel.close();
                            System.out.println("disconnected");

                        }
                    }
                } catch (IOException e) {
                    key.channel().close();
                    key.cancel();
                    System.out.println("disconnected");
                }
            }
        }

    }
}
