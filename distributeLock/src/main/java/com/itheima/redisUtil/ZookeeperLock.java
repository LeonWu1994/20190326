package com.itheima.redisUtil;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;

public class ZookeeperLock {

    public static void main(String[] args) throws Exception{

        RetryPolicy retryPlicy = new ExponentialBackoffRetry(1000,3,3000);
        CuratorFramework client = CuratorFrameworkFactory.newClient("192.168.25.131:2181,192.168.25.132:2181,192.168.25.133:2181", retryPlicy);
        client.start();
        //创建锁 . 根节点/lock
        InterProcessMutex interProcessMutex = new InterProcessMutex(client,"/lock");
        interProcessMutex.acquire();
        //获取到了锁
        //执行业务逻辑
        //释放锁
        interProcessMutex.release();
        client.close();
    }
}
