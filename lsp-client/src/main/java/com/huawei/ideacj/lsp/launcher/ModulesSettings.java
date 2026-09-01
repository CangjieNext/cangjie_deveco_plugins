/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.launcher;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * the class is for converting modules.json by gson
 *
 * @author t30009182
 * @since 2021-10-21
 */
public class ModulesSettings {
    /**
     * cangjie verison
     */
    @SerializedName("charc_version")
    private String charcVersion;

    /**
     * organization : cangjie
     */
    private String organization;

    /**
     * name
     */
    private String name;

    /**
     * version
     */
    private String version;

    /**
     * requires
     */
    private Map requires;

    /**
     * main_dir
     */
    @SerializedName("main_dir")
    private String mainDir;

    /**
     * foreign_requires
     */
    @SerializedName("foreign_requires")
    private Map foreignRequires;

    /**
     * library_type
     */
    @SerializedName("library_type")
    private String libraryType;

    /**
     * command_option
     */
    @SerializedName("command_option")
    private String commandOption;

    public String getCharcVersion() {
        return charcVersion;
    }

    public void setCharcVersion(String charcVersion) {
        this.charcVersion = charcVersion;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map getRequires() {
        return requires;
    }

    public void setRequires(Map requires) {
        this.requires = requires;
    }

    public String getMainDir() {
        return mainDir;
    }

    public void setMainDir(String mainDir) {
        this.mainDir = mainDir;
    }

    public Map getForeignRequires() {
        return foreignRequires;
    }

    public void setForeignRequires(Map foreignRequires) {
        this.foreignRequires = foreignRequires;
    }

    public String getLibraryType() {
        return libraryType;
    }

    public void setLibraryType(String libraryType) {
        this.libraryType = libraryType;
    }

    public String getCommandOption() {
        return commandOption;
    }

    public void setCommandOption(String commandOption) {
        this.commandOption = commandOption;
    }
}
