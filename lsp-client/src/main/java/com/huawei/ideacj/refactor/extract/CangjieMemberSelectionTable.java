/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.refactor.extract;

import com.huawei.ideacj.lsp.utils.CangjieBundle;

import com.intellij.icons.AllIcons;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.classMembers.MemberInfoChange;
import com.intellij.refactoring.classMembers.MemberInfoModel;
import com.intellij.refactoring.ui.AbstractMemberSelectionTable;
import com.intellij.ui.ColoredTableCellRenderer;
import com.intellij.ui.IconManager;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.TableUtil;
import com.intellij.ui.icons.RowIcon;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * 仓颉成员选择表格实现，用于管理和渲染重构抽取接口过程中的组件列表。
 *
 * @since 2026-05-28
 */
public class CangjieMemberSelectionTable extends AbstractMemberSelectionTable<PsiElement, CangjieMemberInfo> {
    private static final long serialVersionUID = 1L;

    /**
     * 内部持久化的模型层引用。
     */
    protected CangjieMemberInfoModel myModel;

    /**
     * 仓颉成员选择表格构造器。
     *
     * @param infos 成员信息数据集
     * @param model 对应绑定的表格数据模型
     */
    public CangjieMemberSelectionTable(Collection<CangjieMemberInfo> infos, CangjieMemberInfoModel model) {
        super(infos, null, null);
        myModel = model;
        model.setTable(this);
        setModel(myModel);
        setMemberInfoModel(myModel);
        initUI();
    }

