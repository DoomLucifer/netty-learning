# Netty-Learning

[TOC]

## NIO

### NIO概述

- 核心组成部分
  1. Channels
  2. Buffers
  3. Selectors
- Channels and Buffers

> 数据可以从Channel读到Buffer中，也可以从Buffer写到Channel中

- Selector

> Selector允许单线程处理多个Channel。要使用Selector，需要向Selector注册Channel，然后调用它的select()方法，这个方法会一直阻塞到某个注册的通道有事件就绪。一旦这个方法返回，线程就可以处理这些事件，如连接已建立，数据接收等事件。

### Channel

- NIO中的通道类似于流，但有些不同：

1. 既可以从通道中读取数据，又可以写数据到通道。但流的读写通常是单向的。
2. 通道可以异步的读写。
3. 通道中的数据总是要先读到一个Buffer，或者总是要从一个Buffer中写入。

- Channel的实现

1. FileChannel  从文件中读写数据
2. DatagramChannel 通过UDP读写网络中的数据
3. SocketChannel 通过TCP读写网络中的数据
4. ServerSocketChannel 监听新进来的TCP连接，像WEB服务一样，对每一个新进来的连接都会创建一个SocketChannel。

### Buffer

> 缓冲区本质上就是一块可以写入数据，然后可以从中读取数据的内存。这块内存被包装成NIO Buffer对象，并提供了一组方法，用来访问这块内存

- Buffer的基本用法

1. 写入数据到Buffer
2. 调用flip（）方法
3. 从Buffer中读取数据
4. 调用clear（）方法或者compact（）方法

> flip()方法将Buffer从写模式切换到读模式

- Buffer的capacity，position，limit

