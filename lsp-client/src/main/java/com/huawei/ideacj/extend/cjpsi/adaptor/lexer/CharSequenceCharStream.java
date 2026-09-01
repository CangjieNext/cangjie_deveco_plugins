/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.extend.cjpsi.adaptor.lexer;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.IntStream;
import org.antlr.v4.runtime.misc.Interval;

/**
 * This class provides a basic implementation of
 * {@link CharStream} backed by an arbitrary
 * {@link CharSequence}.
 *
 * @since 2024/03/20
 */
class CharSequenceCharStream implements CharStream {
    private final CharSequence buffer;

    private final int endOffset;

    private final String sourceName;

    private int pos;

    /**
     * Instantiates a new Char sequence char stream.
     *
     * @param buffer     the buffer
     * @param endOffset  the end offset
     * @param sourceName the source name
     */
    public CharSequenceCharStream(CharSequence buffer, int endOffset, String sourceName) {
        this.buffer = buffer;
        this.sourceName = sourceName;
        this.endOffset = endOffset;
    }

    /**
     * Gets position.
     *
     * @return the position
     */
    protected final int getPosition() {
        return pos;
    }

    @Override
    public String getText(Interval interval) {
        int start = interval.a;
        int stop = interval.b;
        int size = size();
        if (stop >= size) {
            stop = size - 1;
        }
        if (start >= size) {
            return "";
        }
        return buffer.subSequence(start, stop + 1).toString();
    }

    @Override
    public void consume() {
        if (pos == size()) {
            throw new IllegalStateException("attempted to consume EOF");
        }

        pos++;
    }

    @Override
    public int LA(int position) {
        if (position > 0) {
            int index = this.pos + position - 1;
            if (index >= size()) {
                return IntStream.EOF;
            }

            return buffer.charAt(index);
        }
        if (position < 0) {
            int index = this.pos + position;
            if (index < 0) {
                return 0;
            }

            return buffer.charAt(index);
        }
        return 0;
    }

    @Override
    public void release(int marker) {}

    @Override
    public int mark() {
        return 0;
    }

    @Override
    public int index() {
        return pos;
    }

    @Override
    public void seek(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index cannot be negative");
        }
        pos = Math.min(index, size());
    }

    @Override
    public int size() {
        if (endOffset >= 0) {
            return endOffset;
        }

        return buffer.length();
    }

    @Override
    public String getSourceName() {
        return sourceName;
    }
}
