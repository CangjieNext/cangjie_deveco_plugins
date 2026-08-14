/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.utils;

import com.huawei.cjprofiler.common.Constants;
import com.huawei.deveco.insight.ohos.utils.LogPrinter;

import com.google.common.reflect.ClassPath;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * ClassUtil
 *
 * @since 2025/4/22
 */
public class ClassUtil {
    private static final LogPrinter LOGGER = LogPrinter.createLogger(ClassUtil.class);

    /**
     * findClassesWithAnnotation
     *
     * @param annotationClass Class<? extends Annotation>
     * @return List<Class<?>>
     */
    public static List<Class<?>> findClassesWithAnnotation(Class<? extends Annotation> annotationClass) {
        List<Class<?>> classes = findProjectClasses();
        return classes.stream()
                .filter(clz -> clz.isAnnotationPresent(annotationClass))
                .toList();
    }

    /**
     * closeAllIo
     *
     * @param allIo Closeable...
     */
    public static void closeAllIo(Closeable... allIo) {
        if (allIo == null) {
            return;
        }
        for (var io : allIo) {
            if (io != null) {
                try {
                    io.close();
                } catch (IOException e) {
                    String stackTrace = ExceptionUtils.getStackTrace(e);
                    LOGGER.warn(stackTrace);
                }
            }
        }
    }

    private static List<Class<?>> findProjectClasses() {
        URL rootUrl = ClassUtil.class.getClassLoader().getResource(Constants.CONTROLLER_ROOT_PATH);
        if (rootUrl == null) {
            return Collections.emptyList();
        }
        URL scanUrl = rootUrl;
        String classUrlPath = rootUrl.getPath();
        int jarUrlIndex = classUrlPath.lastIndexOf(Constants.JAR_URL_SEPARATOR);
        Set<ClassPath.ClassInfo> classInfos;
        URLClassLoader classLoader = null;
        try {
            if (jarUrlIndex >= 0) {
                scanUrl = URI.create(classUrlPath.substring(0, jarUrlIndex)).toURL();
            }
            classLoader = URLClassLoader.newInstance(new URL[]{scanUrl},
                    Thread.currentThread().getContextClassLoader());
            classInfos = ClassPath.from(classLoader).getTopLevelClassesRecursive(Constants.PACKAGE_ROOT_NAME);
        } catch (IOException e) {
            return Collections.emptyList();
        } finally {
            closeAllIo(classLoader);
        }

        List<Class<?>> classes = new ArrayList<>();
        for (var classInfo : classInfos) {
            try {
                classes.add(Class.forName(classInfo.getName()));
            } catch (NoClassDefFoundError | ClassNotFoundException e) {
                String stackTrace = ExceptionUtils.getStackTrace(e);
                LOGGER.warn(stackTrace);
            }
        }
        return classes;
    }
}
