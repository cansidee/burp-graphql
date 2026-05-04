package com.burpgraphql.extension;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import com.burpgraphql.ui.MainTab;

public class GraphQLSchemaParser implements BurpExtension {

    @Override
    public void initialize(MontoyaApi api) {
        api.extension().setName("GraphQL Schema Parser");

        MainTab tab = new MainTab(api);
        // registerSuiteTab(String caption, Component component)
        api.userInterface().registerSuiteTab("GraphQL Schema Parser", tab.uiComponent());

        api.logging().logToOutput("GraphQL Schema Parser loaded.");
    }
}
