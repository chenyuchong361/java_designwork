/*
Script: MainFrame.java
Purpose: Build the main application window and coordinate mind map editing, file actions, and node property editing.
Author: chenyuchong
Created: 2026-03-14
Last Updated: 2026-04-30
Dependencies: Java Swing, AWT, java.io.File, com.course.mindmap.io, com.course.mindmap.model
Usage: Launched by MindMapApp to host menus, toolbar actions, canvas interactions, and file operations.

Changelog:
- 2026-03-14 chenyuchong: Initial creation.
- 2026-04-27 Codex: Added canvas node context menu actions and synchronized them with existing edit commands. Original author: chenyuchong. Reason: allow right-click editing directly on nodes in the drawing area. Impact: backward compatible.
- 2026-04-27 Codex: Added per-node text, fill, and line color editing actions with style persistence support. Original author: chenyuchong. Reason: allow users to customize module appearance directly in the application. Impact: backward compatible.
- 2026-04-28 Codex: Simplified styling controls to fill-only actions with a no-fill option. Original author: chenyuchong. Reason: match the requested module appearance workflow while keeping default borders. Impact: backward compatible.
- 2026-04-28 Codex: Replaced the simple fill menu with a right-click node property panel for fill, border, text, and branch styles. Original author: chenyuchong. Reason: align the interaction model with mainstream mind map tools. Impact: backward compatible.
- 2026-04-28 Codex: Fixed the node property popup sizing so the panel is visible on right-click. Original author: chenyuchong. Reason: the popup content was accidentally given a zero-height preferred size. Impact: backward compatible.
- 2026-04-28 Codex: Reworked swatch rendering and color picking to use a compact palette popup with accurate live colors. Original author: chenyuchong. Reason: make the property panel reflect current colors and provide a cleaner color-selection experience. Impact: backward compatible.
- 2026-04-28 Codex: Moved palette selection into the node property popup so color picks apply immediately and return to the same property level. Original author: chenyuchong. Reason: fix missed color application and preserve the expected property-panel workflow. Impact: backward compatible.
- 2026-04-30 温文辉: Added explicit deselection controls and tightened canvas/tree/property-panel state synchronization. Original author: chenyuchong. Reason: complete task B interaction flow so selection, cancellation, and structure updates stay consistent during demos. Impact: backward compatible.
*/
package com.course.mindmap.ui;

