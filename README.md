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
//get方法有很多版本，允许你以不同的方式从Buffer中读取数据。例如，从指定position读取，或者从Buffer中读取数据到字节数组。更多Buffer实现的细节参考JavaDoc。
byte aByte = buf.get();
```

- rewind（）方法
  Buffer.rewind()方法重置position为0，所以可以重读buffer中的所有数据。limit保持不变，仍然表示能从Buffer中读多少个元素（byte、char等）

- clear（）and compact（）

一旦读完Buffer中的数据，需要让Buffer准备好再次被写入。可以通过clear()或compact()方法来完成。

> clear()方法：将position设置成0，limit设置成capacity。也就是说Buffer可以再次被写入了，但是之前的数据没有被清空，只是标记被重置。如果还有没读取完的数据，调用clear方法后，这些数据将“被遗忘”，意味着不再有任何标记会告诉你哪些数据被读过，哪些还没读。
>
> compact()方法：如果有未读的数据，并且一会在读，但是现在要做写入操作，这是调用该方法来代替clear方法。compact方法将所有未读的数据移动到Buffer的开始处，然后将position设置成未读数据的下一个位置，limit仍然设置成capacity。现在再往Buffer里写入数据将不会覆盖未读的数据。

- mark() and reset()

通过调用Buffer.mark()方法，可以标记Buffer中的一个特定position。之后可以通过调用Buffer.reset()方法恢复到这个position

```java
buffer.mark();
//call buffer.get() a couple of times
buffer.reset(); //set position back to mark
```

- equals()	and compareTo()

用于比较两个Buffers

**equals()**

满足下列条件时，表示两个Buffer相等：

1. 相同的类型（byte、char、int等）
2. Buffer中剩余的byte、char等的数量相同
3. Buffer中剩余的byte、char等是相同的

equals方法只比较Buffer中的剩余元素。

**compareTo()**

compareTo方法比较两个Buffer中的剩余元素，如果满足下列条件，则认为一个Buffer"小于"另一个Buffer：

1. 第一个不相等的元素小于另一个buffer中对应的元素
2. 所有元素都想等，但第一个Bufer比另一个Buffer先读完（第一个Buffer的元素个数比另一个少）。

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

toChannel.transferFrom(fromChannel,position,count);
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

> Selector是一个能够监听多个NIO通道的NIO组件，它知道哪个通道已经准备好了读或者写的事件。单线程的Selector可以管理多个通道，从而管理多个网络连接。

- Why User a Selector？

> 为什么使用Selector？Selector单线程监听多个通道的好处是使用更少的线程，更少的线程带来的直接的好处就是减少线程上下文的切换，而线程的切换对于操作系统来说是一笔昂贵的开销，并且每个线程也会占用一定的内存资源，因此使用的线程越少越好。

单线程Selector管理3个通道插图

- Creating a Selector

```java
Selector selector = Selector.open();
```

- Registering Channels With the Selector（向Selector注册通道）

```java
channel.configureBlocking(false);
SelectionKey key = channel.register(selector,SelectionKey.OP_READ);
```

通道必须是非阻塞模式才能与Selector一起使用，这意味着不能使用FileChannel，因为FileChannel无法转换成非阻塞模式。Socket通道是可以的。

register()方法的第二个参数表示Selector要监听通道的哪个事件，总共包括以下四个事件：

1. Connect
2. Accept
3. Read
4. Write

通道触发了一个事件意思是该事件已经就绪。一个通道已经成功连接到服务器表示"连接就绪；一个ServerSocketChannel接受了incoming connection表示"接收就绪"；一个有数据可读的通道可以说是"读就绪"；等待写数据的通道可以说是"写就绪"。

这四个事件由这四个常量key表示：

1. SelectionKey.OP_CONNECT 
2. SelectionKey.OP_ACCEPT
3. SelectionKey.OP_READ
4. SelectionKey.OP_WRITE

```java
//监听多个事件
int interestSet = SelectionKey.OP_READ | SelectionKey.OP_WRITE;    
```

- ## SelectionKey's

SelectionKey包含以下几个属性：

1. The interest set
2. The ready set
3. The Channel
4. The Selector
5. An attached object (optional)

- interest集合

> interest集合是你所选择的的感兴趣的事件集合。可以通过SelectionKey读写interest集合：

```java
int interestSet = selectionKey.interestOps();

