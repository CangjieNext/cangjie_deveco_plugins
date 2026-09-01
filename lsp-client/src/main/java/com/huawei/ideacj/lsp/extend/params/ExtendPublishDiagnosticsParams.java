/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.extend.params;

import org.eclipse.lsp4j.jsonrpc.util.Preconditions;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * CodeAction Diagnostics notification are sent from the server to the client to signal results of validation runs.
 *
 * @since 2024-12-18
 */
public class ExtendPublishDiagnosticsParams {
    @NonNull
    private String uri;

    @NonNull
    private List<ExtendDiagnostic> diagnostics;

    private Integer version;

    /**
     * Inline fixes for diagnostics
     */
    public ExtendPublishDiagnosticsParams() {
        ArrayList<ExtendDiagnostic> arrayList = new ArrayList<ExtendDiagnostic>();
        this.diagnostics = arrayList;
    }

    /**
     * Inline fixes for diagnostics
     *
     * @param uri           uri
     * @param diagnostics   diagnostics
     * @param version       version
     */
    public ExtendPublishDiagnosticsParams(@NonNull final String uri,
                                          @NonNull final List<ExtendDiagnostic> diagnostics,
                                          final Integer version) {
        this.uri = Preconditions.<String> checkNotNull(uri, "uri");
        this.diagnostics = Preconditions.<List<ExtendDiagnostic>> checkNotNull(diagnostics, "diagnostics");
        this.version = Preconditions.<Integer> checkNotNull(version, "version");
    }

    /**
     * The URI for which diagnostic information is reported.
     *
     * @return String
     */
    public String getUri() {
        return uri;
    }

    /**
     * The URI for which diagnostic information is reported.
     *
     * @param uri uri
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * An array of diagnostic information items.
     *
     * @return List<ExtendDiagnostic>
     */
    public List<ExtendDiagnostic> getDiagnostics() {
        return diagnostics;
    }

    /**
     * An array of diagnostic information items.
     *
     * @param diagnostics ExtendDiagnostic
     */
    public void setDiagnostics(List<ExtendDiagnostic> diagnostics) {
        this.diagnostics = diagnostics;
    }

    /**
     * Optional the version number of the document the diagnostics are published for.
     * Since 3.15.0
     *
     * @return Integer
     */
    public Integer getVersion() {
        return version;
    }

    /**
     * Optional the version number of the document the diagnostics are published for.
     * Since 3.15.0
     *
     * @param version doc version
     */
    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!(o instanceof ExtendPublishDiagnosticsParams)) {
            return false;
        }
        ExtendPublishDiagnosticsParams that = (ExtendPublishDiagnosticsParams) o;
        return Objects.equals(uri, that.uri) && Objects.equals(diagnostics, that.diagnostics)
                && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri, diagnostics, version);
    }
}
