/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.ability.arkmemory.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;

/**
 * Tests for {@link CjMemoryParser} focusing on pure logic methods.
 */
class CjMemoryParserTest {
    private static final List<Class<?>> PARSER_ARG = List.of(JsonParser.class);

    @Test
    void convertToMilliseconds_correctlyCombinesHighAndLowBits() throws Exception {
        long result = (long) CjMemoryParserTestSupport.invokePrivate(
            "convertToMilliseconds", List.of(int.class, int.class), List.of(0, 1000));
        assertThat(result).isEqualTo(1L);

        result = (long) CjMemoryParserTestSupport.invokePrivate(
            "convertToMilliseconds", List.of(int.class, int.class), List.of(1, 0));
        assertThat(result).isEqualTo(4_294_967L);
    }

    @Test
    void getList_extractsIntegerArrayFromJsonParser() throws Exception {
        JsonParser parser = new JsonFactory().createParser("[1, 2, 3, 42]");
        // getList calls nextToken() internally which advances to START_ARRAY:
        // do NOT pre-advance, it expects the parser to be before START_ARRAY

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) CjMemoryParserTestSupport.invokePrivate(
            "getList", PARSER_ARG, List.of(parser));
        assertThat(result).containsExactly(1, 2, 3, 42);
    }

    @Test
    void getList_throwsWhenNotStartArray() throws Exception {
        JsonParser parser = new JsonFactory().createParser("42");
        // getList calls nextToken() -> VALUE_NUMBER, then checks != START_ARRAY -> throws

        assertThatThrownBy(() -> CjMemoryParserTestSupport.invokePrivate(
            "getList", PARSER_ARG, List.of(parser)))
            .isInstanceOf(InvocationTargetException.class)
            .hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void convertListToIntArray_returnsEmptyArrayForEmptyList() throws Exception {
        int[] result = (int[]) CjMemoryParserTestSupport.invokePrivate(
            "convertListToIntArray", List.of(List.class), List.of(Collections.emptyList()));
        assertThat(result).isEmpty();
    }
}