package com.github.wmhxx.application.api.wechat.config.redis;

import org.springframework.data.redis.core.ZSetOperations;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * redis操作Service
 *
 * @author wmhxx
 * @date 2022/08/09 14:46:45
 */
public interface RedisService {

    /**
     * 保存属性
     */
    void set(String key, Object value, long time);


    /**
     * 保存属性
     */
    void set(String key, Object value, long time, TimeUnit timeUnit);

    /**
     * 保存属性
     */
    void set(String key, Object value);

    /**
     * 获取属性
     */
    Object get(String key);

    /**
     * 删除属性
     */
    Boolean del(String key);

    /**
     * 批量删除属性
     */
    Long del(Collection<String> keys);

    /**
     * 设置过期时间
     */
    Boolean expire(String key, long time);

    /**
     * 设置过期时间
     */
    Boolean expire(String key, long time, TimeUnit timeUnit);

    /**
     * 获取过期时间
     */
    Long getExpire(String key);

    /**
     * 判断是否有该属性
     */
    Boolean hasKey(String key);


    /**
     * 模糊查询redisKey (后置)
     *
     * @param key 键
     * @return {@link Set}<{@link String}>
     */
    Set<String> keysRight(String key);

    /**
     * 模糊查询redisKey (前置)
     *
     * @param key 键
     * @return {@link Set}<{@link String}>
     */
    Set<String> keysLeft(String key);


    /**
     * 按delta递增
     */
    Long incr(String key, long delta);

    /**
     * 按delta递增 double
     */
    Double incr(String key, double delta);

    /**
     * 按delta递增 double
     */
    Double incr(String key, double delta, Long time);

    /**
     * 按delta递减
     */
    Long decr(String key, long delta);

    /**
     * 获取Hash结构中的属性
     */
    Object hGet(String key, String hashKey);

    /**
     * 获取Hash结构中的属性
     */
    Object hGet(String key);

    /**
     * 向Hash结构中放入一个属性
     */
    Boolean hSet(String key, String hashKey, Object value, long time);

    /**
     * 向Hash结构中放入一个属性
     */
    Boolean hSet(String key, String hashKey, Object value, long time, TimeUnit timeUnit);

    /**
     * 向Hash结构中放入一个属性
     */
    void hSet(String key, String hashKey, Object value);

    /**
     * 直接获取整个Hash结构
     */
    Map<Object, Object> hGetAll(String key);

    /**
     * 直接设置整个Hash结构
     */
    Boolean hSetAll(String key, Map<String, Object> map, long time);

    /**
     * 直接设置整个Hash结构
     */
    void hSetAll(String key, Map<String, ?> map);

    /**
     * 删除Hash结构中的属性
     */
    void hDel(String key, Object... hashKey);

    /**
     * 删除Hash结构中的属性
     */
    void hDel(String key);

    /**
     * 判断Hash结构中是否有该属性
     */
    Boolean hHasKey(String key, String hashKey);

    /**
     * Hash结构中属性递增
     */
    Long hIncr(String key, String hashKey, Long delta);

    /**
     * Hash结构中属性递增
     */
    Double hIncr(String key, String hashKey, double delta);


    /**
     * hash中key的数量
     *
     * @param key 关键
     * @return {@link Long}
     */
    Long hSize(String key);

    /**
     * Hash结构中属性递减
     */
    Long hDecr(String key, String hashKey, Long delta);

    /**
     * 获取Set结构
     */
    Set<Object> sMembers(String key);

    /**
     * 向Set结构中添加属性
     */
    Long sAdd(String key, Object... values);

    /**
     * 向Set结构中添加属性
     */
    Long sAdd(String key, Object values, long time, TimeUnit timeUnit);

    /**
     * 向Set结构中添加属性
     */
    Long sAddExp(String key, long time, Object... values);

    /**
     * 是否为Set中的属性
     */
    Boolean sIsMember(String key, Object value);

    /**
     * 获取Set结构的长度
     */
    Long sSize(String key);

    /**
     * 删除Set结构中的属性
     */
    Long sRemove(String key, Object... values);

    /**
     * 获取List结构中的属性
     */
    List<Object> lRange(String key, long start, long end);

    /**
     * 获取List结构中的所有属性
     */
    List<Object> lRangeAll(String key);

    /**
     * 根据key获取Set中的所有值
     *
     * @param key 关键
     * @return {@link Set}<{@link Object}>
     */
    Set<Object> sGet(String key);

    /**
     * 获取List结构的长度
     */
    Long lSize(String key);

    /**
     * 移除并获取列表最后一个元素
     *
     * @param key 关键
     * @return {@link String}
     */
    String lRightPop(String key);

    /**
     * 根据索引获取List中的属性
     */
    Object lIndex(String key, long index);

    /**
     * 向List结构中添加属性
     */
    Long lPush(String key, Object value);

    /**
     * 向List结构中添加属性
     */
    Long lPush(String key, Object value, long time);

    /**
     * 向List结构中批量添加属性
     */
    Long lPushAll(String key, Object... values);

    /**
     * 向List结构中批量添加属性
     */
    Long lPushAll(String key, Long time, Object... values);

    /**
     * 从List结构中移除属性
     */
    Long lRemove(String key, long count, Object value);

    /**
     * 裁剪list
     *
     * @param key   redisKey
     * @param start 开始
     * @param end   结束
     */
    void lTrim(String key, long start, long end);

    /**
     * 获取所有的key，value
     *
     * @return 键值map
     */
    Set<String> getAllKeyValues(String key);


    /*------------------zSet相关操作--------------------------------*/

    /**
     * 添加元素,有序集合是按照元素的score值由小到大排列
     *
     * @param key   redisKey
     * @param value redisValue
     * @param score score
     * @return java.lang.Boolean
     */
    Boolean zAdd(String key, String value, double score);


    /**
     * 添加元素,有序集合是按照元素的score值由小到大排列
     *
     * @param key      redisKey
     * @param value    redisValue
     * @param score    score
     * @param time     时间
     * @param timeUnit 时间单位
     * @return java.lang.Boolean
     */
    Boolean zAdd(String key, String value, double score, long time, TimeUnit timeUnit);

    /**
     * 增加元素的score值，并返回增加后的值
     *
     * @param key   redisKey
     * @param value redisValue
     * @param delta score
     * @return java.lang.Double
     */
    Double zIncrementScore(String key, String value, double delta);

    /**
     * 获取集合的元素, 从大到小排序, 并返回score值
     *
     * @param key   redisKey
     * @param start 开始
     * @param end   结束
     * @return java.util.Set
     */
    Set<ZSetOperations.TypedTuple<Object>> zReverseRangeWithScores(String key, long start, long end);

}
