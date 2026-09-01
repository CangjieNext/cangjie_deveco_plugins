/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.extend.cjpsi.adaptor.lexer;

import net.jcip.annotations.Immutable;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.misc.IntegerStack;
import org.antlr.v4.runtime.misc.MurmurHash;
import org.antlr.v4.runtime.misc.ObjectEqualityComparator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * CJLexerState
 *
 * @since 2024/03/20
 */
@Immutable
public class CJLexerState {
    private final int mode;
    @Nullable
    private final int[] modeStack;
    private int cachedHashCode;

    /**
     * Constructor
     *
     * @param mode      The current lexer mode, {@link Lexer#_mode}.
     * @param modeStack The lexer mode stack, {@link Lexer#_modeStack}, or {@code null} .
     */
    public CJLexerState(int mode, @Nullable IntegerStack modeStack) {
        this.mode = mode;
        this.modeStack = modeStack != null ? modeStack.toArray() : null;
    }

    /**
     * Apply.
     *
     * @param lexer the lexer
     */
    public void apply(@NotNull Lexer lexer) {
        lexer._mode = mode;
        lexer._modeStack.clear();
        if (modeStack != null) {
            lexer._modeStack.addAll(modeStack);
        }
    }

    @Override
    public final int hashCode() {
        if (cachedHashCode == 0) {
            int hash = MurmurHash.initialize();
            hash = MurmurHash.update(hash, mode);
            hash = MurmurHash.update(hash, modeStack);
            cachedHashCode = MurmurHash.finish(hash, 2);
        }

        return cachedHashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof CJLexerState)) {
            return false;
        }

        CJLexerState other = (CJLexerState) obj;
        return this.mode == other.mode && ObjectEqualityComparator.INSTANCE.equals(this.modeStack, other.modeStack);
    }
}