boolean isInterestedInAccept  = (interestSet & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT；
boolean isInterestedInConnect = interestSet & SelectionKey.OP_CONNECT;
boolean isInterestedInRead    = interestSet & SelectionKey.OP_READ;
boolean isInterestedInWrite   = interestSet & SelectionKey.OP_WRITE;
```

可以看到，用“位与”操作interest 集合和给定的SelectionKey常量，可以确定某个确定的事件是否在interest 集合中。 

- ready集合

ready集合是通道已经准备就绪的操作的集合。在一次选择（Selection）之后，你会首先访问这个ready set。可以这样访问ready集合：

```java
int readySet = selectionKey.readyOps();0
```

可以像检测interest集合那样的方法，来检测channel中什么事件或操作已经就绪。也可以使用一下四个方法，它们返回一个布尔类型：

```java
selectionKey.isAcceptable();
selectionKey.isConnectable();
selectionKey.isReadable();
selectionKey.isWritable();
```

- Channel + Selector

从SelectionKey访问Channel和Selector很简单。如下：

```java
Channel channel = selectionKey.channel();
Selector selector = selectionKey.selector();
```

- 附件的对象

可以将一个对象或者更多信息附着到SelectionKey上，这样就能方便的识别某个给定的通道。例如，可以附加 与通道一起使用的Buffer，或是包含聚集数据的某个对象。使用方法如下：

```java
selectionKey.attach(theObject);
Object attachedObj = selectionKey.attachment();
```

还可以在用register()方法向Selector注册Channel的时候附加对象。如：

```java
SelectionKey key = channel.register(selector,SelectionKey.OP_READ,theObject);
```

- 通过Selector选择通道

一旦向Selector注册了一或多个通道，就可以调用几个重载的select()方法。这些方法返回你所感兴趣的事件（如连接、接受、读或写）已经准备就绪的那些通道。换句话说，如果你对“读就绪”的通道感兴趣，select()方法会返回读事件已经就绪的那些通道。

下面是select()方法：

```java
int select();
int select(long timeout);
int selectNow();
```

select() 阻塞到至少有一个通道在你注册的事件上就绪了。

select(long timeout)和select()一样，除了最长会阻塞timeout毫秒(参数)。

selectNow()不会阻塞，不管什么通道就绪都立刻返回（译者注：此方法执行非阻塞的选择操作。如果自从前一次选择操作后，没有通道变成可选择的，则此方法直接返回零。）。

select()方法返回的int值表示有多少通道已经就绪。亦即，自上次调用select()方法后有多少通道变成就绪状态。如果调用select()方法，因为有一个通道变成就绪状态，返回了1，若再次调用select()方法，如果另一个通道就绪了，它会再次返回1。如果对第一个就绪的channel没有做任何操作，现在就有两个就绪的通道，但在每次select()方法调用之间，只有一个通道就绪了。

- selectedKeys(）

一旦调用了select()方法，并且返回值表明有一个或更多个通道就绪了，然后可以通过调用selector的selectedKeys()方法，访问“已选择键集（selected key set）”中的就绪通道。如下所示：

```java
Set selectedKeys = selector.selectedKeys();
```

当像Selector注册Channel时，Channel.register()方法会返回一个SelectionKey 对象。这个对象代表了注册到该Selector的通道。可以通过SelectionKey的selectedKeySet()方法访问这些对象。

可以遍历这个已选择的键集合来访问就绪的通道。如下：

```java
Set selectedKeys = selector.selectedKeys();
Iterator keyIterator = selectedKeys.iterator();
while(keyIterator.hasNext()){
    SelectionKey key = keyIterator.next();
    if(key.isAcceptable()){
        //a connection was accepted by a ServerSocketChannel.
    }else if(key.isConnectable()){
        //a connection was established with a remote server.
    }else if(key.isReadable()){
        //a channel is ready for reading
    }else if(key.isWritable()){
        //a channel is ready for writing
    }
    keyIterator.remove();
}
```

这个循环遍历已选择键集中的每个键，并检测各个键所对应的通道的就绪事件。

注意每次迭代末尾的keyIterator.remove()调用。Selector不会自己从已选择键集中移除SelectionKey实例。必须在处理完通道时自己移除。下次该通道变成就绪时，Selector会再次将其放入已选择键集中。

SelectionKey.channel()方法返回的通道需要转型成你要处理的类型，如ServerSocketChannel或SocketChannel等。

- wakeUp()

某个线程调用select()方法后阻塞了，即使没有通道已经就绪，也有办法让其从select()方法返回。只要让其它线程在第一个线程调用select()方法的那个对象上调用Selector.wakeup()方法即可。阻塞在select()方法上的线程会立马返回。

如果有其它线程调用了wakeup()方法，但当前没有线程阻塞在select()方法上，下个调用select()方法的线程会立即“醒来（wake up）”。

- close()

用完Selector后调用其close()方法会关闭该Selector，且使注册到该Selector上的所有SelectionKey实例无效。通道本身并不会关闭。

- 完整的示例

这里有一个完整的示例，打开一个Selector，注册一个通道注册到这个Selector上(通道的初始化过程略去),然后持续监控这个Selector的四种事件（接受，连接，读，写）是否就绪。

```java
Selector selector = Selector.open();
channel.configureBlocking(false);
SelectionKey key = channel.register(selector,SelectionKey.OP_READ);
while(true){
    int readyChannels = selector.select();
    if(readyChannels == 0) continue;
    Set selectedKeys = selector.selectedKeys();
    Iterator keyIterator = selectedKeys.iterator();
    while(keyIterator.hasNext()){
        SelectionKey key = keyIterator.next();
        if(key.isAcceptable()){
            //a connection was accepted by a ServerSocketChannel.
        }else if(key.isConnectable()){
            //a connection was established with a remote server.
        }else if(key.isReadable()){
            //a channel is ready for reading
        }else if(key.isWritable()){
            //a channel is ready for writing
        }
        keyIterator.remove();
    }
}
```

