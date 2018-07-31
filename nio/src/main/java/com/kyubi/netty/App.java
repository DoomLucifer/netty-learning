package com.kyubi.netty;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
        try {
            RandomAccessFile file = new RandomAccessFile("E:/a.txt","rw");
            FileChannel fileChannel = file.getChannel();

            //从通道中读取数据到Buffer
            ByteBuffer buffer = ByteBuffer.allocate(48);
            int bytesRead = fileChannel.read(buffer);
            while (bytesRead != -1){
                System.out.println("Read " + bytesRead);
                //反转Buffer
                buffer.flip();

                while (buffer.hasRemaining()){
                    //从Buffer中读取数据
                    System.out.println(buffer.get());
                }

                buffer.clear();
                //再从通道中读取数据到Buffer
                bytesRead = fileChannel.read(buffer);
            }
            file.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
