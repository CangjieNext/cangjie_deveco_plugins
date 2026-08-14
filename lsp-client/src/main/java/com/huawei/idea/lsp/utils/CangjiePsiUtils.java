/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.idea.lsp.utils;

import com.huawei.idea.language.CangJieTypes;
import com.huawei.idea.language.psi.toplevel.classnode.CjClassDefinition;
import com.huawei.idea.language.psi.toplevel.enumnode.CjEnumDefinition;
import com.huawei.idea.language.psi.toplevel.extendnode.CjExtendDefinition;
import com.huawei.idea.language.psi.toplevel.interfacenode.CjInterfaceDefinition;
import com.huawei.idea.language.psi.toplevel.structnode.CjStructDefinition;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopesCore;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

/**
 * CommonUtils
 *
 * @since 2025-06-19
 */
public class CangjiePsiUtils {
    private static final Map<Class<? extends PsiElement>, TypeNameExtractor<? extends PsiElement>>
        TYPE_EXTRACTOR_REGISTRY = Map.of(
            CjClassDefinition.class, new TypeNameExtractor<CjClassDefinition>() {
                @Override
                public String extractName(CjClassDefinition element) {
                    return element.getClassName();
                }
                @Override
                public String getTypeName() {
                    return "class";
                }
            },
            CjStructDefinition.class, new TypeNameExtractor<CjStructDefinition>() {
                @Override
                public String extractName(CjStructDefinition element) {
                    return element.getName();
                }
                @Override
                public String getTypeName() {
                    return "struct";
                }
            },
            CjInterfaceDefinition.class, new TypeNameExtractor<CjInterfaceDefinition>() {
                @Override
                public String extractName(CjInterfaceDefinition element) {
                    return element.getName();
                }
                @Override
                public String getTypeName() {
                    return "interface";
                }
            },
            CjEnumDefinition.class, new TypeNameExtractor<CjEnumDefinition>() {
                @Override
                public String extractName(CjEnumDefinition element) {
                    return element.getName();
                }
                @Override
                public String getTypeName() {
                    return "enum";
                }
            },
            CjExtendDefinition.class, new TypeNameExtractor<CjExtendDefinition>() {
                @Override
                public String extractName(CjExtendDefinition element) {
                    return element.getName();
                }
                @Override
                public String getTypeName() {
                    return "extend";
                }
            }
        );

    private interface TypeNameExtractor<T extends PsiElement> {
        String extractName(T element);
        String getTypeName();
    }

    /**
     * Delete comma separated element.
     *
     * @param element the element
     */
    public static void deleteCommaSeparatedElement(PsiElement element) {
        // First, check backwards. if there is a comma
        // it indicates that the element is not the last parameter
        // delete the element and the comma
        ASTNode node = element.getNode();
        ASTNode check = node;
        ASTNode foundComma = null;
        while ((check = check.getTreeNext()) != null) {
            if (check.getElementType() == CangJieTypes.COMMA) {
                foundComma = check;
                break;
            }
        }

        // Otherwise, look forwards. In this case
        // the element is the last parameter
        // delete the element and the comma of the prev element
        if (check == null) {
            check = node;
            while ((check = check.getTreePrev()) != null) {
                if (check.getElementType() == CangJieTypes.COMMA) {
                    foundComma = check;
                } else if (foundComma != null) {
                    break;
                } else {
                    continue;
                }
            }
        }

        // Delete element and any discovered comma.
        // Keep whitespaces
        ASTNode parentNode = element.getParent().getNode();
        parentNode.removeChild(node);
        if (foundComma != null) {
            parentNode.removeChild(foundComma);
        }
    }

