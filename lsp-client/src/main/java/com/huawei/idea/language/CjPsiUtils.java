/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.idea.language;

import com.huawei.idea.extend.cjpsi.adaptor.psi.CJPsiLeafNode;
import com.huawei.idea.language.psi.compileunitnode.CjTranslationUnit;
import com.huawei.idea.language.psi.othersnode.CjIdentifier;
import com.huawei.idea.language.psi.packagenode.CjPackageNameIdentifier;
import com.huawei.idea.language.psi.toplevel.CjTopLevelObject;
import com.huawei.idea.language.psi.toplevel.CjTypeParameters;
import com.huawei.idea.language.psi.toplevel.classnode.CjClassDefinition;
import com.huawei.idea.language.psi.toplevel.classnode.CjSuperInterfaces;
import com.huawei.idea.language.psi.toplevel.classnode.CjUserType;
import com.huawei.idea.language.psi.toplevel.functionnode.CjFunctionDefinition;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Utils for Psi node
 *
 * @since 2020-07-31
 */
public class CjPsiUtils {
    /**
     * Instance for Cang Jie Psi Utils
     */
    public static final CjPsiUtils INSTANCE = new CjPsiUtils();

    private CjPsiUtils() {
    }

    /**
     * Find package name in given psi file.
     *
     * @param file PsiFile
     * @return package Name
     */
    @NotNull
    public String getPackageInPsiFile(PsiFile file) {
        if (file == null) {
            return "";
        }
        return getPackageInElements(file.getChildren());
    }

    /**
     * Return CjTypeParameters
     *
     * @param element target element
     * @return String
     */
    public static String getCjTypeParameters(PsiElement element) {
        int countChildrenOfType = PsiTreeUtil.countChildrenOfType(element, CjTypeParameters.class);
        if (countChildrenOfType == 0) {
            return "";
        }
        CjTypeParameters cjTypeParameters = PsiTreeUtil.getRequiredChildOfType(element, CjTypeParameters.class);
        return cjTypeParameters.getText();
    }

    /**
     * 返回 macro 的返回值类型
     *
     * @param element element
     * @return macro 的返回值类型
     */
    public static String getMacroReturnType(PsiElement element) {
        int countChildrenOfType = PsiTreeUtil.countChildrenOfType(element, CjUserType.class);
        if (countChildrenOfType == 0) {
            return "";
        }
        CjUserType userType = PsiTreeUtil.getRequiredChildOfType(element, CjUserType.class);
        return userType.getText();
    }

    /**
     * Return ID PsiElement
     *
     * @param element element
     * @return List<PsiElement>
     */
    public static List<PsiElement> getIDPsi(PsiElement element) {
        List<PsiElement> aimElement = new ArrayList<>();
        PsiElement[] elements = element.getChildren();
        for (PsiElement childElement : elements) {
            if (CjPsiUtils.INSTANCE.isIdentifier(childElement)) {
                aimElement.add(childElement);
                return aimElement;
            }
        }
        return aimElement;
    }

    /**
     * getCjSuperInterfacesIDPsi
     *
     * @param element element
     * @return SuperInterfaces IDPsi
     */
    public static List<PsiElement> getCjSuperInterfacesIDPsi(PsiElement element) {
        List<PsiElement> psiElements = new ArrayList<>();
        PsiElement[] children = element.getChildren();
        for (PsiElement child : children) {
            if (CjPsiUtils.INSTANCE.isIdentifier(child)) {
                psiElements.add(child);
            } else if (child instanceof CjSuperInterfaces) {
                List<PsiElement> insert = ((CjSuperInterfaces) child).getIDPsi();
                if (insert == null) {
                    continue;
                }
                psiElements.addAll(insert);
            } else {
                continue;
            }
        }
        return psiElements;
    }

    /**
     * 从残缺的PsiElement中得到最原始的PsiElement
     *
     * @param element 残缺信息的element
     * @return 原始的PsiElement
     */
    public static PsiElement getOriginalPsiElement(PsiElement element) {
        if (element.getTextRange() != null) {
            PsiFile containingFile = element.getContainingFile();
            if (containingFile != null) {
                int offset = element.getTextRange().getStartOffset();
                PsiElement found = containingFile.findElementAt(offset);
                if (found != null) {
                    return found;
                }
            }
        }
        return element;
    }

    /**
     * get valid next token
     *
     * @param element target element
     * @return valid next token
     */
    public static PsiElement getValidNextToken(@NotNull PsiElement element) {
        PsiElement next = element.getNextSibling();
        while (next instanceof PsiWhiteSpace
                || next instanceof CJPsiLeafNode leafNode && leafNode.getElementType().equals(CangJieTypes.NL)) {
            next = next.getNextSibling();
        }
        return next;
    }

    private String getPackageInElements(PsiElement[] elements) {
        String result = "";
        for (PsiElement element : elements) {
            if (element instanceof CjPackageNameIdentifier) {
                return element.getText().replace(".", "\r\n");
            } else if (element instanceof CjTopLevelObject) {
                // package should in the beginning of files which mean it should appear before any TopLevelObject
                return result;
            } else {
                result = getPackageInElements(element.getChildren());
                if (!result.isEmpty()) {
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Find class name in given psi file's target line.
     *
     * @param psiFile PsiFile
     * @param line line
     * @return class Name
     */
    @NotNull
    public String getClassNameForTargetLine(PsiFile psiFile, int line) {
        if (psiFile == null) {
            return "";
        }
        Optional<PsiElement> element = getPsiElementInLine(psiFile, line);
        if (element.isPresent()) {
            return getClassNameForPsiElement(element.get());
        }
        return "";
    }

    private Optional<PsiElement> getPsiElementInLine(@NotNull PsiFile psiFile, int line) {
        VirtualFile file = psiFile.getVirtualFile();
        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document == null) {
            return Optional.empty();
        }
        int elementOffset = document.getLineStartOffset(line);
        PsiElement element = psiFile.findElementAt(elementOffset);
        return Optional.ofNullable(element);
    }

    private boolean isPsiElementInValid(PsiElement element) {
        return element == null || element instanceof CjTranslationUnit;
    }

    private String getClassNameForPsiElement(PsiElement element) {
        if (isPsiElementInValid(element)) {
            return "";
        }
        if (element instanceof CjClassDefinition) {
            return ((CjClassDefinition) element).getClassName();
        }
        return getClassNameForPsiElement(element.getParent());
    }

    private String getFunctionNameForPsiElement(PsiElement element) {
        if (isPsiElementInValid(element)) {
            return "";
        }
        if (element instanceof CjFunctionDefinition) {
            return ((CjFunctionDefinition) element).getFunctionDefinitionInfo().getName();
        }
        return getFunctionNameForPsiElement(element.getParent());
    }

    /**
     * Find if target psi element is Identifier
     *
     * @param psiElement psi element
     * @return boolean if is identifier
     */
    public boolean isIdentifier(PsiElement psiElement) {
        return psiElement instanceof CjIdentifier;
    }

    /**
     * Find if target psi element is aimString
     *
     * @param psiElement PsiElement
     * @param aimString  String
     * @return boolean is aimString
     */
    public boolean isAimString(PsiElement psiElement, String aimString) {
        return aimString.equals(psiElement.getNode().getElementType().toString());
    }
}