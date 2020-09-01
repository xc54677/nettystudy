package com.mashibing.nettystudy.s07;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;

public class Server {

    public static final ChannelGroup clients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public static void main(String[] args) {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup(2);

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            ChannelFuture channelFuture = bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new ServerChildHandler());
                        }
                    }).bind(8888)
            .sync();

            System.out.println("Server started.....!");

            channelFuture.channel().closeFuture().sync();  //close() --> ChannelFuture
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }


    }
}

class ServerChildHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Server.clients.add(ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = null;
        try {
            buf = (ByteBuf) msg;
            byte[] bytes =  new byte[buf.readableBytes()];
            buf.getBytes(buf.readerIndex(), bytes);
            System.out.println(new String(bytes));

//            System.out.println(buf);
//            System.out.println(buf.refCnt());

            Server.clients.writeAndFlush(msg); //将数据写到多个client端
//            ctx.writeAndFlush(msg); //将数据顺着channel通道写回去client端
        }finally {
            /*if (buf != null && buf.refCnt()!=0){
                ReferenceCountUtil.release(buf);  //如果server端只读不往client端回写，就要手动释放； 如果回写端话就不需要释放。
            }*/
//            System.out.println(buf.refCnt());
        }

    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}


