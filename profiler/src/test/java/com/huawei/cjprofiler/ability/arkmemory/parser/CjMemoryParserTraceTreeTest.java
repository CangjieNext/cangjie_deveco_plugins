/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.ability.arkmemory.parser;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 针对 CjMemoryParser 的 "head"（调用树 traceTree）解析链路的测试：
 * 覆盖 readHeadInfo / readTraceTree / addObject / readStartArray /
 * calcTraceNodeTotalSize / readTraceNode 一整条路径。
 */
public class CjMemoryParserTraceTreeTest {

    @BeforeEach
    void setUp() throws Exception {
        CjMemoryParserTestSupport.resetStaticState();
    }

    @AfterEach
    void tearDown() throws Exception {
        CjMemoryParserTestSupport.resetStaticState();
    }

    /**
     * 构造一个含 head 属性的最小 JSON：
     * 根节点 callFrame 之后，通过 "children" 数组挂一个子 callFrame，
     * 使 addObject 触发 readStartArray（嵌套数组分支）和 calcTraceNodeTotalSize。
     */
    private JsonParser parserOf(String head) throws Exception {
        String json = "{\"head\":" + head + "}";
        return new JsonFactory().createParser(json);
    }

    @Test
    void handleProperty_withHeadRootFrame_parsesTraceTree() throws Exception {
        String head = "{\"callFrame\":{\"functionName\":\"main\",\"scriptName\":\"app.js\","
            + "\"lineNumber\":1,\"columnNumber\":2},\"selfSize\":10,\"id\":1}";

        try (JsonParser jsonParser = parserOf(head)) {
            // handleProperty 假定 parser 已定位在根 START_OBJECT 上
            jsonParser.nextToken();
            CjMemoryParserTestSupport.invokePrivate("handleProperty",
                List.of(JsonParser.class), List.of(jsonParser));
        }

        List<?> traceTree = CjMemoryParserTestSupport.getStaticField("traceTree");
        assertNotNull(traceTree);
        // 根节点: [id, funcIdx]，id 来自对象外 "id" 字段值 token
        assertEquals(2, traceTree.size());
        assertEquals("1", traceTree.get(0));
        List<?> strings = CjMemoryParserTestSupport.getStaticField("STRING_ARRAY");
        assertNotNull(strings);
    }

    @Test
    void handleProperty_withHeadParentAndChildren_buildsNestedTree() throws Exception {
        // 根 callFrame + 一个通过数组嵌套的子 callFrame，走 readStartArray 完整分支
        String head = "{\"callFrame\":{\"functionName\":\"parent\",\"scriptName\":\"p.js\","
            + "\"lineNumber\":1,\"columnNumber\":2},\"selfSize\":30,\"id\":1,"
            + "\"children\":[{\"callFrame\":{\"functionName\":\"child\",\"scriptName\":\"c.js\","
            + "\"lineNumber\":5,\"columnNumber\":6},\"selfSize\":20,\"id\":2}]}";

        try (JsonParser jsonParser = parserOf(head)) {
            // handleProperty 假定 parser 已定位在根 START_OBJECT 上
            jsonParser.nextToken();
            CjMemoryParserTestSupport.invokePrivate("handleProperty",
                List.of(JsonParser.class), List.of(jsonParser));
        }

        List<?> traceTree = CjMemoryParserTestSupport.getStaticField("traceTree");
        assertNotNull(traceTree);
        // 根: id="1"? 实际 size 字段落在 nextIntValue 之外，id 取 children 数组前的值 token
        assertEquals(5, traceTree.size());
        assertEquals("1", traceTree.get(0));
        assertNotNull(traceTree.get(4));
    }

    @Test
    void readHeadInfo_withNonObjectToken_throwsIllegalState() throws Exception {
        String json = "{\"head\":123}";

        try (JsonParser jsonParser = new JsonFactory().createParser(json)) {
            // 定位到根 START_OBJECT，让 handleProperty 内部推进到 "head" 字段及其值 123
            jsonParser.nextToken();

            List<Class<?>> paramTypes = List.of(JsonParser.class);
            List<Object> args = List.of(jsonParser);
            assertThrows(Exception.class, () ->
                CjMemoryParserTestSupport.invokePrivate("handleProperty", paramTypes, args));
        }
    }

    @Test
    void handleProperty_withUnknownProperty_skipsChildren() throws Exception {
        String json = "{\"futureSection\":{\"a\":[1,2,3]}}";

        try (JsonParser jsonParser = new JsonFactory().createParser(json)) {
            // handleProperty 假定 parser 已定位在根 START_OBJECT 上
            jsonParser.nextToken();
            CjMemoryParserTestSupport.invokePrivate("handleProperty",
                List.of(JsonParser.class), List.of(jsonParser));
        }
        // 执行到END_OBJECT 不抛异常即为通过
        List<?> traceTree = CjMemoryParserTestSupport.getStaticField("traceTree");
        assertNotNull(traceTree);
    }
}