    /**
     * 判断是否是 cangjie 文件
     *
     * @param file file
     * @return 是否是cangjie文件
     */
    public static boolean isCangjieFile(PsiFile file) {
        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null) {
            return false;
        }
        String extension = virtualFile.getExtension();
        if (StringUtils.isEmpty(extension)) {
            return false;
        }
        return LanguageManager.CANGJIE_EXTENSION.equals(extension);
    }

    /**
     * Record to represent a type name conflict.
     *
     * @param typeName the type display name (class, struct, interface, enum, extend)
     * @param targetName the conflicting target name
     */
    public record TypeNameConflict(String typeName, String targetName) {}

    /**
     * Find a type name conflict in the target directory using PSI API.
     * This method uses type-safe PSIElement comparison instead of string comparison.
     *
     * @param project the current project
     * @param targetPath the target directory path
     * @param targetName the target name to check for
     * @param ignoredElement the element to ignore (usually the source element)
     * @return an Optional containing the conflict if found, empty otherwise
     */
    @NotNull
    public static Optional<TypeNameConflict> findTargetNameConflict(
        @NotNull Project project,
        @NotNull String targetPath,
        @NotNull String targetName,
        @Nullable PsiElement ignoredElement) {
        if (targetName.isBlank() || targetPath.isBlank()) {
            return Optional.empty();
        }

        VirtualFile targetDirectory = LocalFileSystem.getInstance().findFileByPath(targetPath);
        if (targetDirectory == null || !targetDirectory.isDirectory()) {
            return Optional.empty();
        }

        PsiDirectory psiDirectory = PsiManager.getInstance(project).findDirectory(targetDirectory);
        if (psiDirectory == null) {
            return Optional.empty();
        }

        String normalizedTargetName = targetName.trim();
        PsiManager psiManager = PsiManager.getInstance(project);

        GlobalSearchScope directoryScope = GlobalSearchScopesCore.directoryScope(project,targetDirectory, true);

        Collection<VirtualFile> virtualFiles = FilenameIndex.getAllFilesByExt(
            project, LanguageManager.CANGJIE_EXTENSION, directoryScope);

        for (VirtualFile virtualFile : virtualFiles) {
            PsiFile psiFile = psiManager.findFile(virtualFile);
            if (psiFile == null) {
                continue;
            }
            Optional<TypeNameConflict> conflict = findTypeNamedInFile(
                psiFile, normalizedTargetName, ignoredElement);
            if (conflict.isPresent()) {
                return conflict;
            }
        }
        return Optional.empty();
    }

    @NotNull
    private static Optional<TypeNameConflict> findTypeNamedInFile(
        @NotNull PsiFile psiFile,
        @NotNull String targetName,
        @Nullable PsiElement ignoredElement) {
        String fileText = psiFile.getText();
        int offset = 0;
        int targetLength = targetName.length();

        List<Integer> offsets = new ArrayList<>();
        while ((offset = fileText.indexOf(targetName, offset)) != -1) {
            offsets.add(offset);
            offset += targetLength;
        }

        for (int targetOffset : offsets) {
            Optional<TypeNameConflict> conflict = checkConflictAtOffset(
                psiFile, targetOffset, targetName, ignoredElement);
            if (conflict.isPresent()) {
                return conflict;
            }
        }

        return Optional.empty();
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private static Optional<TypeNameConflict> checkConflictAtOffset(
        @NotNull PsiFile psiFile,
        int offset,
        @NotNull String targetName,
        @Nullable PsiElement ignoredElement) {
        PsiElement leafElement = psiFile.findElementAt(offset);
        if (leafElement == null) {
            return Optional.empty();
        }

        // 核心优化：只向上找2层父亲
        PsiElement cjIdentifier = leafElement.getParent();
        if (cjIdentifier == null || cjIdentifier instanceof PsiFile) {
            return Optional.empty();
        }
        PsiElement parent = cjIdentifier.getParent();
        if (parent == null || parent instanceof PsiFile) {
            return Optional.empty();
        }

        TypeNameExtractor<PsiElement> extractor = null;
        for (Map.Entry<Class<? extends PsiElement>, TypeNameExtractor<? extends PsiElement>> entry
                : TYPE_EXTRACTOR_REGISTRY.entrySet()) {
            if (entry.getKey().isInstance(parent)) {
                extractor = (TypeNameExtractor<PsiElement>) entry.getValue();
                break;
            }
        }

        if (extractor != null) {
            if (isNotIgnored(parent, ignoredElement) && targetName.equals(extractor.extractName(parent))) {
                return Optional.of(new TypeNameConflict(extractor.getTypeName(), targetName));
            }
        }
        return Optional.empty();
    }

    private static boolean isNotIgnored(@NotNull PsiElement candidate, @Nullable PsiElement ignoredElement) {
        if (ignoredElement == null) {
            return true;
        }
        return candidate.getContainingFile() != ignoredElement.getContainingFile()
            || !candidate.getTextRange().equals(ignoredElement.getTextRange());
    }
}
