package com.burpgraphql.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import com.burpgraphql.generator.QueryGenerator;
import com.burpgraphql.schema.model.ArgDef;
import com.burpgraphql.schema.model.FieldDef;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

import static burp.api.montoya.http.message.requests.HttpRequest.httpRequestFromUrl;

public class DetailPanel extends JPanel {

    private final MontoyaApi api;
    private final ObjectMapper mapper = new ObjectMapper();

    private final JLabel signatureLabel;
    private final JLabel returnTypeLabel;
    private final JTextArea argsArea;
    private final JTextArea queryArea;
    private final JButton sendBtn;
    private final JButton sendAllBtn;

    private OperationContext currentCtx;

    public DetailPanel(MontoyaApi api) {
        this.api = api;
        setLayout(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        // ── Info panel (top) ──────────────────────────────────────────────
        signatureLabel = new JLabel("Select an operation from the tree");
        signatureLabel.setFont(signatureLabel.getFont().deriveFont(Font.BOLD, 13f));
        returnTypeLabel = new JLabel(" ");

        argsArea = new JTextArea(5, 50);
        argsArea.setEditable(false);
        argsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        argsArea.setBackground(UIManager.getColor("Panel.background"));

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.add(signatureLabel);
        infoPanel.add(Box.createVerticalStrut(4));
        infoPanel.add(returnTypeLabel);
        infoPanel.add(Box.createVerticalStrut(6));

        JPanel argsWrapper = new JPanel(new BorderLayout());
        argsWrapper.setBorder(new TitledBorder("Arguments"));
        argsWrapper.add(new JScrollPane(argsArea), BorderLayout.CENTER);
        infoPanel.add(argsWrapper);

        // ── Query panel (center) ──────────────────────────────────────────
        queryArea = new JTextArea(12, 50);
        queryArea.setEditable(true);
        queryArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JPanel queryWrapper = new JPanel(new BorderLayout());
        queryWrapper.setBorder(new TitledBorder("Generated Query  (editable before sending)"));
        queryWrapper.add(new JScrollPane(queryArea), BorderLayout.CENTER);

        // ── Button panel (bottom) ─────────────────────────────────────────
        sendBtn    = new JButton("Send to Repeater");
        sendAllBtn = new JButton("Send to Repeater (all in group)");
        sendBtn.setEnabled(false);
        sendAllBtn.setEnabled(false);

        sendBtn.addActionListener(e -> sendCurrent());
        sendAllBtn.addActionListener(e -> sendAll());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        btnPanel.add(sendBtn);
        btnPanel.add(sendAllBtn);

        // ── Layout ────────────────────────────────────────────────────────
        JPanel center = new JPanel(new BorderLayout(6, 6));
        center.add(infoPanel, BorderLayout.NORTH);
        center.add(queryWrapper, BorderLayout.CENTER);

        add(center, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);
    }

    /** Called by MainTab when a tree node is selected. Must run on EDT. */
    public void showOperation(OperationContext ctx) {
        this.currentCtx = ctx;
        FieldDef field = ctx.field;

        signatureLabel.setText(field.name + buildArgSig(field)
                + ": " + (field.type != null ? field.type.toGraphQLString() : "?"));
        returnTypeLabel.setText("Return type: "
                + (field.type != null ? field.type.toGraphQLString() : "?"));

        // Args table
        StringBuilder sb = new StringBuilder();
        if (field.hasArgs()) {
            for (ArgDef arg : field.args) {
                sb.append(String.format("  %-20s  %-25s  %s",
                        arg.name,
                        arg.getTypeString(),
                        arg.isRequired() ? "[REQUIRED]" : "[optional]"));
                if (arg.defaultValue != null && !arg.defaultValue.isEmpty()) {
                    sb.append("  default=").append(arg.defaultValue);
                }
                sb.append('\n');
            }
        } else {
            sb.append("  (none)");
        }
        argsArea.setText(sb.toString());

        // Generated query
        QueryGenerator gen = new QueryGenerator(ctx.typeMap);
        queryArea.setText(gen.generate(field, ctx.operationType));
        queryArea.setCaretPosition(0);

        sendBtn.setEnabled(true);
        sendAllBtn.setEnabled(true);
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private String buildArgSig(FieldDef field) {
        if (!field.hasArgs()) return "";
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < field.args.size(); i++) {
            if (i > 0) sb.append(", ");
            ArgDef a = field.args.get(i);
            sb.append(a.name).append(": ").append(a.getTypeString());
        }
        return sb.append(")").toString();
    }

    private void sendCurrent() {
        if (currentCtx == null) return;
        sendToRepeater(queryArea.getText(), currentCtx.field.name, currentCtx.endpointUrl);
    }

    private void sendAll() {
        if (currentCtx == null || currentCtx.groupSiblings == null) return;
        QueryGenerator gen = new QueryGenerator(currentCtx.typeMap);
        for (OperationContext sibling : currentCtx.groupSiblings) {
            String q = gen.generate(sibling.field, sibling.operationType);
            sendToRepeater(q, sibling.field.name, sibling.endpointUrl);
        }
    }

    private void sendToRepeater(String query, String opName, String endpointUrl) {
        try {
            String body = mapper.writeValueAsString(
                    new java.util.LinkedHashMap<String, Object>() {{
                        put("query", query);
                        put("variables", new java.util.HashMap<>());
                    }}
            );

            HttpRequest request = httpRequestFromUrl(endpointUrl)
                    .withMethod("POST")
                    .withAddedHeader("Content-Type", "application/json")
                    .withBody(body);

            api.repeater().sendToRepeater(request, "GraphQL: " + opName);
        } catch (Exception ex) {
            api.logging().logToError("Send to Repeater failed: " + ex.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Error sending to Repeater:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
