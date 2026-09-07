/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.ability.arkmemory.parser;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.huawei.deveco.insight.ohos.service.ark.RecordServiceBase.ParserStreamContext;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 覆盖 {@link CjMemoryParser} 各 readXxx 解析方法的防御性异常分支。
 *
 * <p>这些分支（Expected START_ARRAY / END_ARRAY for sub-array、未知 tag 跳过、
 * 溢出 catch 等）在良构的真实快照数据中不会出现，只能通过构造非法 JSON 触发。
 * 走 {@code handleProperty} 属性分发链路，与生产代码的真实调用路径一致。
 *
 * <p>trace 链路方法（readVersion/readHeadInfo/readTraceTree/readStartArray/
 * readTraceNode）的正常逻辑依赖真实 chunk 中的 {@code head}/{@code VERSION}
 * 数据，本测试仅覆盖其中可由非法输入触发的防御分支。
 */
class CjMemoryParserReadXxxDefensiveTest {
    @AfterEach
    void tearDown() throws Exception {
        // 部分非法输入在抛异常前已写入 static 集合，必须清理，避免跨测试污染
        CjMemoryParserTestSupport.resetStaticState();
    }

    /**
     * 构造指向外部大对象 START_OBJECT 的 parser，交给 handleProperty 分发。
     *
     * @param json 待解析的 JSON 字符串
     * @return 已移动到 START_OBJECT 的 JsonParser
     * @throws Exception 构造 parser 或移动 token 失败时抛出
     */
    private static JsonParser parserAtRootObject(String json) throws Exception {
        JsonParser parser = new JsonFactory().createParser(json);
        parser.nextToken(); // 到 START_OBJECT
        return parser;
    }

    /**
     * 断言：handleProperty 分发到目标属性后，因非法值抛出 IllegalStateException。
     *
     * @param json 待解析的 JSON 字符串
     */
    private static void assertHandlePropertyThrows(String json) {
        assertThatThrownBy(() -> CjMemoryParserTestSupport.invokePrivate(
            "handleProperty", List.of(JsonParser.class),
            List.of(parserAtRootObject(json))))
            .isInstanceOf(InvocationTargetException.class)
            .hasRootCauseInstanceOf(IllegalStateException.class);
    }

    // ---------- HEADER ----------

    @Test
    void readHeader_throwsWhenValueNotStartArray() {
        assertHandlePropertyThrows("{\"HEADER\": 42}");
    }

    // ---------- OBJECTS（readHeapDumpList isObjects=true） ----------

    @Test
    void readHeapDumpList_throwsWhenValueNotStartArray() {
        assertHandlePropertyThrows("{\"OBJECTS\": 42}");
    }

    @Test
    void readHeapDumpList_throwsWhenElementNotSubArray() {
        assertHandlePropertyThrows("{\"OBJECTS\": [42]}");
    }

    // ---------- readSubRecord ----------

    @Test
    void readSubRecord_throwsWhenElementNotSubArray() {
        // 带 VERSION 头的真实快照格式（dataVersion>0），checkHasTag 恒返回 true；
        // 首元素 [1,-128,595] = [tag,nodeType,id] 正常结束，第二个元素 42 不是子数组
        assertHandlePropertyThrows("{\"VERSION\":[1],\"OBJECTS\": [[1,-128,595],42]}");
    }

    @Test
    void readSubRecord_throwsWhenSubArrayNotEndArray() {
        // 带 VERSION 头（dataVersion>0，checkHasTag 恒 true）；
        // 第二个子数组 [255,100,200,300] = [tag,nodeType,id,多余字段]：tag=255(ROOT_UNKNOWN)
        // 消费 nodeType=100、id=200 后剩余 300，使子数组不以 END_ARRAY 结束
        assertHandlePropertyThrows("{\"VERSION\":[1],\"OBJECTS\": [[1,-128,595],[255,100,200,300]]}");
    }

    @Test
    void readSubRecord_skipsUnknownTag() {
        // 带 VERSION 头（dataVersion>0，checkHasTag 恒 true，记录结构 [tag,nodeType,id...]）；
        // tag=999 不在 SubRecordType 中，走"未知 tag 跳过"分支，正常返回不抛异常
        assertThatCode(() -> CjMemoryParserTestSupport.invokePrivate(
            "handleProperty", List.of(JsonParser.class),
            List.of(parserAtRootObject("{\"VERSION\":[1],\"OBJECTS\": [[999,-128,1,2,3]]}"))))
            .doesNotThrowAnyException();
    }

    @Test
    void readSubRecord_skipsUnknownTagWithNestedArray() {
        // 未知 tag 且子数组内嵌套数组，覆盖 depth 递增/递减逻辑
        assertThatCode(() -> CjMemoryParserTestSupport.invokePrivate(
            "handleProperty", List.of(JsonParser.class),
            List.of(parserAtRootObject("{\"VERSION\":[1],\"OBJECTS\": [[999,-128,[1,2],3]]}"))))
            .doesNotThrowAnyException();
    }