import com.course.mindmap.io.MindMapFileManager;
import com.course.mindmap.model.LayoutMode;
import com.course.mindmap.model.MindMapDocument;
import com.course.mindmap.model.MindMapNode;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public class MainFrame extends JFrame {
    private enum ColorTarget {
        FILL,
        BORDER,
        TEXT,
        BRANCH
    }

    private static final Color ROOT_FILL_COLOR = new Color(227, 241, 255);
    private static final Color ROOT_BORDER_COLOR = new Color(56, 116, 203);
    private static final Color NODE_FILL_COLOR = Color.WHITE;
    private static final Color NODE_BORDER_COLOR = new Color(114, 132, 158);
    private static final Color NODE_TEXT_COLOR = new Color(33, 43, 54);
    private static final Color BRANCH_FALLBACK_COLOR = Color.BLACK;
    private static final Dimension COLOR_BUTTON_SIZE = new Dimension(34, 22);
    private static final Dimension PALETTE_TILE_SIZE = new Dimension(28, 28);
    private static final Color[] PALETTE_COLORS = {
            new Color(255, 255, 255), new Color(245, 245, 245), new Color(224, 224, 224), new Color(189, 189, 189),
            new Color(117, 117, 117), new Color(66, 66, 66), new Color(33, 33, 33), new Color(0, 0, 0),
            new Color(255, 205, 86), new Color(255, 138, 128), new Color(129, 199, 132), new Color(77, 208, 225),
            new Color(100, 181, 246), new Color(121, 134, 203), new Color(186, 104, 200), new Color(244, 143, 177),
            new Color(255, 179, 0), new Color(255, 82, 82), new Color(76, 175, 80), new Color(0, 188, 212),
            new Color(33, 150, 243), new Color(63, 81, 181), new Color(156, 39, 176), new Color(233, 30, 99),
            new Color(255, 152, 0), new Color(244, 67, 54), new Color(46, 125, 50), new Color(0, 131, 143),
            new Color(2, 119, 189), new Color(40, 53, 147), new Color(123, 31, 162), new Color(173, 20, 87),
            new Color(255, 111, 0), new Color(211, 47, 47), new Color(27, 94, 32), new Color(0, 96, 100),
            new Color(1, 87, 155), new Color(26, 35, 126), new Color(74, 20, 140), new Color(136, 14, 79)
    };

    private final MindMapFileManager fileManager = new MindMapFileManager();
    private final MindMapCanvas canvas = new MindMapCanvas();
    private final JTree outlineTree = new JTree();
    private final JLabel titleLabel = new JLabel();
    private final JLabel statusLabel = new JLabel();
    private final JButton addChildButton = new JButton("添加子节点");
    private final JButton addSiblingButton = new JButton("添加兄弟节点");
    private final JButton renameNodeButton = new JButton("重命名");
    private final JButton clearSelectionButton = new JButton("取消选中");
    private final JButton deleteNodeButton = new JButton("删除节点");
    private final JComboBox<LayoutMode> layoutModeBox = new JComboBox<>(LayoutMode.values());
    private final Map<String, TreePath> treePathsByNodeId = new HashMap<>();

    private MindMapDocument document;
    private MindMapNode selectedNode;
    private File currentFile;
    private boolean dirty;
    private boolean syncingSelection;
    private boolean syncingLayoutBox;
    private JPopupMenu nodePropertyPopup;
    private Point nodePropertyPopupPoint;
    private ColorTarget activeColorTarget;

    public MainFrame() {
        super("思维导图绘制工具");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1380, 860);
        setMinimumSize(new Dimension(1100, 720));
        setLocationRelativeTo(null);

        setJMenuBar(createMenuBar());
        add(createToolBar(), BorderLayout.NORTH);
        add(createCenterPanel(), BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);

        wireEvents();
        createNewDocument(false);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("文件");
        fileMenu.add(createMenuItem("新建", KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), () -> createNewDocument(true)));
        fileMenu.add(createMenuItem("打开", KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK), this::openDocument));
        fileMenu.add(createMenuItem("保存", KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), () -> saveDocument(false)));
        fileMenu.add(createMenuItem("另存为", KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), () -> saveDocument(true)));
        fileMenu.add(createMenuItem("导出图片", KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK), this::exportImage));
        fileMenu.addSeparator();
        fileMenu.add(createMenuItem("退出", null, this::closeWindow));

        JMenu editMenu = new JMenu("编辑");
        editMenu.add(createMenuItem("添加子节点", KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), this::addChildNode));
        editMenu.add(createMenuItem("添加兄弟节点", KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.SHIFT_DOWN_MASK), this::addSiblingNode));
        editMenu.add(createMenuItem("重命名节点", KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), this::renameSelectedNode));
        editMenu.add(createMenuItem("取消选中", KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), this::clearSelection));
        editMenu.add(createMenuItem("删除节点", KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), this::deleteSelectedNode));

        JMenu helpMenu = new JMenu("帮助");
        helpMenu.add(createMenuItem("使用说明", null, this::showHelp));

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 12));
        toolBar.add(titleLabel);
        toolBar.addSeparator(new Dimension(14, 32));

        toolBar.add(createToolbarButton("新建", "创建新的思维导图", () -> createNewDocument(true)));
        toolBar.add(createToolbarButton("打开", "打开已有 .dt 文件", this::openDocument));
        toolBar.add(createToolbarButton("保存", "保存当前思维导图", () -> saveDocument(false)));
        toolBar.add(createToolbarButton("另存为", "保存为新文件", () -> saveDocument(true)));
        toolBar.add(createToolbarButton("导出", "导出为 PNG 或 JPG 图片", this::exportImage));
        toolBar.addSeparator(new Dimension(14, 32));

        addChildButton.setToolTipText("为当前选中节点添加子节点");
        addChildButton.addActionListener(event -> addChildNode());
        toolBar.add(addChildButton);

        addSiblingButton.setToolTipText("为当前选中节点添加兄弟节点");
        addSiblingButton.addActionListener(event -> addSiblingNode());
        toolBar.add(addSiblingButton);

        renameNodeButton.setToolTipText("修改当前节点文本");
        renameNodeButton.addActionListener(event -> renameSelectedNode());
        toolBar.add(renameNodeButton);

        clearSelectionButton.setToolTipText("取消当前节点选中状态");
        clearSelectionButton.addActionListener(event -> clearSelection());
        toolBar.add(clearSelectionButton);

        deleteNodeButton.setToolTipText("删除当前选中节点及其所有子节点");
        deleteNodeButton.addActionListener(event -> deleteSelectedNode());
        toolBar.add(deleteNodeButton);

        toolBar.addSeparator(new Dimension(14, 32));
        toolBar.add(new JLabel("布局方式: "));
        layoutModeBox.setMaximumSize(new Dimension(150, 28));
        toolBar.add(layoutModeBox);
        toolBar.add(Box.createHorizontalGlue());

        JButton helpButton = createToolbarButton("使用说明", "查看当前演示与操作说明", this::showHelp);
        toolBar.add(helpButton);

        return toolBar;
    }

    private JSplitPane createCenterPanel() {
        JScrollPane canvasScrollPane = new JScrollPane(canvas);
        canvasScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        canvasScrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        outlineTree.setRootVisible(true);
        outlineTree.setShowsRootHandles(true);
        outlineTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        JScrollPane treeScrollPane = new JScrollPane(outlineTree);

        JPanel outlinePanel = new JPanel(new BorderLayout());
        outlinePanel.setBorder(BorderFactory.createTitledBorder("结构显示区"));
        outlinePanel.setPreferredSize(new Dimension(300, 0));
        outlinePanel.add(treeScrollPane, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, canvasScrollPane, outlinePanel);
        splitPane.setResizeWeight(0.78);
        splitPane.setDividerLocation(1020);
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(true);
        return splitPane;
    }

    private JPanel createStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        panel.add(statusLabel, BorderLayout.CENTER);
        return panel;
    }

    private void wireEvents() {
        canvas.setSelectionListener(this::setSelectedNode);
        canvas.setNodeActivationListener(node -> renameSelectedNode());
        canvas.setNodeContextMenuListener((node, point) -> showCanvasNodePropertyPopup(point));

        outlineTree.addTreeSelectionListener(event -> {
            if (syncingSelection) {
                return;
            }
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) outlineTree.getLastSelectedPathComponent();
            MindMapNode node = treeNode == null ? null : (MindMapNode) treeNode.getUserObject();
            setSelectedNode(node);
        });

        layoutModeBox.addActionListener(event -> {
            if (syncingLayoutBox || document == null) {
                return;
            }
            LayoutMode layoutMode = (LayoutMode) layoutModeBox.getSelectedItem();
            applyLayoutMode(layoutMode);
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                closeWindow();
            }
        });
    }

    private JButton createToolbarButton(String text, String toolTip, Runnable action) {
        JButton button = new JButton(text);
        button.setFocusable(false);
        button.setToolTipText(toolTip);
        button.addActionListener(event -> action.run());
        return button;
    }

    private JMenuItem createMenuItem(String text, KeyStroke keyStroke, Runnable action) {
        JMenuItem menuItem = new JMenuItem(text);
        if (keyStroke != null) {
            menuItem.setAccelerator(keyStroke);
        }
        menuItem.addActionListener(event -> action.run());
        return menuItem;
    }

    private void createNewDocument(boolean confirmDirty) {
        if (confirmDirty && !ensureChangesHandled()) {
            return;
        }

        document = MindMapDocument.createBlank();
        currentFile = null;
        dirty = false;
        refreshDocumentView(document.getRoot());
    }

    private void openDocument() {
        if (!ensureChangesHandled()) {
            return;
        }

        JFileChooser chooser = createMindMapChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            File chosenFile = chooser.getSelectedFile();
            document = fileManager.load(chosenFile);
            currentFile = chosenFile;
            dirty = false;
            refreshDocumentView(document.getRoot());
        } catch (Exception exception) {
            showError("打开失败", exception);
        }
    }

    private boolean saveDocument(boolean forceChooser) {
        if (document == null) {
            return false;
        }

        File targetFile = currentFile;
        if (forceChooser || targetFile == null) {
            JFileChooser chooser = createMindMapChooser();
            chooser.setSelectedFile(new File(defaultFileName() + ".dt"));
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
                return false;
            }
            targetFile = ensureExtension(chooser.getSelectedFile(), "dt");
        }

        try {
            fileManager.save(document, targetFile);
            currentFile = targetFile;
            dirty = false;
            updateWindowState();
            return true;
        } catch (Exception exception) {
            showError("保存失败", exception);
            return false;
        }
    }

    private void exportImage() {
        if (document == null) {
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("导出图片");
        FileNameExtensionFilter pngFilter = new FileNameExtensionFilter("PNG 图片 (*.png)", "png");
        FileNameExtensionFilter jpgFilter = new FileNameExtensionFilter("JPG 图片 (*.jpg)", "jpg", "jpeg");
        chooser.addChoosableFileFilter(pngFilter);
        chooser.addChoosableFileFilter(jpgFilter);
        chooser.setFileFilter(pngFilter);
        chooser.setSelectedFile(new File(defaultFileName() + ".png"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        FileFilter selectedFilter = chooser.getFileFilter();
        String format = selectedFilter == jpgFilter ? "jpg" : "png";
        File targetFile = ensureExtension(chooser.getSelectedFile(), format);

        try {
            canvas.exportToImage(targetFile, format);
            statusLabel.setText("已导出图片: " + targetFile.getAbsolutePath());
        } catch (Exception exception) {
            showError("导出失败", exception);
        }
    }

    private void addChildNode() {
        if (!ensureNodeSelected()) {
            return;
        }

        String text = promptText("请输入子节点名称：", "新节点");
        if (text == null) {
            return;
        }

        MindMapNode child = selectedNode.addChild(text);
        markDocumentDirtyAndRefresh(child);
    }

    private void addSiblingNode() {
        if (!ensureNodeSelected()) {
            return;
        }
        if (selectedNode.isRoot()) {
            showMessage("中心节点不能添加兄弟节点。");
            return;
        }

        String text = promptText("请输入兄弟节点名称：", "新节点");
        if (text == null) {
            return;
        }

        MindMapNode sibling = selectedNode.addSiblingAfter(text);
        markDocumentDirtyAndRefresh(sibling);
    }

    private void renameSelectedNode() {
        if (!ensureNodeSelected()) {
            return;
        }

        String text = promptText("请输入新的节点名称：", selectedNode.getText());
        if (text == null) {
            return;
        }

        selectedNode.setText(text);
        if (selectedNode.isRoot()) {
            document.setTitle(text);
        }
        markDocumentDirtyAndRefresh(selectedNode);
    }

    private void deleteSelectedNode() {
        if (!ensureNodeSelected()) {
            return;
        }
        if (selectedNode.isRoot()) {
            showMessage("中心节点不能删除。");
            return;
        }

        int option = JOptionPane.showConfirmDialog(
                this,
                "删除后将同时移除该节点的所有子节点，是否继续？",
                "确认删除",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (option != JOptionPane.YES_OPTION) {
            return;
        }

        MindMapNode parent = selectedNode.getParent();
        parent.removeChild(selectedNode);
        markDocumentDirtyAndRefresh(parent);
    }

    private void clearSelection() {
        if (selectedNode == null) {
            return;
        }
        activeColorTarget = null;
        hideNodePropertyPopup();
        setSelectedNode(null);
    }

    private void applySelectedNodeFillColor(Color selectedColor) {
        selectedNode.setFillColorHex(toColorHex(selectedColor));
        activeColorTarget = null;
        markDocumentDirtyAndRefresh(selectedNode);
        reopenNodePropertyPopup();
    }

    private void setSelectedNodeNoFill() {
        if (!ensureNodeSelected()) {
            return;
        }
        if (selectedNode.isFillTransparent()) {
            return;
        }

        selectedNode.setFillTransparent(true);
        activeColorTarget = null;
        markDocumentDirtyAndRefresh(selectedNode);
        reopenNodePropertyPopup();
    }

    private void resetSelectedNodeFill() {
        if (!ensureNodeSelected()) {
            return;
        }
        if (!selectedNode.hasCustomFillStyle()) {
            return;
        }

        selectedNode.clearCustomFillStyle();
        activeColorTarget = null;
        markDocumentDirtyAndRefresh(selectedNode);
        reopenNodePropertyPopup();
    }

    private void applySelectedNodeBorderColor(Color selectedColor) {
        selectedNode.setBorderColorHex(toColorHex(selectedColor));
        activeColorTarget = null;
        markDocumentDirtyAndRefresh(selectedNode);
        reopenNodePropertyPopup();
    }

    private void resetSelectedNodeBorder() {
        if (!ensureNodeSelected()) {
            return;
        }
        if (!selectedNode.hasCustomBorderStyle()) {
            return;
        }

        selectedNode.clearCustomBorderStyle();
        activeColorTarget = null;
        markDocumentDirtyAndRefresh(selectedNode);
        reopenNodePropertyPopup();
    }

    private void applySelectedNodeTextColor(Color selectedColor) {
        selectedNode.setTextColorHex(toColorHex(selectedColor));
        activeColorTarget = null;
        markDocumentDirtyAndRefresh(selectedNode);
        reopenNodePropertyPopup();
    }

    private void resetSelectedNodeTextStyle() {
        if (!ensureNodeSelected()) {
            return;
        }
        if (!selectedNode.hasCustomTextStyle()) {
            return;
        }

        selectedNode.clearCustomTextStyle();
        activeColorTarget = null;
        markDocumentDirtyAndRefresh(selectedNode);
        reopenNodePropertyPopup();
    }

    private void updateSelectedNodeFontSize(int fontSize) {
        if (!ensureNodeSelected()) {
            return;
        }
        if (selectedNode.getFontSize() == fontSize) {
            return;
        }

        selectedNode.setFontSize(fontSize);
        markDocumentDirtyAndRefresh(selectedNode);
        reopenNodePropertyPopup();
    }

    private void updateSelectedNodeBold(boolean bold) {
        if (!ensureNodeSelected()) {
            return;
        }
        if (selectedNode.isBold() == bold) {
            return;
        }

        selectedNode.setBold(bold);
        markDocumentDirtyAndRefresh(selectedNode);
        reopenNodePropertyPopup();
    }

    private void applySelectedNodeBranchColor(Color selectedColor) {
        selectedNode.setBranchColorHex(toColorHex(selectedColor));
        activeColorTarget = null;
        markDocumentDirtyAndRefresh(selectedNode);
        reopenNodePropertyPopup();
    }

    private void resetSelectedNodeBranchColor() {
        if (!ensureNodeSelected() || selectedNode.isRoot()) {
            return;
        }
        if (!selectedNode.hasCustomBranchStyle()) {
            return;
        }

        selectedNode.clearCustomBranchStyle();
        activeColorTarget = null;
        markDocumentDirtyAndRefresh(selectedNode);
        reopenNodePropertyPopup();
    }

    private void showCanvasNodePropertyPopup(Point point) {
        if (selectedNode == null) {
            return;
        }
        nodePropertyPopupPoint = new Point(point.x + 8, point.y + 2);
        activeColorTarget = null;
        reopenNodePropertyPopup();
    }

    private void hideNodePropertyPopup() {
        if (nodePropertyPopup != null) {
            nodePropertyPopup.setVisible(false);
            nodePropertyPopup = null;
        }
        nodePropertyPopupPoint = null;
    }

    private void reopenNodePropertyPopup() {
        if (selectedNode == null || nodePropertyPopupPoint == null) {
            return;
        }

        hideNodePropertyPopup();

        nodePropertyPopup = new JPopupMenu();
        nodePropertyPopup.setBorder(BorderFactory.createLineBorder(new Color(214, 214, 214)));
        nodePropertyPopup.add(buildNodePropertyPanel(nodePropertyPopup));
        nodePropertyPopup.show(canvas, nodePropertyPopupPoint.x, nodePropertyPopupPoint.y);
    }

    private JPanel buildNodePropertyPanel(JPopupMenu popupMenu) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("节点属性");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(10));
        panel.add(buildActionSection(popupMenu));
        panel.add(createSectionSeparator());
        panel.add(buildFillSection());
        panel.add(createSectionSeparator());
        panel.add(buildBorderSection());
        panel.add(createSectionSeparator());
        panel.add(buildTextSection());
        panel.add(createSectionSeparator());
        panel.add(buildBranchSection());
        Dimension preferredSize = panel.getPreferredSize();
        panel.setPreferredSize(new Dimension(Math.max(300, preferredSize.width), preferredSize.height));
        return panel;
    }

    private JPanel buildActionSection(JPopupMenu popupMenu) {
        JPanel section = createSectionPanel();
        section.add(createSectionTitle("操作"));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.setOpaque(false);
        buttons.add(createPopupActionButton("添加子节点", true, popupMenu, this::addChildNode));
        buttons.add(createPopupActionButton("添加兄弟节点", selectedNode != null && !selectedNode.isRoot(), popupMenu, this::addSiblingNode));
        buttons.add(createPopupActionButton("删除节点", selectedNode != null && !selectedNode.isRoot(), popupMenu, this::deleteSelectedNode));
        section.add(buttons);
        return section;
    }

    private JPanel buildFillSection() {
        JPanel section = createSectionPanel();
        section.add(createSectionTitle("填充"));
        JPanel row = createPropertyRow("背景");
        row.add(createColorSwatchButton(resolveFillPreviewColor(selectedNode), "选择填充颜色", ColorTarget.FILL));
        row.add(createSmallButton("无填充", this::setSelectedNodeNoFill));
        row.add(createSmallButton("默认", this::resetSelectedNodeFill));
        section.add(row);
        if (activeColorTarget == ColorTarget.FILL) {
            section.add(createInlinePalettePanel("选择填充颜色", resolveFillPreviewColor(selectedNode), ColorTarget.FILL));
        }
        return section;
    }

    private JPanel buildBorderSection() {
        JPanel section = createSectionPanel();
        section.add(createSectionTitle("边框"));
        JPanel row = createPropertyRow("颜色");
        row.add(createColorSwatchButton(resolveBorderPreviewColor(selectedNode), "选择边框颜色", ColorTarget.BORDER));
        row.add(createSmallButton("自动", this::resetSelectedNodeBorder));
        section.add(row);
        if (activeColorTarget == ColorTarget.BORDER) {
            section.add(createInlinePalettePanel("选择边框颜色", resolveBorderPreviewColor(selectedNode), ColorTarget.BORDER));
        }
        return section;
    }

    private JPanel buildTextSection() {
        JPanel section = createSectionPanel();
        section.add(createSectionTitle("文本"));

        JPanel sizeRow = createPropertyRow("字号");
        JSpinner fontSizeSpinner = new JSpinner(new SpinnerNumberModel(selectedNode.getFontSize(), 8, 72, 1));
        fontSizeSpinner.setPreferredSize(new Dimension(70, 26));
        fontSizeSpinner.addChangeListener(event -> updateSelectedNodeFontSize((Integer) fontSizeSpinner.getValue()));
        sizeRow.add(fontSizeSpinner);

        JCheckBox boldCheckBox = new JCheckBox("加粗", selectedNode.isBold());
        boldCheckBox.setOpaque(false);
        boldCheckBox.addActionListener(event -> updateSelectedNodeBold(boldCheckBox.isSelected()));
        sizeRow.add(boldCheckBox);
        section.add(sizeRow);

        JPanel colorRow = createPropertyRow("颜色");
        colorRow.add(createColorSwatchButton(resolveTextPreviewColor(selectedNode), "选择文字颜色", ColorTarget.TEXT));
        colorRow.add(createSmallButton("默认", this::resetSelectedNodeTextStyle));
        section.add(colorRow);
        if (activeColorTarget == ColorTarget.TEXT) {
            section.add(createInlinePalettePanel("选择文字颜色", resolveTextPreviewColor(selectedNode), ColorTarget.TEXT));
        }
        return section;
    }

    private JPanel buildBranchSection() {
        JPanel section = createSectionPanel();
        section.add(createSectionTitle("分支"));

        if (selectedNode != null && selectedNode.isRoot()) {
            JPanel row = createPropertyRow("说明");
            JLabel label = new JLabel("根节点没有父分支颜色。");
            label.setForeground(new Color(102, 102, 102));
            row.add(label);
            section.add(row);
            return section;
        }

        JPanel row = createPropertyRow("颜色");
        row.add(createColorSwatchButton(resolveBranchPreviewColor(selectedNode), "选择分支颜色", ColorTarget.BRANCH));
        row.add(createSmallButton("自动", this::resetSelectedNodeBranchColor));
        section.add(row);
        if (activeColorTarget == ColorTarget.BRANCH) {
            section.add(createInlinePalettePanel("选择分支颜色", resolveBranchPreviewColor(selectedNode), ColorTarget.BRANCH));
        }
        return section;
    }

    private JPanel createSectionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private JLabel createSectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JPanel createPropertyRow(String labelText) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(labelText, SwingConstants.LEFT);
        label.setPreferredSize(new Dimension(44, 24));
        row.add(label);
        return row;
    }

    private JSeparator createSectionSeparator() {
        JSeparator separator = new JSeparator();
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        separator.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        return separator;
    }

    private JButton createPopupActionButton(String text, boolean enabled, JPopupMenu popupMenu, Runnable action) {
        JButton button = new JButton(text);
        button.setEnabled(enabled);
        button.addActionListener(event -> {
            popupMenu.setVisible(false);
            action.run();
        });
        return button;
    }

    private JButton createColorSwatchButton(Color color, String title, ColorTarget target) {
        JButton button = new JButton(new ImageIcon(createColorSwatchImage(color, COLOR_BUTTON_SIZE.width, COLOR_BUTTON_SIZE.height)));
        button.setPreferredSize(COLOR_BUTTON_SIZE);
        button.setMinimumSize(COLOR_BUTTON_SIZE);
        button.setMaximumSize(COLOR_BUTTON_SIZE);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setOpaque(false);
        button.setToolTipText(title);
        button.addActionListener(event -> {
            activeColorTarget = activeColorTarget == target ? null : target;
            reopenNodePropertyPopup();
        });
        return button;
    }

    private JButton createSmallButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(event -> action.run());
        return button;
    }

    private JPanel createInlinePalettePanel(String title, Color initialColor, ColorTarget target) {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(218, 218, 218)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        content.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel grid = new JPanel(new GridLayout(5, 8, 4, 4));
        grid.setOpaque(false);
        for (Color paletteColor : PALETTE_COLORS) {
            grid.add(createPaletteColorTile(paletteColor, target));
        }
        content.add(grid);
        content.add(Box.createVerticalStrut(10));

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        footer.setOpaque(false);

        JLabel hexLabel = new JLabel(toColorHex(initialColor));
        hexLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(214, 214, 214)),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        footer.add(hexLabel);
        footer.add(createColorPreviewChip(initialColor));

        JButton moreButton = new JButton("更多颜色");
        moreButton.addActionListener(event -> {
            Color selectedColor = JColorChooser.showDialog(this, title, initialColor);
            if (selectedColor == null) {
                return;
            }
            applyColorSelection(target, selectedColor);
        });
        footer.add(moreButton);

        content.add(footer);
        return content;
    }

    private JPanel createPaletteColorTile(Color color, ColorTarget target) {
        JPanel tile = new JPanel();
        tile.setPreferredSize(PALETTE_TILE_SIZE);
        tile.setBackground(color);
        tile.setBorder(BorderFactory.createLineBorder(new Color(206, 206, 206)));
        tile.setToolTipText(toColorHex(color));
        tile.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                applyColorSelection(target, color);
            }

            @Override
            public void mouseEntered(MouseEvent event) {
                tile.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.focusColor") != null
                        ? UIManager.getColor("Component.focusColor")
                        : new Color(76, 132, 255), 2));
            }

            @Override
            public void mouseExited(MouseEvent event) {
                tile.setBorder(BorderFactory.createLineBorder(new Color(206, 206, 206)));
            }
        });
        return tile;
    }

    private JPanel createColorPreviewChip(Color color) {
        JPanel chip = new JPanel();
        chip.setPreferredSize(new Dimension(24, 24));
        chip.setBackground(color);
        chip.setBorder(BorderFactory.createLineBorder(new Color(90, 90, 90)));
        return chip;
    }

    private BufferedImage createColorSwatchImage(Color color, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = image.createGraphics();
        try {
            graphics2D.setColor(Color.WHITE);
            graphics2D.fillRoundRect(0, 0, width, height, 8, 8);
            graphics2D.setColor(color);
            graphics2D.fillRoundRect(3, 3, width - 6, height - 6, 6, 6);
            graphics2D.setColor(new Color(90, 90, 90));
            graphics2D.drawRoundRect(3, 3, width - 7, height - 7, 6, 6);
        } finally {
            graphics2D.dispose();
        }
        return image;
    }

    private void applyColorSelection(ColorTarget target, Color color) {
        if (target == null) {
            return;
        }
        switch (target) {
            case FILL -> applySelectedNodeFillColor(color);
            case BORDER -> applySelectedNodeBorderColor(color);
            case TEXT -> applySelectedNodeTextColor(color);
            case BRANCH -> applySelectedNodeBranchColor(color);
        }
    }

    private void applyLayoutMode(LayoutMode layoutMode) {
        if (document == null || layoutMode == null) {
            return;
        }

        if (selectedNode == null || !selectedNode.isRoot()) {
            syncingLayoutBox = true;
            layoutModeBox.setSelectedItem(document.getLayoutMode());
            syncingLayoutBox = false;
            showMessage("请先选中中心节点，再切换布局方式。");
            return;
        }

        if (document.getLayoutMode() == layoutMode) {
            return;
        }

        document.setLayoutMode(layoutMode);
        dirty = true;
        canvas.refreshLayout();
        updateWindowState();
    }

    private void refreshDocumentView(MindMapNode preferredSelection) {
        canvas.setDocument(document);
        rebuildOutlineTree();
        syncingLayoutBox = true;
        layoutModeBox.setSelectedItem(document.getLayoutMode());
        syncingLayoutBox = false;
        setSelectedNode(preferredSelection == null ? document.getRoot() : preferredSelection);
        canvas.refreshLayout();
        updateWindowState();
    }

    private void rebuildOutlineTree() {
        if (document == null) {
            outlineTree.setModel(null);
            treePathsByNodeId.clear();
            return;
        }

        DefaultMutableTreeNode rootTreeNode = buildTreeNode(document.getRoot());
        DefaultTreeModel model = new DefaultTreeModel(rootTreeNode);
        outlineTree.setModel(model);
        treePathsByNodeId.clear();
        collectTreePaths(rootTreeNode, new TreePath(rootTreeNode));
        expandAllRows();
    }

    private DefaultMutableTreeNode buildTreeNode(MindMapNode node) {
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(node);
        for (MindMapNode child : node.getChildren()) {
            treeNode.add(buildTreeNode(child));
        }
        return treeNode;
    }

    private void collectTreePaths(DefaultMutableTreeNode treeNode, TreePath path) {
        MindMapNode node = (MindMapNode) treeNode.getUserObject();
        treePathsByNodeId.put(node.getId(), path);

        Enumeration<?> children = treeNode.children();
        while (children.hasMoreElements()) {
            DefaultMutableTreeNode childTreeNode = (DefaultMutableTreeNode) children.nextElement();
            collectTreePaths(childTreeNode, path.pathByAddingChild(childTreeNode));
        }
    }

    private void expandAllRows() {
        int row = 0;
        while (row < outlineTree.getRowCount()) {
            outlineTree.expandRow(row);
            row++;
        }
    }

    private void setSelectedNode(MindMapNode node) {
        selectedNode = node;
        if (selectedNode == null) {
            activeColorTarget = null;
            hideNodePropertyPopup();
        } else if (nodePropertyPopupPoint != null && nodePropertyPopup != null && nodePropertyPopup.isVisible()) {
            reopenNodePropertyPopup();
        }
        syncingSelection = true;
        try {
            canvas.setSelectedNode(node, true);
            if (node == null) {
                outlineTree.clearSelection();
            } else {
                TreePath treePath = treePathsByNodeId.get(node.getId());
                if (treePath != null) {
                    outlineTree.setSelectionPath(treePath);
                    outlineTree.scrollPathToVisible(treePath);
                }
            }
        } finally {
            syncingSelection = false;
        }
        updateWindowState();
    }

    private void updateWindowState() {
        String documentTitle = document == null ? "未命名导图" : document.getTitle();
        titleLabel.setText("导图名称: " + documentTitle);
        layoutModeBox.setEnabled(selectedNode != null && selectedNode.isRoot());
        addChildButton.setEnabled(selectedNode != null);
        addSiblingButton.setEnabled(selectedNode != null && !selectedNode.isRoot());
        renameNodeButton.setEnabled(selectedNode != null);
        clearSelectionButton.setEnabled(selectedNode != null);
        deleteNodeButton.setEnabled(selectedNode != null && !selectedNode.isRoot());

        String fileText = currentFile == null ? "未保存" : currentFile.getName();
        String selectionText = selectedNode == null ? "未选中节点" : "当前节点: " + selectedNode.getText();
        int totalNodes = document == null ? 0 : document.getRoot().countNodes();
        String dirtyText = dirty ? " | 有未保存修改" : "";
        statusLabel.setText(selectionText + " | 节点数: " + totalNodes + " | 文件: " + fileText + dirtyText);

        setTitle((dirty ? "* " : "") + documentTitle + " - 思维导图绘制工具");
    }

    private boolean ensureChangesHandled() {
        if (!dirty) {
            return true;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "当前导图有未保存的修改，是否先保存？",
                "未保存修改",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION) {
            return false;
        }
        if (choice == JOptionPane.YES_OPTION) {
            return saveDocument(false);
        }
        return true;
    }

    private JFileChooser createMindMapChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("思维导图文件 (*.dt)", "dt"));
        return chooser;
    }

    private File ensureExtension(File file, String extension) {
        Objects.requireNonNull(file, "file");
        String lowerName = file.getName().toLowerCase(Locale.ROOT);
        String normalizedExtension = "." + extension.toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(normalizedExtension)) {
            return file;
        }
        return new File(file.getParentFile(), file.getName() + normalizedExtension);
    }

    private String defaultFileName() {
        String title = document == null ? "mind-map" : document.getTitle();
        String normalized = title.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return normalized.isEmpty() ? "mind-map" : normalized;
    }

    private String promptText(String message, String initialValue) {
        String value = JOptionPane.showInputDialog(this, message, initialValue);
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.isEmpty()) {
            showMessage("节点名称不能为空。");
            return null;
        }
        return normalized;
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "提示", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String title, Exception exception) {
        JOptionPane.showMessageDialog(
                this,
                title + "：" + exception.getMessage(),
                "错误",
                JOptionPane.ERROR_MESSAGE
        );
    }

    private void showHelp() {
        String helpText = """
                1. 新建后会自动生成一个中心节点。
                2. 单击节点可选中，双击节点可快速重命名，按 Esc 或点击“取消选中”可清除当前选中。
                3. 选中中心节点可以添加子节点和切换布局方式。
                4. 选中普通节点可以添加子节点、兄弟节点，或删除该节点。
                5. 左侧结构显示区与绘图区会同步选中与滚动定位，适合演示和排查层级关系。
                6. 右击节点会弹出属性框，可设置填充、边框、文本和分支颜色。
                7. 文本属性支持字号、颜色和加粗；分支颜色未设置时会自动跟随当前填充色，无填充时回退为黑色。
                8. 保存文件使用自定义 .dt 扩展名，导出支持 PNG 和 JPG。
                """;
        JOptionPane.showMessageDialog(this, helpText, "使用说明", JOptionPane.INFORMATION_MESSAGE);
    }

    private void closeWindow() {
        if (!ensureChangesHandled()) {
            return;
        }
        dispose();
    }

    private boolean ensureNodeSelected() {
        if (selectedNode != null) {
            return true;
        }
        showMessage("请先选中一个节点。");
        return false;
    }

    private void markDocumentDirtyAndRefresh(MindMapNode preferredSelection) {
        dirty = true;
        refreshDocumentView(preferredSelection);
    }

    private Color defaultFillColor(MindMapNode node) {
        return node != null && node.isRoot() ? ROOT_FILL_COLOR : NODE_FILL_COLOR;
    }

    private Color defaultBorderColor(MindMapNode node) {
        return node != null && node.isRoot() ? ROOT_BORDER_COLOR : NODE_BORDER_COLOR;
    }

    private Color resolveFillPreviewColor(MindMapNode node) {
        return parseColor(node.getFillColorHex(), defaultFillColor(node));
    }

    private Color resolveBorderPreviewColor(MindMapNode node) {
        if (node.getBorderColorHex() != null) {
            return parseColor(node.getBorderColorHex(), defaultBorderColor(node));
        }
        if (!node.isFillTransparent()) {
            return resolveFillPreviewColor(node);
        }
        return defaultBorderColor(node);
    }

    private Color resolveTextPreviewColor(MindMapNode node) {
        return parseColor(node.getTextColorHex(), NODE_TEXT_COLOR);
    }

    private Color resolveBranchPreviewColor(MindMapNode node) {
        if (node.getBranchColorHex() != null) {
            return parseColor(node.getBranchColorHex(), BRANCH_FALLBACK_COLOR);
        }
        if (!node.isFillTransparent()) {
            return resolveFillPreviewColor(node);
        }
        return BRANCH_FALLBACK_COLOR;
    }

    private Color parseColor(String colorHex, Color fallbackColor) {
        if (colorHex == null || colorHex.isBlank()) {
            return fallbackColor;
        }
        try {
            return Color.decode(colorHex);
        } catch (NumberFormatException exception) {
            return fallbackColor;
        }
    }

    private String toColorHex(Color color) {
        return String.format(Locale.ROOT, "#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }
}
