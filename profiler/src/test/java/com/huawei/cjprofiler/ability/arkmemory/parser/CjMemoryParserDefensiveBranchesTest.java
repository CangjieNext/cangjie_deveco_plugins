/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.ability.arkmemory.parser;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * 覆盖 {@link CjMemoryParser#readStackFrameList}/{@link CjMemoryParser#samples}
 * 的防御性异常分支（非法输入时抛 IllegalStateException）。
 *
 * <p>这些分支在真实快照数据中不会出现（数据总是良构的），只能通过构造非法 JSON
 * 触发；与 {@code CjMemoryParserTest.getList_throwsWhenNotStartArray} 同一模式。
 *
 * <p>注意：readStackFrameList/samples 先检查 currentToken 是否为 START_ARRAY，
 * 因此需要在调用前 {@code parser.nextToken()} 让指针位于 START_ARRAY，才能
 * 命中子数组/对象元素的异常分支。
 */
class CjMemoryParserDefensiveBranchesTest {
    private static final List<Class<?>> PARSER_ARG = List.of(JsonParser.class);

    @AfterEach
    void tearDown() throws Exception {
        // readStackFrameList/samples 异常路径可能已写入 static 集合，必须清理
        CjMemoryParserTestSupport.resetStaticState();
    }

    // ---------- readStackFrameList 防御分支 ----------

    @Test
    void readStackFrameList_throwsWhenNotStartArray() throws Exception {
        // 指针停在 VALUE_NUMBER，非 START_ARRAY；无需 nextToken
        assertThatThrownBy(() -> CjMemoryParserTestSupport.invokePrivate(
            "readStackFrameList", PARSER_ARG, List.of(new JsonFactory().createParser("42"))))
            .isInstanceOf(InvocationTargetException.class)
            .hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void readStackFrameList_throwsWhenSubArrayNotStartArray() throws Exception {
        // [42]：外层 START_ARRAY，但元素 42 不是子数组
        JsonParser parser = new JsonFactory().createParser("[42]");
        parser.nextToken();
        assertThatThrownBy(() -> CjMemoryParserTestSupport.invokePrivate(
            "readStackFrameList", PARSER_ARG, List.of(parser)))
            .isInstanceOf(InvocationTargetException.class)
            .hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void readStackFrameList_throwsWhenSubArrayNotEndArray() throws Exception {
        // [[100,1,2,66,999]]：帧数组多出一个元素，读 lineNum 后 token 非 END_ARRAY
        JsonParser parser = new JsonFactory().createParser("[[100,1,2,66,999]]");
        parser.nextToken();
        assertThatThrownBy(() -> CjMemoryParserTestSupport.invokePrivate(
            "readStackFrameList", PARSER_ARG, List.of(parser)))
            .isInstanceOf(InvocationTargetException.class)
            .hasRootCauseInstanceOf(IllegalStateException.class);
    }

    // ---------- samples 防御分支 ----------

    @Test
    void samples_throwsWhenNotStartArray() throws Exception {
        // 指针停在 VALUE_NUMBER，非 START_ARRAY；无需 nextToken
        assertThatThrownBy(() -> CjMemoryParserTestSupport.invokePrivate(
            "samples", PARSER_ARG, List.of(new JsonFactory().createParser("42"))))
            .isInstanceOf(InvocationTargetException.class)
            .hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void samples_throwsWhenElementNotStartObject() throws Exception {
        // [42]：外层 START_ARRAY，但元素 42 不是对象
        JsonParser parser = new JsonFactory().createParser("[42]");
        parser.nextToken();
        assertThatThrownBy(() -> CjMemoryParserTestSupport.invokePrivate(
            "samples", PARSER_ARG, List.of(parser)))
            .isInstanceOf(InvocationTargetException.class)
            .hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void samples_throwsWhenObjectNotEndObject() throws Exception {
        // sample 对象多出第 4 个字段，读完 ordinal 后 token 非 END_OBJECT
        JsonParser parser =
            new JsonFactory().createParser("[{\"size\":16,\"nodeId\":1,\"ordinal\":0,\"extra\":1}]");
        parser.nextToken();
        assertThatThrownBy(() -> CjMemoryParserTestSupport.invokePrivate(
            "samples", PARSER_ARG, List.of(parser)))
            .isInstanceOf(InvocationTargetException.class)
            .hasRootCauseInstanceOf(IllegalStateException.class);
    }
}