    @Test
    void readSubRecord_throwsWhenNotStartArrayDirectCall() throws Exception {
        // 直接反射调用，currentToken 为 null，命中方法入口的 START_ARRAY 检查
        JsonParser parser = new JsonFactory().createParser("42");
        assertThatThrownBy(() -> CjMemoryParserTestSupport.invokePrivate(
            "readSubRecord", List.of(JsonParser.class, boolean.class),
            List.of(parser, false)))
            .isInstanceOf(InvocationTargetException.class)
            .hasRootCauseInstanceOf(IllegalStateException.class);
    }

    // ---------- readObjectArrayDump 溢出 ----------

    @Test
    void readObjectArrayDump_handlesIntegerOverflow() {
        // num=9999999999 超出 int，命中 Math.toIntExact 的 ArithmeticException catch，
        // 解析仍正常完成。带 VERSION 头（dataVersion>0，checkHasTag 恒 true）；
        // 第二个子数组 [34,-128,100,9999999999,200,[]] 为 OBJECT_ARRAY_DUMP
        assertThatCode(() -> CjMemoryParserTestSupport.invokePrivate(
            "handleProperty", List.of(JsonParser.class),
            List.of(parserAtRootObject(
                "{\"VERSION\":[1],\"OBJECTS\": [[1,-128,595],[34,-128,100,9999999999,200,[]]]}"))))
            .doesNotThrowAnyException();
    }

    // ---------- STARTTHREAD ----------

    @Test
    void readStartThreadList_throwsWhenValueNotStartArray() {
        assertHandlePropertyThrows("{\"STARTTHREAD\": 42}");
    }

    @Test
    void readStartThreadList_throwsWhenSubArrayNotEndArray() {
        assertHandlePropertyThrows("{\"STARTTHREAD\": [1,2,3,4,999]}");
    }

    // ---------- STACKTRACE ----------

    @Test
    void readStackTraceList_throwsWhenValueNotStartArray() {
        assertHandlePropertyThrows("{\"STACKTRACE\": 42}");
    }

    @Test
    void readStackTraceList_throwsWhenFramesNotStartArray() {
        assertHandlePropertyThrows("{\"STACKTRACE\": [1,2,3]}");
    }

    @Test
    void readStackTraceList_throwsWhenSubArrayNotEndArray() {
        assertHandlePropertyThrows("{\"STACKTRACE\": [1,2,3,[100,101],999]}");
    }

    // ---------- STRING ----------

    @Test
    void readStringRecordList_throwsWhenValueNotStartArray() {
        assertHandlePropertyThrows("{\"STRING\": 42}");
    }

    @Test
    void readStringRecordList_throwsWhenElementNotSubArray() {
        assertHandlePropertyThrows("{\"STRING\": [42]}");
    }

    @Test
    void readStringRecordList_throwsWhenSubArrayNotEndArray() {
        assertHandlePropertyThrows("{\"STRING\": [[1,\"a\",999]]}");
    }

    // ---------- CLASSLOAD ----------

    @Test
    void readLoadClassList_throwsWhenValueNotStartArray() {
        assertHandlePropertyThrows("{\"CLASSLOAD\": 42}");
    }

    @Test
    void readLoadClassList_throwsWhenElementNotSubArray() {
        assertHandlePropertyThrows("{\"CLASSLOAD\": [42]}");
    }

    @Test
    void readLoadClassList_throwsWhenSubArrayNotEndArray() {
        assertHandlePropertyThrows("{\"CLASSLOAD\": [[1,2,999]]}");
    }

    // ---------- VERSION（trace 链路的非法输入防御分支） ----------

    @Test
    void readVersion_throwsWhenValueNotStartArray() {
        assertHandlePropertyThrows("{\"VERSION\": 42}");
    }

    // ---------- readRawCjHeapSnapshot / parseJson 顶层防御 ----------

    @Test
    void readRawCjHeapSnapshot_throwsWhenContentNotObject() throws Exception {
        // 顶层不是对象：nextToken 得 START_ARRAY，命中"Expected content to be an object"
        JsonParser parser = new JsonFactory().createParser("[1]");
        assertThatThrownBy(() -> CjMemoryParserTestSupport.invokePrivate(
            "readRawCjHeapSnapshot", List.of(JsonParser.class), List.of(parser)))
            .isInstanceOf(InvocationTargetException.class)
            .hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void parseJson_throwsWhenInvalidJsonStream() throws Exception {
        // 截断的 JSON 流：Jackson 抛 JsonParseException（IOException 子类），
        // 命中 parseJson 的 IOException catch 分支
        ParserStreamContext context = new ParserStreamContext();
        try (PipedInputStream pin = new PipedInputStream();
             PipedOutputStream pout = new PipedOutputStream(pin)) {
            pout.write("{\"HEADER\": [1".getBytes(StandardCharsets.UTF_8));
            // 关键：关闭写入端，让读端读到 EOF。否则 Jackson 在 readHeader 中继续
            // nextToken() 会因 PipedInputStream 无 EOF 而永久阻塞（测试卡住的根因）。
            pout.close();
            context.setInputStream(pin);
            assertThatThrownBy(() -> new CjMemoryParser().parseJson(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("IOException");
        }
    }
}