    private void initUI() {
        TableColumnModel columnModel = getColumnModel();
        columnModel.getColumn(DISPLAY_NAME_COLUMN).setCellRenderer(new CangjieTableRenderer(this));

        TableColumn checkBoxColumn = columnModel.getColumn(CHECKED_COLUMN);
        TableUtil.setupCheckboxColumn(checkBoxColumn, 6);

        int rowCount = myMemberInfos.size();
        int visibleRows = Math.max(3, Math.min(rowCount, 12));
        int preferredHeight = getRowHeight() * visibleRows;

        setPreferredScrollableViewportSize(JBUI.size(400, preferredHeight));

        setRowSelectionAllowed(false);
        setFocusable(false);
        setCellSelectionEnabled(false);

        setShowGrid(false);
        setIntercellSpacing(new Dimension(0, 0));

        int abstractColumnIndex = 2;
        TableColumn abstractColumn = columnModel.getColumn(abstractColumnIndex);

        abstractColumn.setCellRenderer(new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                if (!(table.getModel() instanceof CangjieMemberSelectionTable.CangjieMemberInfoModel model)) {
                    return new JLabel("");
                }
                int modelRow = table.convertRowIndexToModel(row);
                Optional<CangjieMemberInfo> memberInfo = model.getMemberInfoAt(modelRow);
                CangjieMemberInfo info = memberInfo.orElse(null);

                if (info != null && info.isStatic()) {
                    JLabel emptyLabel = new JLabel("");
                    emptyLabel.setOpaque(true);
                    emptyLabel.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                    return emptyLabel;
                }

                JCheckBox checkBox = new JCheckBox();
                if (info != null) {
                    boolean isAbstract = info.isToAbstract();
                    checkBox.setSelected(isAbstract);
                }
                checkBox.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                checkBox.setHorizontalAlignment(SwingConstants.CENTER);

                checkBox.setBorder(JBUI.Borders.emptyRight(20));
                checkBox.setBorderPainted(true);
                checkBox.setEnabled(true);

                return checkBox;
            }
        });

        FontMetrics metrics = getFontMetrics(getFont());
        int textWidth = metrics.stringWidth("Make Abstract") + JBUI.scale(30);

        abstractColumn.setMaxWidth(textWidth);
        abstractColumn.setMinWidth(textWidth);
        abstractColumn.setPreferredWidth(textWidth);
    }

    @Nullable
    @Override
    protected Object getAbstractColumnValue(CangjieMemberInfo memberInfo) {
        return memberInfo.isToAbstract();
    }

    @Override
    protected boolean isAbstractColumnEditable(int rowIndex) {
        return true;
    }

    @Override
    protected Icon getOverrideIcon(CangjieMemberInfo memberInfo) {
        return EMPTY_OVERRIDE_ICON;
    }

    @Override
    protected Icon getMemberIcon(CangjieMemberInfo memberInfo, int flags) {
        return memberInfo.getIcon();
    }

    @Override
    protected void setVisibilityIcon(CangjieMemberInfo memberInfo, RowIcon icon) {
        String visibility = memberInfo.getVisibility();

        if ("public".equalsIgnoreCase(visibility)) {
            icon.setIcon(AllIcons.Nodes.Public, VISIBILITY_ICON_POSITION);
        } else if ("protected".equalsIgnoreCase(visibility)) {
            icon.setIcon(AllIcons.Nodes.Protected, VISIBILITY_ICON_POSITION);
        } else if ("private".equalsIgnoreCase(visibility)) {
            icon.setIcon(AllIcons.Nodes.Private, VISIBILITY_ICON_POSITION);
        } else {
            icon.setIcon(AllIcons.Nodes.PackageLocal, VISIBILITY_ICON_POSITION);
        }
    }

    /**
     * 仓颉成员信息模型。
     */
    public static class CangjieMemberInfoModel extends AbstractTableModel
            implements MemberInfoModel<PsiElement, CangjieMemberInfo> {
        /**
         * 是否勾选列索引。
         */
        public static final int IS_SELECTED_COLUMN = 0;

        /**
         * 成员名称列索引。
         */
        public static final int NAME_COLUMN = 1;

        /**
         * 是否抽象列索引。
         */
        public static final int ABSTRACT_COLUMN = 2;

        private CangjieMemberSelectionTable myTable;
        private final List<CangjieMemberInfo> myInfoList;

        /**
         * 模型层构造方法。
         *
         * @param infoList 数据关联数据集
         */
        public CangjieMemberInfoModel(List<CangjieMemberInfo> infoList) {
            this.myInfoList = infoList;
        }

        @Override
        public int getRowCount() {
            return myInfoList.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int col) {
            if (col == IS_SELECTED_COLUMN) {
                return "";
            }
            if (col == NAME_COLUMN) {
                return CangjieBundle.message("lsp.refactor.extract.interface.member.column");
            }
            if (col == ABSTRACT_COLUMN) {
                return "Make Abstract";
            }
            return "";
        }

        @Override
        public Class<?> getColumnClass(int col) {
            return col == NAME_COLUMN ? String.class : Boolean.class;
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col == IS_SELECTED_COLUMN;
        }

        @Override
        public Object getValueAt(int row, int col) {
            if (row < 0 || row >= myInfoList.size()) {
                return null;
            }
            CangjieMemberInfo info = myInfoList.get(row);
            return switch (col) {
                case IS_SELECTED_COLUMN -> info.isChecked();
                case NAME_COLUMN -> info.getDisplayName();
                case ABSTRACT_COLUMN -> info.isToAbstract();
                default -> null;
            };
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (row < 0 || row >= myInfoList.size()) {
                return;
            }
            CangjieMemberInfo info = myInfoList.get(row);
            if (col == IS_SELECTED_COLUMN && value instanceof Boolean) {
                info.setChecked((Boolean) value);
                info.setToAbstract((Boolean) value);
                if (info.isPrivate()) {
                    info.setToAbstract(false);
                }
                fireTableRowsUpdated(row, row);
            }
        }

        @Override
        public boolean isMemberEnabled(CangjieMemberInfo member) {
            return true;
        }

        @Override
        public boolean isCheckedWhenDisabled(CangjieMemberInfo member) {
            return false;
        }

        @Override
        public boolean isAbstractEnabled(CangjieMemberInfo member) {
            return false;
        }

        @Override
        public boolean isAbstractWhenDisabled(CangjieMemberInfo member) {
            return false;
        }

        @Override
        public Boolean isFixedAbstract(CangjieMemberInfo member) {
            return Boolean.FALSE;
        }

        @Override
        public int checkForProblems(@NotNull CangjieMemberInfo member) {
            return 0;
        }

        @Override
        public String getTooltipText(CangjieMemberInfo member) {
            return member.getDisplayName();
        }

        @Override
        public void memberInfoChanged(@NotNull MemberInfoChange<PsiElement, CangjieMemberInfo> event) {
        }

        /**
         * 设置绑定的目标表格视图。
         *
         * @param table 选择表格实例
         */
        public void setTable(CangjieMemberSelectionTable table) {
            this.myTable = table;
        }

        @Override
        public void addTableModelListener(TableModelListener l) {
        }

        @Override
        public void removeTableModelListener(TableModelListener l) {
        }

        /**
         * 获取指定行索引对应的仓颉成员数据特征。
         *
         * @param row 行索引
         * @return 成员数据特征结构
         */
        public Optional<CangjieMemberInfo> getMemberInfoAt(int row) {
            if (row >= 0 && row < myInfoList.size()) {
                return Optional.ofNullable(myInfoList.get(row));
            }
            return Optional.empty();
        }
    }

    /**
     * 单元格渲染器。
     */
    private static class CangjieTableRenderer extends ColoredTableCellRenderer {
        private final CangjieMemberSelectionTable myTable;

        public CangjieTableRenderer(CangjieMemberSelectionTable table) {
            this.myTable = table;
        }

        @Override
        protected void customizeCellRenderer(@NotNull JTable table, Object value, boolean isSelected,
                                             boolean hasFocus, int row, int column) {
            if (row < 0 || row >= myTable.myMemberInfos.size()) {
                return;
            }

            CangjieMemberInfo memberInfo = myTable.myMemberInfos.get(row);

            RowIcon rowIcon = IconManager.getInstance().createRowIcon(3);
            rowIcon.setIcon(myTable.getMemberIcon(memberInfo, 0), MEMBER_ICON_POSITION);
            myTable.setVisibilityIcon(memberInfo, rowIcon);
            rowIcon.setIcon(myTable.getOverrideIcon(memberInfo), OVERRIDE_ICON_POSITION);
            setIcon(rowIcon);

            String text = (value instanceof String) ? (String) value : "";
            append(text, SimpleTextAttributes.REGULAR_ATTRIBUTES);

            setEnabled(myTable.myMemberInfoModel.isMemberEnabled(memberInfo));
        }
    }
}