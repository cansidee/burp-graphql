package com.burpgraphql.ui;

import burp.api.montoya.MontoyaApi;
import com.burpgraphql.http.SchemaFetcher;
import com.burpgraphql.schema.IntrospectionParser;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutionException;

public class MainTab {

    private final MontoyaApi api;
    private final JPanel root;
    private final JTextField urlField;
    private final JLabel statusLabel;
    private final SchemaTreePanel treePanel;
    private final DetailPanel detailPanel;
    private final SchemaFetcher fetcher;

    public MainTab(MontoyaApi api) {
        this.api = api;
        this.fetcher = new SchemaFetcher(api);

        root = new JPanel(new BorderLayout(4, 4));

        // ── Top bar ───────────────────────────────────────────────────────
        urlField = new JTextField("http://localhost:4000/graphql", 50);
        JButton fetchBtn = new JButton("Fetch Schema");
        statusLabel = new JLabel("Ready");
        statusLabel.setForeground(Color.GRAY);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        top.add(new JLabel("Endpoint:"));
        top.add(urlField);
        top.add(fetchBtn);
        top.add(statusLabel);

        // ── Split pane ────────────────────────────────────────────────────
        detailPanel = new DetailPanel(api);
        treePanel   = new SchemaTreePanel(detailPanel::showOperation);

        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, treePanel, detailPanel);
        split.setDividerLocation(300);
        split.setResizeWeight(0.25);
        split.setOneTouchExpandable(true);

        root.add(top, BorderLayout.NORTH);
        root.add(split, BorderLayout.CENTER);

        fetchBtn.addActionListener(e -> fetchSchema());
        urlField.addActionListener(e -> fetchSchema());
    }

    /** The Component to register with Burp's suite tab system. */
    public Component uiComponent() {
        return root;
    }

    private void fetchSchema() {
        String url = urlField.getText().trim();
        if (url.isEmpty()) {
            setStatus("Error: URL is empty", true);
            return;
        }

        setStatus("Loading…", false);

        new SwingWorker<IntrospectionParser.ParsedSchema, Void>() {
            @Override
            protected IntrospectionParser.ParsedSchema doInBackground() throws Exception {
                return fetcher.fetch(url);
            }

            @Override
            protected void done() {
                try {
                    IntrospectionParser.ParsedSchema schema = get();
                    treePanel.loadSchema(schema, url);

                    int total = schema.queries.size()
                            + schema.mutations.size()
                            + schema.subscriptions.size();
                    setStatus("Loaded: " + total + " operations ("
                            + schema.queries.size() + "Q / "
                            + schema.mutations.size() + "M / "
                            + schema.subscriptions.size() + "S)", false);
                } catch (ExecutionException ee) {
                    String msg = ee.getCause() != null
                            ? ee.getCause().getMessage() : ee.getMessage();
                    setStatus("Error: " + msg, true);
                    api.logging().logToError("GraphQL Schema Parser: " + msg);
                } catch (Exception ex) {
                    setStatus("Error: " + ex.getMessage(), true);
                    api.logging().logToError("GraphQL Schema Parser: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void setStatus(String text, boolean error) {
        statusLabel.setText(text);
        statusLabel.setForeground(error ? Color.RED : new Color(0, 120, 0));
    }
}
