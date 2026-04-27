/*
Script: MainFrame.java
Purpose: Build the main application window and coordinate mind map editing, file actions, and node styling.
Author: chenyuchong
Created: 2026-03-14
Last Updated: 2026-04-27
Dependencies: Java Swing, AWT, java.io.File, com.course.mindmap.io, com.course.mindmap.model
Usage: Launched by MindMapApp to host menus, toolbar actions, canvas interactions, and file operations.

Changelog:
- 2026-03-14 chenyuchong: Initial creation.
- 2026-04-27 Codex: Added canvas node context menu actions and synchronized them with existing edit commands. Original author: chenyuchong. Reason: allow right-click editing directly on nodes in the drawing area. Impact: backward compatible.
- 2026-04-27 Codex: Added per-node text, fill, and line color editing actions with style persistence support. Original author: chenyuchong. Reason: allow users to customize module appearance directly in the application. Impact: backward compatible.
*/
package com.course.mindmap.ui;

import com.course.mindmap.io.MindMapFileManager;
import com.course.mindmap.model.LayoutMode;
import com.course.mindmap.model.MindMapDocument;
import com.course.mindmap.model.MindMapNode;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
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
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public class MainFrame extends JFrame {
    private static final Color DEFAULT_TEXT_COLOR = new Color(33, 43, 54);
    private static final Color ROOT_FILL_COLOR = new Color(227, 241, 255);
    private static final Color ROOT_LINE_COLOR = new Color(56, 116, 203);
    private static final Color NODE_FILL_COLOR = Color.WHITE;
    private static final Color NODE_LINE_COLOR = new Color(114, 132, 158);

    private final MindMapFileManager fileManager = new MindMapFileManager();
    private final MindMapCanvas canvas = new MindMapCanvas();
    private final JTree outlineTree = new JTree();
    private final JLabel titleLabel = new JLabel();
    private final JLabel statusLabel = new JLabel();
    private final JButton addChildButton = new JButton("添加子节点");
    private final JButton addSiblingButton = new JButton("添加兄弟节点");
    private final JButton renameNodeButton = new JButton("重命名");
    private final JButton deleteNodeButton = new JButton("删除节点");
    private final JComboBox<LayoutMode> layoutModeBox = new JComboBox<>(LayoutMode.values());
    private final Map<String, TreePath> treePathsByNodeId = new HashMap<>();
    private final JMenuItem canvasAddChildMenuItem = createMenuItem("添加子节点", null, this::addChildNode);
    private final JMenuItem canvasAddSiblingMenuItem = createMenuItem("添加兄弟节点", null, this::addSiblingNode);
    private final JMenuItem canvasDeleteNodeMenuItem = createMenuItem("删除节点", null, this::deleteSelectedNode);
    private final JPopupMenu canvasNodeMenu = createCanvasNodeMenu();

    private MindMapDocument document;
    private MindMapNode selectedNode;
    private File currentFile;
    private boolean dirty;
    private boolean syncingSelection;
    private boolean syncingLayoutBox;

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
        editMenu.add(createMenuItem("删除节点", KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), this::deleteSelectedNode));
        editMenu.addSeparator();
        editMenu.add(createMenuItem("设置文字颜色", null, this::chooseSelectedNodeTextColor));
        editMenu.add(createMenuItem("设置填充颜色", null, this::chooseSelectedNodeFillColor));
        editMenu.add(createMenuItem("设置线条颜色", null, this::chooseSelectedNodeLineColor));
        editMenu.add(createMenuItem("恢复默认样式", null, this::resetSelectedNodeStyle));

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

        deleteNodeButton.setToolTipText("删除当前选中节点及其所有子节点");
        deleteNodeButton.addActionListener(event -> deleteSelectedNode());
        toolBar.add(deleteNodeButton);

        toolBar.addSeparator(new Dimension(14, 32));
        toolBar.add(new JLabel("布局方式: "));
        layoutModeBox.setMaximumSize(new Dimension(150, 28));
        toolBar.add(layoutModeBox);

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
        splitPane.setResizeWeight(0.8);
        splitPane.setDividerLocation(1040);
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
        canvas.setNodeContextMenuListener((node, point) -> showCanvasNodeMenu(point));

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

    private JPopupMenu createCanvasNodeMenu() {
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.add(canvasAddChildMenuItem);
        popupMenu.add(canvasAddSiblingMenuItem);
        popupMenu.addSeparator();
        popupMenu.add(canvasDeleteNodeMenuItem);
        popupMenu.addSeparator();
        popupMenu.add(createMenuItem("设置文字颜色", null, this::chooseSelectedNodeTextColor));
        popupMenu.add(createMenuItem("设置填充颜色", null, this::chooseSelectedNodeFillColor));
        popupMenu.add(createMenuItem("设置线条颜色", null, this::chooseSelectedNodeLineColor));
        popupMenu.add(createMenuItem("恢复默认样式", null, this::resetSelectedNodeStyle));
        return popupMenu;
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

    private void chooseSelectedNodeTextColor() {
        if (!ensureNodeSelected()) {
            return;
        }
        chooseSelectedNodeColor("选择文字颜色", selectedNode.getTextColorHex(), defaultTextColor(selectedNode), selectedNode::setTextColorHex);
    }

    private void chooseSelectedNodeFillColor() {
        if (!ensureNodeSelected()) {
            return;
        }
        chooseSelectedNodeColor("选择填充颜色", selectedNode.getFillColorHex(), defaultFillColor(selectedNode), selectedNode::setFillColorHex);
    }

    private void chooseSelectedNodeLineColor() {
        if (!ensureNodeSelected()) {
            return;
        }
        chooseSelectedNodeColor("选择线条颜色", selectedNode.getLineColorHex(), defaultLineColor(selectedNode), selectedNode::setLineColorHex);
    }

    private void resetSelectedNodeStyle() {
        if (!ensureNodeSelected()) {
            return;
        }
        if (!selectedNode.hasCustomStyle()) {
            return;
        }

        selectedNode.clearCustomStyle();
        markDocumentDirtyAndRefresh(selectedNode);
    }

    private void chooseSelectedNodeColor(String dialogTitle, String currentColorHex, Color fallbackColor, Consumer<String> setter) {
        Color initialColor = parseColor(currentColorHex, fallbackColor);
        Color selectedColor = JColorChooser.showDialog(this, dialogTitle, initialColor);
        if (selectedColor == null) {
            return;
        }

        setter.accept(toColorHex(selectedColor));
        markDocumentDirtyAndRefresh(selectedNode);
    }

    private void showCanvasNodeMenu(Point point) {
        if (selectedNode == null) {
            return;
        }

        boolean rootSelected = selectedNode.isRoot();
        canvasAddChildMenuItem.setEnabled(true);
        canvasAddSiblingMenuItem.setEnabled(!rootSelected);
        canvasDeleteNodeMenuItem.setEnabled(!rootSelected);
        canvasNodeMenu.show(canvas, point.x, point.y);
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
                2. 单击节点可选中，双击节点可快速重命名。
                3. 选中中心节点可以添加子节点和切换布局方式。
                4. 选中普通节点可以添加子节点、兄弟节点，或删除该节点。
                5. 右击节点或通过“编辑”菜单可以设置文字颜色、填充颜色、线条颜色。
                6. 保存文件使用自定义 .dt 扩展名，导出支持 PNG 和 JPG。
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

    private Color defaultTextColor(MindMapNode node) {
        return DEFAULT_TEXT_COLOR;
    }

    private Color defaultFillColor(MindMapNode node) {
        return node != null && node.isRoot() ? ROOT_FILL_COLOR : NODE_FILL_COLOR;
    }

    private Color defaultLineColor(MindMapNode node) {
        return node != null && node.isRoot() ? ROOT_LINE_COLOR : NODE_LINE_COLOR;
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
