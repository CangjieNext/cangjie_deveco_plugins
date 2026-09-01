/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.resource;

import com.huawei.deveco.projectmodel.ohos.model.ModuleModel;
import com.huawei.deveco.res.common.FolderTypeRelationship;
import com.huawei.deveco.res.common.ResourceFolderType;
import com.huawei.deveco.res.common.ResourceType;
import com.huawei.deveco.res.ohos.common.item.HosResourceItem;
import com.huawei.deveco.res.ohos.utils.DocMarkUpUtil;
import com.huawei.deveco.res.ohos.utils.ModuleUtils;
import com.huawei.ideacj.language.psi.othersnode.CjValueArgument;
import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroTokens;

import com.intellij.lang.documentation.DocumentationProvider;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Cangjie Resource Documentation Provider
 * 用于处理仓颉代码中 @r(...) 资源引用的悬浮文档显示
 *
 * @since 2026-03-28
 */
public class CangjieResourceDocProvider implements DocumentationProvider {
    // 匹配类似 app.string.name 的格式
    private static final Pattern RESOURCE_PATTERN = Pattern.compile("app\\.[a-z]+\\.[0-9a-zA-Z_-]+");
    private static final String AT_R = "@r";
    private static final String DOT = ".";

    @Override
    @Nullable
    public String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        return generateHoverDoc(element, originalElement);
    }

    @Override
    @Nullable
    public String generateHoverDoc(@NotNull PsiElement element, @Nullable PsiElement originalElement) {
        // 基础环境校验
        if (originalElement == null) {
            return null;
        }
        CjMacroTokens cjMacroTokens = PsiTreeUtil.getParentOfType(originalElement, CjMacroTokens.class);
        if (cjMacroTokens == null) {
            return null;
        }

        // 获取原始文本并清理引号
        String rawText = cjMacroTokens.getText();
        String resourceText = StringUtil.unquoteString(rawText.trim());

        // 如果是折叠后的状态，element 可能是 CjValueArgument
        // 如果是正常状态，我们需要校验它是否符合资源路径格式
        if (!RESOURCE_PATTERN.matcher(resourceText).matches()) {
            return null;
        }

        return getDocument(resourceText, cjMacroTokens);
    }

    @Nullable
    private String getDocument(String resourceText, PsiElement originalElement) {
        // 分解 app.type.name
        List<String> parts = StringUtil.split(resourceText, DOT);
        if (parts.size() != 3) {
            return null;
        }

        String type = parts.get(1);
        String name = parts.get(2);

        ResourceType resourceType = ResourceType.fromTypeName(type);
        if (resourceType == null || !FolderTypeRelationship.match(resourceType, ResourceFolderType.ELEMENT)) {
            return null;
        }

        ModuleModel module = ModuleUtils.findModuleModelByPsiElement(originalElement);
        if (module == null) {
            return null;
        }

        // 调用底层工具类获取资源项列表
        List<HosResourceItem> list = DocMarkUpUtil.getReferenceResources(module, resourceType, name);
        if (list.isEmpty()) {
            return null;
        }

        // 生成标准的悬浮文档内容（包含路径、Key、Value的HTML）
        return DocMarkUpUtil.generateHoverDocument(list, module);
    }

    @Override
    @Nullable
    public PsiElement getCustomDocumentationElement(@NotNull Editor editor, @NotNull PsiFile file,
            @Nullable PsiElement contextElement, int targetOffset) {
        if (contextElement == null) {
            return null;
        }

        // 如果鼠标悬浮在 @r(...) 的任何部分
        // 尝试向上寻找 CjValueArgument 或向下寻找 CjMacroTokens
        CjValueArgument argument = PsiTreeUtil.getParentOfType(contextElement, CjValueArgument.class);
        if (argument != null && StringUtil.startsWith(argument.getText(), AT_R)) {
            CjMacroTokens token = PsiTreeUtil.findChildOfType(argument, CjMacroTokens.class);
            if (token != null) {
                return token; // 返回内部 Token 供 generateHoverDoc 解析路径
            }
            return argument;
        }

        return null;
    }
}
