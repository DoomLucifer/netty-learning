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



![Buffer的写模式与读模式](D:\IdeaProjects\garaiya\netty-learning\images\buffers-modes.png)

