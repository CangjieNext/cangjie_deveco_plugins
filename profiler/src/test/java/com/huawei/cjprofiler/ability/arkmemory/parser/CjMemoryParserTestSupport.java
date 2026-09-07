/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.ability.arkmemory.parser;

import com.huawei.deveco.insight.ohos.ability.arkmemory.entity.RawHeapSnapshot;
import com.huawei.deveco.insight.ohos.service.ark.RecordServiceBase.ParserStreamContext;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

import java.io.File;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * 供 CjMemoryParser 相关测试复用的支撑代码，集中处理三块样板逻辑：
 *
 * <ul>
 *   <li>反射调用 private/static 方法（CjMemoryParser 的多数解析入口不可见）；</li>
 *   <li>重置 CjMemoryParser 的 static 解析集合，避免跨测试污染；</li>
 *   <li>从 raw_cangjie_heap_snapshot*.data 提取并拼接 chunk 的两阶段解析。</li>
 * </ul>
 */
final class CjMemoryParserTestSupport {
    private CjMemoryParserTestSupport() {
    }

    /**
     * 反射调用 CjMemoryParser 的 private 方法并返回结果。
     *
     * @param methodName 目标 private 方法名
     * @param paramTypes 目标方法的参数类型集合
     * @param args       传给目标方法的实参集合
     * @return 目标方法的返回值
     * @throws Exception 反射调用或目标方法执行失败时抛出
     */
    static Object invokePrivate(String methodName, List<Class<?>> paramTypes, List<Object> args) throws Exception {
        Method method = CjMemoryParser.class.getDeclaredMethod(methodName, paramTypes.toArray(new Class<?>[0]));
        method.setAccessible(true);
        CjMemoryParser parser = new CjMemoryParser();
        // 复现生产链路 parseRawJson 的前置初始化：handleProperty 依赖 initRootTypeList
        // 填充实例字段 rootTypeIdList，否则 readRootGlobal/Local/Unknown 会对空列表
        // get(key) 抛 IndexOutOfBoundsException（size=0），而非预期的 IllegalStateException
        if ("handleProperty".equals(methodName)) {
            Method init = CjMemoryParser.class.getDeclaredMethod("initRootTypeList");
            init.setAccessible(true);
            init.invoke(parser);
        }
        return method.invoke(parser, args.toArray());
    }

    /**
     * 设置 CjMemoryParser 的 static 字段值，供测试预置解析集合。
     *
     * @param name  字段名
     * @param value 要设置的值
     * @throws Exception 字段访问失败时抛出
     */
    static void setStaticField(String name, Object value) throws Exception {
        Field f = CjMemoryParser.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(null, value);
    }

    /**
     * 读取 CjMemoryParser 的 static 字段值，供测试断言解析集合状态。
     *
     * @param name 字段名
     * @param <T>  字段类型
     * @return 字段值
     * @throws Exception 字段访问失败时抛出
     */
    @SuppressWarnings("unchecked")
    static <T> T getStaticField(String name) throws Exception {
        Field f = CjMemoryParser.class.getDeclaredField(name);
        f.setAccessible(true);
        return (T) f.get(null);
    }

    /**
     * 读取 CjMemoryParser 实例的字段值，供测试断言实例状态。
     *
     * @param parser 目标实例
     * @param name   字段名
     * @param <T>    字段类型
     * @return 字段值
     * @throws Exception 字段访问失败时抛出
     */
    @SuppressWarnings("unchecked")
    static <T> T getInstanceField(Object parser, String name) throws Exception {
        Field f = CjMemoryParser.class.getDeclaredField(name);
        f.setAccessible(true);
        return (T) f.get(parser);
    }

    /**
     * 反射调用 CjMemoryParser 的 private 无参方法并返回结果。
     *
     * @param methodName 目标 private 方法名
     * @param target     目标实例
     * @return 目标方法的返回值
     * @throws Exception 反射调用或目标方法执行失败时抛出
     */
    static Object invokeNoArg(String methodName, Object target) throws Exception {
        Method method = CjMemoryParser.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(target);
    }

