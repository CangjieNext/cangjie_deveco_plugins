/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package lsp.service;

import com.google.gson.GsonBuilder;
import com.huawei.ideacj.language.CangjieFile;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.DocumentUtil;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.json.adapters.*;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.utils.ApplicationUtils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

public class LanguageServiceUtils {

    private static final Logger LOG = Logger.getInstance(LanguageServiceUtils.class);

    public static final ClientCapabilities CLIENT_CAPABILITIES;

    public static final GsonBuilder LSP4J_GSON_BUILDER = new GsonBuilder()
            .registerTypeAdapterFactory(new CollectionTypeAdapter.Factory())
            .registerTypeAdapterFactory(new ThrowableTypeAdapter.Factory())
            .registerTypeAdapterFactory(new EitherTypeAdapter.Factory())
            .registerTypeAdapterFactory(new TupleTypeAdapters.TwoTypeAdapterFactory())
            .registerTypeAdapterFactory(new EnumTypeAdapter.Factory());

    static {
        InputStream in = LanguageServiceUtils.class.getClassLoader().getResourceAsStream("icons/ClientCapabilities.json");
        CLIENT_CAPABILITIES = LSP4J_GSON_BUILDER.create().fromJson(new InputStreamReader(in), ClientCapabilities.class);
    }

    /**
     * @param project     project
     * @param virtualFile vf
     * @return 是否是仓颉文件
     */
    public static boolean isCharPsiFile(VirtualFile virtualFile, Project project) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        return psiFile instanceof CangjieFile;
    }

    /**
     * copied from FileUtils
     */
    public static String sanitizeURI(String uri) {
        if (uri == null) {
            return null;
        } else {
            StringBuilder reconstructed = new StringBuilder();
            String uriCp = uri.replaceAll(" ", "%20");
            if (!uri.startsWith("file:")) {
                return uri;
            } else {
                for (uriCp = uriCp.substring("file:".length()); uriCp.startsWith(Character.toString('/')); uriCp = uriCp.substring(1)) {
                }
                reconstructed.append("file:///");
                String os = System.getProperty("os.name");
                if (!os.toLowerCase().contains("win")) {
                    return reconstructed.append(uriCp).toString();
                } else {
                    reconstructed.append(uriCp.substring(0, uriCp.indexOf(47)));
                    char driveLetter = reconstructed.charAt("file:///".length());
                    if (Character.isUpperCase(driveLetter)) {
                        reconstructed.setCharAt("file:///".length(), Character.toLowerCase(driveLetter));
                    }

                    if (reconstructed.toString().endsWith(":")) {
                        reconstructed.delete(reconstructed.length() - 1, reconstructed.length());
                    }

                    if (!reconstructed.toString().endsWith("%3A")) {
                        reconstructed.append("%3A");
                    }

                    return reconstructed.append(uriCp.substring(uriCp.indexOf(47))).toString();
                }
            }
        }
    }

    /**
     * copied from FileUtils
     */
    public static Position logicalToLSPPos(LogicalPosition position, Editor editor) {
        return offsetToLSPPos(editor.getDocument(), editor.logicalPositionToOffset(position));
    }

    /**
     * copied from FileUtils
     */
    public static Position offsetToLSPPos(LogicalPosition position, Editor editor) {
        return offsetToLSPPos(editor.getDocument(), editor.logicalPositionToOffset(position));
    }

    /**
     * copied from FileUtils
     */
    public static Position offsetToLSPPos(Document doc, int offset) {
        return (Position) ApplicationUtils.computableReadAction(() -> {
            int line = doc.getLineNumber(offset);
            int lineStart = doc.getLineStartOffset(line);
            String lineTextBeforeOffset = doc.getText(TextRange.create(lineStart, offset));
            int column = lineTextBeforeOffset.length();
            return (Position) ApplicationUtils.computableReadAction(() -> {
                return new Position(line, column);
            });
        });
    }

    /**
     * copied from FileUtils
     */
    public static int LSPPosToOffset(Editor editor, Position pos) {
        return (Integer) ApplicationUtils.computableReadAction(() -> {
            try {
                if (editor.isDisposed()) {
                    return -1;
                } else {
                    Document doc = editor.getDocument();
                    int line = Math.max(0, Math.min(pos.getLine(), doc.getLineCount()));
                    String lineText = doc.getText(DocumentUtil.getLineTextRange(doc, line));
                    String lineTextForPosition = !lineText.isEmpty() ? lineText.substring(0, Math.min(lineText.length(), pos.getCharacter())) : "";
                    int tabs = StringUtil.countChars(lineTextForPosition, '\t');
                    int tabSize = editor.getSettings().getTabSize(editor.getProject());
                    int column = tabs * tabSize + lineTextForPosition.length() - tabs;
                    int offset = editor.logicalPositionToOffset(new LogicalPosition(line, column));
                    if (pos.getCharacter() >= lineText.length()) {
                        LOG.warn("LSPPOS outofbounds : " + pos + " line : " + lineText + " column : " + column + " offset : " + offset);
                    }

                    int docLength = doc.getTextLength();
                    if (offset > docLength) {
                        LOG.warn("Offset greater than text length : " + offset + " > " + docLength);
                    }

                    return Math.min(Math.max(offset, 0), docLength);
                }
            } catch (IndexOutOfBoundsException var11) {
                return -1;
            }
        });
    }

    @NotNull
    public static TextDocumentItem createTextDocumentItem(String uri, String fullText) {
        uri = sanitizeURI(uri);
        return new TextDocumentItem(uri, "char", 0, fullText);
    }

    @NotNull
    public static List<TextDocumentContentChangeEvent> createTextDocumentContentChangeEvents(List<DocumentEvent> documentEvents) {
        return documentEvents.stream().map(documentEvent -> {
            Document document = documentEvent.getDocument();
            Position startPosition = LanguageServiceUtils.offsetToLSPPos(document, documentEvent.getOffset());
            int startLine = startPosition.getLine();
            int startColumn = startPosition.getCharacter();
            int endLine;
            int endColumn;
            if (documentEvent.getOldLength() > 0) {
                String oldText = documentEvent.getOldFragment().toString();
                endLine = startLine + StringUtil.countNewLines(oldText);
                String[] oldLines = oldText.split("\n");
                int oldTextLength = oldLines.length == 0 ? 0 : oldLines[oldLines.length - 1].length();
                endColumn = oldText.endsWith("\n") ? 0 : (oldLines.length == 1 ? startColumn + oldTextLength : oldTextLength);
            } else {
                endLine = startLine;
                endColumn = startColumn;
            }
            Range range = new Range(new Position(startLine, startColumn), new Position(endLine, endColumn));
            TextDocumentContentChangeEvent event = new TextDocumentContentChangeEvent();
            event.setText(documentEvent.getNewFragment().toString());
            event.setRange(range);
            return event;
        }).collect(Collectors.toList());
    }

}