![Buffer的写模式与读模式](https://github.com/garaiya/netty-learning/blob/8fc87b3af0fa43be732eb19aa70ec4d4e2acc450/images/buffers-modes.png?raw=true)

> capacity：Buffer缓冲区的容量
>
> position：写模式下表示下一个可插入数据的位置，读模式下表示下一个可读取数据的位置。
>
> 详细流程：当写数据到Buffer中时，position表示当前的位置。初始的position值为0，当一个byte、long、char等数据写到Buffer后，position会向前移动到下一个可插入数据的Buffer单元。position的最大值capacity-1；当将Buffer从写模式切换到读模式，position会被重置为0。当从Buffer的position处读取数据时，position向前移动到下一个可读取的位置。
>
> limit：写模式下，Buffer的limit表示最多能写多少数据。写模式下limit等于Buffer的capacity。读模式下，limit表示最多能读多少数据，因此当切换到读模式时，limit会被设置成写模式下的position值。也就是说，能读到之前写入的所有数据。

- Buffer的类型

1. ByteBuffer
2. CharBuffer
3. IntBuffer
4. ShortBuffer
5. LongBuffer
6. FloatBuffer
7. DoubleBuffer
8. MappedByteBuffer

- Buffer的分配

```java
//分配一个48字节容量大小的缓冲区
ByteBuffer buf = ByteBuffer.allocate(48);
//分配一个1024个字符大小的缓冲区
CharBuffer buf = CharBuffer.allocate(1024);
```

- 向Buffer中写数据

1. 从Channel写到Buffer
2. 通过Buffer的put（）方法写到Buffer里

```java
//Channel读取数据到Buffer
int bytesRead = inChannel.read(buf);
//通过put方法写Buffer
buf.put(127);
```

- 从Buffer中读取数据

1. 从Buffer读取数据到Channel
2. 使用get（）方法从Buffer中读取数据

```java
//Buffer读取数据到Channel
int bytesWritten = inChannel.write(buf);
//get方法读取数据
byte aByte = buf.get();
```

- rewind（）方法

> 将position设回0，所以你可以重读Buffer中的所有数据。limit保持不变，仍然表示能从Buffer中读取多少个元素（byte、char等） 

- clear（）与compact（）方法

> clear()方法：position将被设回0，limit被设置成 capacity的值。换句话说，Buffer 被清空了。Buffer中的数据并未清除，只是这些标记告诉我们可以从哪里开始往Buffer里写数据 。如果Buffer中有一些未读的数据，调用clear()方法，数据将“被遗忘”，意味着不再有任何标记会告诉你哪些数据被读过，哪些还没有 。如果Buffer中仍有未读的数据，且后续还需要这些数据，但是此时想要先先写些数据，那么使用compact()方法。 
>
> compact()方法：将所有未读的数据拷贝到Buffer起始处。然后将position设到最后一个未读元素正后面。limit属性依然像clear()方法一样，设置成capacity。现在Buffer准备好写数据了，但是不会覆盖未读的数据 

- mark（）与reset（）方法

> 通过调用Buffer.mark()方法，可以标记Buffer中的一个特定position。之后可以通过调用Buffer.reset()方法恢复到这个position 

- equals（）与compartTo（）方法

### Scatter/Gather

> scatter/gather用于描述从Channel中读取或者写入到Channel的操作

- 分散（scatter）

> Channel将从Channel中读取的数据分散到多个Buffer中去

- 聚集（gather）

> Channel将多个Buffer中的数据聚集到Channel中，也就是写入到Channel

**scatter / gather经常用于需要将传输的数据分开处理的场合，例如传输一个由消息头和消息体组成的消息，你可能会将消息体和消息头分散到不同的buffer中，这样你可以方便的处理消息头和消息体。 **

- 分散读（Scattering Reads）

```java
ByteBuffer header = ByteBuffer.allocate(128);
ByteBuffer body = ByteBuffer.allocate(1024);
ByteBuffer[] bufferArray = {header,body};
channel.read(bufferArray);
//注意：buffer首先被插入到数组，然后再将数组作为channel.read() 的输入参数。read()方法按照buffer在数组中的顺序将从channel中读取的数据写入到buffer，当一个buffer被写满后，channel紧接着向另一个buffer中写。
//Scattering Reads在移动下一个buffer前，必须填满当前的buffer，这也意味着它不适用于动态消息(译者注：消息大小不固定)。换句话说，如果存在消息头和消息体，消息头必须完成填充（例如 128byte），Scattering Reads才能正常工作。
```

- 聚集写（Gathering Writes）

```java
ByteBuffer header = ByteBuffer.allocate(128);
ByteBuffer body = ByteBuffer.allocate(1024);
ByteBuffer[] bufferArray = {header,body};
channel.write(bufferArray);
//buffers数组是write()方法的入参，write()方法会按照buffer在数组中的顺序，将数据写入到channel，注意只有position和limit之间的数据才会被写入。因此，如果一个buffer的容量为128byte，但是仅仅包含58byte的数据，那么这58byte的数据将被写入到channel中。因此与Scattering Reads相反，Gathering Writes能较好的处理动态消息。
```

### 通道之间传输（Channel to Channel Transfers）

- transferFrom

> FileChannel.transferFrom()方法可以将数据从源通道传输到FileChannel中 

```java
RandomAccessFile fromFile = new RandomAccessFile("fromFile.txt","rw");
FileChannel fromChannel = fromFile.getChannel();

RandomAccessFile toFile = new RandomAccessFile("toFile.txt","rw");
FileChannel toChannel = toFile.getChannel();

long position = 0;
long count = fromChannel.size();

toChannel.transferFrom(position,count,fromChannel);
//方法的输入参数position表示从position处开始向目标文件写入数据，count表示最多传输的字节数。如果源通道的剩余空间小于 count 个字节，则所传输的字节数要小于请求的字节数。
//此外要注意，在SoketChannel的实现中，SocketChannel只会传输此刻准备好的数据（可能不足count字节）。因此，SocketChannel可能不会将请求的所有数据(count个字节)全部传输到FileChannel中。
```

- transferTo

> transferTo方法将数据从FileChannel传输到其他的Channel中去

```java
RandomAccessFile fromFile = new RandomAccessFile("fromFile.txt", "rw");
FileChannel      fromChannel = fromFile.getChannel();
 
RandomAccessFile toFile = new RandomAccessFile("toFile.txt", "rw");
FileChannel      toChannel = toFile.getChannel();

long position = 0;
long count = fromChannel.size();
 
fromChannel.transferTo(position, count, toChannel);
```

### Selector