    /**
     * CjMemoryParser 的解析集合大多为 static，跨解析会累积脏数据；
     * 反射调用 private 的 clearAllCollections 并额外清空 ROOT_SET。
     *
     * @throws Exception 反射调用或字段访问失败时抛出
     */
    @SuppressWarnings("unchecked")
    static void resetStaticState() throws Exception {
        Method clear = CjMemoryParser.class.getDeclaredMethod("clearAllCollections");
        clear.setAccessible(true);
        clear.invoke(new CjMemoryParser());

        Field rootSetField = CjMemoryParser.class.getDeclaredField("ROOT_SET");
        rootSetField.setAccessible(true);
        Set<Integer> rootSet = (Set<Integer>) rootSetField.get(null);
        rootSet.clear();

        // startTime 也是 static，会被真实 chunk 解析写入非零值，必须一并重置，避免跨测试污染
        Field startTimeField = CjMemoryParser.class.getDeclaredField("startTime");
        startTimeField.setAccessible(true);
        startTimeField.setLong(null, 0L);
    }

    /**
     * 两阶段解析 raw heap snapshot 文件：
     * 1) 遍历外层 JSON 对象流（{@code {"method":...,"params":{"chunk":"<json片段>"}}}），
     *    按顺序拼接所有 chunk 字符串；
     * 2) 用拼接后的完整 JSON 创建 parser，交给 CjMemoryParser 解析。
     *
     * @param filePath raw heap snapshot 文件路径
     * @return 解析得到的 RawHeapSnapshot
     * @throws Exception 文件读取或解析失败时抛出
     */
    static RawHeapSnapshot processRawChunkFile(String filePath) throws Exception {
        resetStaticState();

        File file = new File(filePath);
        StringBuilder chunkBuilder = new StringBuilder();
        JsonFactory factory = new JsonFactory();

        try (JsonParser fileParser = factory.createParser(file)) {
            while (fileParser.nextToken() != null) {
                if ("chunk".equals(fileParser.getCurrentName())) {
                    fileParser.nextToken(); // 移动到 chunk 的字符串值
                    chunkBuilder.append(fileParser.getText());
                }
            }
        }

        // 拼接后的完整 JSON 应为一个对象
        String fullJson = chunkBuilder.toString();
        org.assertj.core.api.Assertions.assertThat(fullJson.trim()).isNotEmpty()
            .startsWith("{")
            .endsWith("}");

        try (JsonParser chunkParser = factory.createParser(fullJson)) {
            Object rawResult = invokePrivate(
                "readRawCjHeapSnapshot", List.of(JsonParser.class), List.of(chunkParser));
            org.assertj.core.api.Assertions.assertThat(rawResult).isInstanceOf(RawHeapSnapshot.class);
            return RawHeapSnapshot.class.cast(rawResult);
        }
    }

    /**
     * 将文件内容通过管道写入 ParserStreamContext，喂给 {@link CjMemoryParser#parseJson}，
     * 模拟真实场景下的流式输入。
     *
     * @param filePath heap snapshot 文件路径
     * @return 解析得到的 RawHeapSnapshot
     * @throws Exception 文件读取或解析失败时抛出
     */
    static RawHeapSnapshot parseThroughPipe(Path filePath) throws Exception {
        resetStaticState(); // 与 processRawChunkFile 保持一致，避免跨测试 static 污染

        CjMemoryParser parser = new CjMemoryParser();
        ParserStreamContext context = new ParserStreamContext();
        try (InputStream in = Files.newInputStream(filePath);
             PipedInputStream pin = new PipedInputStream(1024 * 1024);
             PipedOutputStream pout = new PipedOutputStream(pin)) {
            context.setInputStream(pin);
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) {
                pout.write(buf, 0, n);
            }
            pout.flush();
            return parser.parseJson(context);
        }
    }
}