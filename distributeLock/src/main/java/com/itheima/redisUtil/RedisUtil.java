package com.itheima.redisUtil;

import redis.clients.jedis.Jedis;

import java.util.Collections;

public class RedisUtil {

    private static final String LOCK_SUCCESS="OK";

    private static final String SET_IF_NOT_EXIT="NX";

    private static final String EXPIRE_TIME_TYPE="EX";

    private static final Long RELEASE_LOCK=1L;

    /**
     * 加锁
     * @param jedis redis客户端
     * @param lockKey 锁
     * @param clientId 客户端标识
     * @param expireTime 过期时间
     * @return 是否加锁成功
     */
    public static boolean getLock(Jedis jedis,String lockKey,String clientId,int expireTime){

        /**
         * key: redis中的标识, ->lockKey作为锁. 因为key在redis中是唯一的.
         * value: redis中key相对应的值. ->clientId., 就是为了满足分布式锁的唯一性.保证一个客户端只能操作自己的锁
         * nxxx: 何时会在redis中添加内容
         *      nx: 当前key不存在的话,执行set方法
         *      xx: 当前key存在的话,执行set方法
         * expx: 设置过期时间的单位
         *      ex: 秒
         *      px: 毫秒
         * time: 过期时长
         */
        String result = jedis.set(lockKey,clientId,SET_IF_NOT_EXIT,EXPIRE_TIME_TYPE,expireTime);

        if (LOCK_SUCCESS.equals(result)){
            //加锁成功
            return true;
        }
        //加锁失败
        return false;
    }

    /**
     * 错误案例一
     * @param jedis
     * @param lockKey
     * @param clientId
     * @param expireTime
     * @return
     */
    public static boolean getLockWrong1(Jedis jedis,String lockKey,String clientId,int expireTime){

        //当前加锁操作不具备原子性

        long result = jedis.setnx(lockKey,clientId);
        if (result == 1){
            //加锁成功 ..程序突然崩溃->死锁
            jedis.expire(lockKey,expireTime);
            return true;
        }
        return false;
    }

    public static boolean getLockWrong2(Jedis jedis,String lockKey,int expiretime){

        long expires = System.currentTimeMillis()+expiretime;
        String expiresStr = String.valueOf(expires);

        //如果当前锁不存在, 加锁成功
        if (jedis.setnx(lockKey,expiresStr) == 1){
            return true;
        }

        //锁已经存在, 获取这把锁的过期时间
        String redisExpireTime = jedis.get(lockKey);
        if (redisExpireTime != null && Long.valueOf(redisExpireTime)<System.currentTimeMillis()){
            //当前锁已经过期,重置这把锁的过期时间
            String keyOldExpireTime = jedis.getSet(lockKey, expiresStr);
            if (keyOldExpireTime != null && keyOldExpireTime.equals(redisExpireTime)){
                return true;
            }
        }
        return false;
    }

    /**
     * 解锁
     * @param jedis redis客户端
     * @param lockKey 锁
     * @param clientId 客户端标识
     * @return 是否解锁成功
     */
    public static boolean releaseLock(Jedis jedis,String lockKey,String clientId){
        String script = "if redis.call('get',KEYS[1]) == ARVG[1] then return redis.call('del',KEYS[1]) else return 0 end";
        Object result = jedis.eval(script, Collections.singletonList(lockKey), Collections.singletonList(clientId));

        if (RELEASE_LOCK.equals(result)){
            return true;
        }
        return false;
    }

    /**
     * 解锁错误案例一
     * @param jedis
     * @param lockKey
     */
    public static void releaseLockWrong1(Jedis jedis,String lockKey){

        jedis.del(lockKey);
    }

    /**
     * 解锁错误案例二
     * @param jedis redis客户端
     * @param lockKey 锁
     * @param clientId 客户端标识
     */
    public static void releaseLockWrong2(Jedis jedis,String lockKey,String clientId){

        if (clientId.equals(jedis.get(lockKey))){
            //同一个客户端
            jedis.del(lockKey);//当前该方法在调用的时候, 很有可能这把锁已经不属于当前的客户端.
        }
    }


}
