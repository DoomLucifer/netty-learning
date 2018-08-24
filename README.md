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

> 用于描述从Channel中读取或者写入到Channel的操作。

分散（scatter）：从Channel中读取数据写入到多个Buffer中。

聚集（gather）：将多个Buffer中的数据写入同一个Channel。

