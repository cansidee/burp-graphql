package com.burpgraphql.ui;

import com.burpgraphql.schema.IntrospectionParser;
import com.burpgraphql.schema.model.FieldDef;
import com.burpgraphql.schema.model.TypeDef;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class SchemaTreePanel extends JPanel {

    private static final String[] GROUP_ORDER = {
        "No Arguments", "Optional Args Only", "Required Args"
    };

    private final Consumer<OperationContext> selectionCallback;
    private final JTree tree;
    private final DefaultTreeModel treeModel;

    public SchemaTreePanel(Consumer<OperationContext> selectionCallback) {
        this.selectionCallback = selectionCallback;

        setLayout(new BorderLayout());

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("GraphQL Schema");
        treeModel = new DefaultTreeModel(root);
        tree = new JTree(treeModel);
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(
                javax.swing.tree.TreeSelectionModel.SINGLE_TREE_SELECTION);

        tree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node =
                    (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (node == null || !node.isLeaf()) return;
            if (node.getUserObject() instanceof OperationContext ctx) {
                selectionCallback.accept(ctx);
            }
        });

        add(new JScrollPane(tree), BorderLayout.CENTER);
        setPreferredSize(new Dimension(300, 600));
    }

    /** Called from the EDT after a successful schema fetch. */
    public void loadSchema(IntrospectionParser.ParsedSchema schema, String endpointUrl) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("GraphQL Schema");

        if (!schema.queries.isEmpty()) {
            DefaultMutableTreeNode qNode = new DefaultMutableTreeNode("Queries");
            addOperations(qNode, schema.queries, "query", schema.typeMap, endpointUrl);
            root.add(qNode);
        }
        if (!schema.mutations.isEmpty()) {
            DefaultMutableTreeNode mNode = new DefaultMutableTreeNode("Mutations");
            addOperations(mNode, schema.mutations, "mutation", schema.typeMap, endpointUrl);
            root.add(mNode);
        }
        if (!schema.subscriptions.isEmpty()) {
            DefaultMutableTreeNode sNode = new DefaultMutableTreeNode("Subscriptions");
            addOperations(sNode, schema.subscriptions, "subscription", schema.typeMap, endpointUrl);
            root.add(sNode);
        }

        treeModel.setRoot(root);

        // Expand everything: iterate forward; new rows appear as parents expand
        int row = 0;
        while (row < tree.getRowCount()) {
            tree.expandRow(row++);
        }
    }

    private void addOperations(DefaultMutableTreeNode categoryNode,
                                List<FieldDef> fields,
                                String operationType,
                                Map<String, TypeDef> typeMap,
                                String endpointUrl) {
        // Group fields preserving insertion order
        Map<String, List<FieldDef>> groups = new LinkedHashMap<>();
        for (String g : GROUP_ORDER) groups.put(g, new ArrayList<>());
        for (FieldDef field : fields) groups.get(field.getGroup()).add(field);

        for (String groupName : GROUP_ORDER) {
            List<FieldDef> groupFields = groups.get(groupName);
            if (groupFields.isEmpty()) continue;

            DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(groupName);
            categoryNode.add(groupNode);

            // Shared mutable list: all siblings point to the same object,
            // so by the time the user clicks a node the list is fully populated.
            List<OperationContext> siblings = new ArrayList<>();
            for (FieldDef field : groupFields) {
                OperationContext ctx = new OperationContext(
                        field, operationType, typeMap, endpointUrl, siblings);
                siblings.add(ctx);
                groupNode.add(new DefaultMutableTreeNode(ctx));
            }
        }
    }
}
