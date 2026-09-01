/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.extend.params;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

import java.util.Objects;

/**
 * In a time-consuming complete scenario, the tip is displayed, indicating that the user needs to wait.
 *
 * @since 2024-02-17
 */
public class CompletionTipParams {
    /**
     * The URI for which completion tip is display.
     */
    @NonNull
    private String uri;

    /**
     * The position of tip message display.
     */
    @NonNull
    private Position position;

    /**
     * The tip message.
     */
    @NonNull
    private String tip;

    /**
     * completion tip
     *
     * @param uri String
     * @param position Position
     * @param tip String
     */
    public CompletionTipParams(String uri, Position position, String tip) {
        this.uri = uri;
        this.position = position;
        this.tip = tip;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public String getTip() {
        return tip;
    }

    public void setTip(String tip) {
        this.tip = tip;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!(o instanceof CompletionTipParams)) {
            return false;
        }
        CompletionTipParams that = (CompletionTipParams) o;
        return Objects.equals(uri, that.uri) && Objects.equals(position, that.position)
                && Objects.equals(tip, that.tip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri, position, tip);
    }
}
