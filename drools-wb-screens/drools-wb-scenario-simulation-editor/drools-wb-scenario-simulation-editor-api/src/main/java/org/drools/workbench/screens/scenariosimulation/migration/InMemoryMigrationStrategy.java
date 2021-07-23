/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.workbench.screens.scenariosimulation.migration;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import org.drools.workbench.screens.scenariosimulation.interfaces.ThrowingConsumer;
import org.drools.workbench.screens.scenariosimulation.utils.GWTParserUtil;

import static org.drools.scenariosimulation.api.utils.ConstantsHolder.BACKGROUND_DATA_NODE;
import static org.drools.scenariosimulation.api.utils.ConstantsHolder.BACKGROUND_NODE;
import static org.drools.scenariosimulation.api.utils.ConstantsHolder.DMO_SESSION_NODE;
import static org.drools.scenariosimulation.api.utils.ConstantsHolder.EXPRESSION_ELEMENTS_NODE;
import static org.drools.scenariosimulation.api.utils.ConstantsHolder.EXPRESSION_IDENTIFIER_NODE;
import static org.drools.scenariosimulation.api.utils.ConstantsHolder.FACT_IDENTIFIER_NODE;
import static org.drools.scenariosimulation.api.utils.ConstantsHolder.FACT_MAPPINGS_NODE;
import static org.drools.scenariosimulation.api.utils.ConstantsHolder.FACT_MAPPING_NODE;
import static org.drools.scenariosimulation.api.utils.ConstantsHolder.FACT_MAPPING_VALUES_NODE;
import static org.drools.scenariosimulation.api.utils.ConstantsHolder.FACT_MAPPING_VALUE_NODE;
import static org.drools.scenariosimulation.api.utils.ConstantsHolder.FACT_MAPPING_VALUE_TYPE_NODE;
import static org.drools.scenariosimulation.api.utils.ConstantsHolder.NOT_EXPRESSION;
import static org.drools.scenariosimulation.api.utils.ConstantsHolder.SCENARIO_SIMULATION_MODEL_NODE;
import static org.drools.scenariosimulation.api.utils.ConstantsHolder.SETTINGS;
import static org.drools.scenariosimulation.api.utils.ConstantsHolder.SETTINGS_NODE;
import static org.drools.scenariosimulation.api.utils.ConstantsHolder.SIMULATION_DESCRIPTOR_NODE;
import static org.drools.scenariosimulation.api.utils.ConstantsHolder.SIMULATION_NODE;
import static org.drools.workbench.screens.scenariosimulation.migration.ScenarioSimulationXMLPersistence.getColumnWidth;

public class InMemoryMigrationStrategy implements MigrationStrategy {

    @Override
    public ThrowingConsumer<Document> from1_0to1_1() {
        return document -> {
            GWTParserUtil.replaceNodeText(document, EXPRESSION_IDENTIFIER_NODE, "type", "EXPECTED", "EXPECT");
            updateVersion(document, "1.1");
        };
    }

    @Override
    public ThrowingConsumer<Document> from1_1to1_2() {
        return document -> {
            Map<Node, List<Node>> dmoSessionNodesMap = GWTParserUtil.getNestedChildrenNodesMap(document, SIMULATION_NODE, SIMULATION_DESCRIPTOR_NODE, DMO_SESSION_NODE);
            Map<Node, List<Node>> dmnFilePathNodesMap = GWTParserUtil.getNestedChildrenNodesMap(document, SIMULATION_NODE, SIMULATION_DESCRIPTOR_NODE, "dmnFilePath");
            Map<Node, List<Node>> typeNodesMap = GWTParserUtil.getNestedChildrenNodesMap(document, SIMULATION_NODE, SIMULATION_DESCRIPTOR_NODE, "type");
            List<Node> dmoSessionNodes = dmoSessionNodesMap.values().iterator().next();
            List<Node> dmnFilePathNodes = dmnFilePathNodesMap.values().iterator().next();
            List<Node> typeNodes = typeNodesMap.values().iterator().next();
            if (!dmoSessionNodes.isEmpty() || (!dmnFilePathNodes.isEmpty() && !typeNodes.isEmpty())) {
                //
            } else {
                GWTParserUtil.createNestedNodes(document, SIMULATION_NODE, SIMULATION_DESCRIPTOR_NODE, DMO_SESSION_NODE, null);
                GWTParserUtil.createNestedNodes(document, SIMULATION_NODE, SIMULATION_DESCRIPTOR_NODE, "type", "RULE");
            }
            updateVersion(document, "1.2");
        };
    }

    @Override
    public ThrowingConsumer<Document> from1_2to1_3() {
        return document -> {
            List<Node> factMappingsNodes = GWTParserUtil.getNestedChildrenNodesList(document, SIMULATION_NODE, SIMULATION_DESCRIPTOR_NODE, FACT_MAPPINGS_NODE);
            Node factMappingsNode = factMappingsNodes.get(0);
            final List<Node> factIdentifierNodeList = GWTParserUtil.getNestedChildrenNodesList(factMappingsNode, FACT_MAPPING_NODE, FACT_IDENTIFIER_NODE);
            factIdentifierNodeList.forEach(factIdentifierNode -> {
                List<Node> factIdentifierNameList = GWTParserUtil.getChildrenNodesList(factIdentifierNode, "name");
                if (!factIdentifierNameList.isEmpty()) {
                    String factIdentifierName = factIdentifierNameList.get(0).getNodeValue();
                    Node factMappingNode = factIdentifierNode.getParentNode();
                    List<Node> expressionElementsNodeList = GWTParserUtil.getChildrenNodesList(factMappingNode, "expressionElements");
                    Node expressionElementsNode;
                    if (expressionElementsNodeList.isEmpty()) {
                        expressionElementsNode = GWTParserUtil.createNodeAtPosition(factMappingNode, "expressionElements", null, 0);
                    } else {
                        expressionElementsNode = expressionElementsNodeList.get(0);
                    }
                    Node expressionElementNode = GWTParserUtil.createNodeAtPosition(expressionElementsNode, "ExpressionElement", null, 0);
                    GWTParserUtil.createNodeAtPosition(expressionElementNode, "step", factIdentifierName, 0);
                }
            });
            updateVersion(document, "1.3");
        };
    }

    @Override
    public ThrowingConsumer<Document> from1_3to1_4() {
        return document -> {
            List<Node> typeNodes = GWTParserUtil.getNestedChildrenNodesList(document, SIMULATION_NODE, SIMULATION_DESCRIPTOR_NODE, "type");
            if (!typeNodes.isEmpty()) {
                Node typeNode = typeNodes.get(0);
                Node simulationDescriptorNode = typeNode.getParentNode();
                String defaultContent = "default";
                switch (typeNode.getNodeValue()) {
                    case "RULE":
                        if (GWTParserUtil.getChildrenNodesList(simulationDescriptorNode, "kieSession").isEmpty()) {
                            GWTParserUtil.createNodeAndAppend(simulationDescriptorNode, "kieSession", defaultContent);
                        }
                        if (GWTParserUtil.getChildrenNodesList(simulationDescriptorNode, "kieBase").isEmpty()) {
                            GWTParserUtil.createNodeAndAppend(simulationDescriptorNode, "kieBase", defaultContent);
                        }
                        if (GWTParserUtil.getChildrenNodesList(simulationDescriptorNode, "ruleFlowGroup").isEmpty()) {
                            GWTParserUtil.createNodeAndAppend(simulationDescriptorNode, "ruleFlowGroup", defaultContent);
                        }
                        break;
                    case "DMN":
                        if (GWTParserUtil.getChildrenNodesList(simulationDescriptorNode, "dmnNamespace").isEmpty()) {
                            GWTParserUtil.createNodeAndAppend(simulationDescriptorNode, "dmnNamespace", null);
                        }
                        if (GWTParserUtil.getChildrenNodesList(simulationDescriptorNode, "dmnName").isEmpty()) {
                            GWTParserUtil.createNodeAndAppend(simulationDescriptorNode, "dmnName", null);
                        }
                        break;
                    default:
                }
                if (GWTParserUtil.getChildrenNodesList(simulationDescriptorNode, "skipFromBuild").isEmpty()) {
                    GWTParserUtil.createNodeAndAppend(simulationDescriptorNode, "skipFromBuild", "false");
                }
                if (GWTParserUtil.getChildrenNodesList(simulationDescriptorNode, "fileName").isEmpty()) {
                    GWTParserUtil.createNodeAndAppend(simulationDescriptorNode, "fileName", null);
                }
            }
            updateVersion(document, "1.4");
        };
    }

    @Override
    public ThrowingConsumer<Document> from1_4to1_5() {
        return document -> {
            Node simulationDescriptorNode = GWTParserUtil.getNestedChildrenNodesList(document, "ScenarioSimulationModel", SIMULATION_NODE, SIMULATION_DESCRIPTOR_NODE).get(0);
            List<Node> dmoSessionNodesList = GWTParserUtil.getChildrenNodesList(simulationDescriptorNode, DMO_SESSION_NODE);
            if (dmoSessionNodesList.isEmpty()) {
                GWTParserUtil.createNodeAndAppend(simulationDescriptorNode, DMO_SESSION_NODE, null);
            } else {
                Node dmoSessionNode = dmoSessionNodesList.get(0);
                if (Objects.equals("default", dmoSessionNode.getNodeValue()) || Objects.equals("", dmoSessionNode.getNodeValue())) {
                    simulationDescriptorNode.removeChild(dmoSessionNode);
                }
            }
            updateVersion(document, "1.5");
        };
    }

    @Override
    public ThrowingConsumer<Document> from1_5to1_6() {
        return document -> {
            GWTParserUtil.cleanupNodes(document, "Scenario", SIMULATION_DESCRIPTOR_NODE);
            List<Node> simulationFactMappingNodeList = GWTParserUtil.getNestedChildrenNodesList(document, SIMULATION_DESCRIPTOR_NODE, FACT_MAPPINGS_NODE, FACT_MAPPING_NODE);
            for (Node simulationFactMapping : simulationFactMappingNodeList) {
                replaceReference(simulationFactMappingNodeList, simulationFactMapping, FACT_IDENTIFIER_NODE);
            }
            List<Node> scenarioFactMappingValueNodeList = GWTParserUtil.getNestedChildrenNodesList(document, "Scenario", "factMappingValues", "FactMappingValue");
            scenarioFactMappingValueNodeList.forEach(scenarioFactMappingValue -> {
                replaceReference(simulationFactMappingNodeList, scenarioFactMappingValue, FACT_IDENTIFIER_NODE);
                replaceReference(simulationFactMappingNodeList, scenarioFactMappingValue, EXPRESSION_IDENTIFIER_NODE);
            });
            updateVersion(document, "1.6");
        };
    }

    @Override
    public ThrowingConsumer<Document> from1_6to1_7() {
        return document -> {
            final List<Node> factMappingNodeList = GWTParserUtil.getNestedChildrenNodesList(document, SIMULATION_DESCRIPTOR_NODE, FACT_MAPPINGS_NODE, FACT_MAPPING_NODE);
            factMappingNodeList.forEach(factMappingNode -> {
                List<Node> expressionIdentifierNamesNodes = GWTParserUtil.getNestedChildrenNodesList(factMappingNode, EXPRESSION_IDENTIFIER_NODE, "name");
                String expressionIdentifierName = expressionIdentifierNamesNodes.get(0).getNodeValue();
                GWTParserUtil.createNodeAndAppend(factMappingNode, "columnWidth", Double.toString(getColumnWidth(expressionIdentifierName)));
            });
            updateVersion(document, "1.7");
        };
    }

    @Override
    public ThrowingConsumer<Document> from1_7to1_8() {
        return document -> {
            final Node settingsNode = GWTParserUtil.createNodeAndAppend(document.getElementsByTagName(SCENARIO_SIMULATION_MODEL_NODE).item(0), SETTINGS_NODE, null);
            for (String setting : SETTINGS) {
                final Map<Node, List<Node>> childrenNodesMap = GWTParserUtil.getChildrenNodesMap(document, SIMULATION_DESCRIPTOR_NODE, setting);
                childrenNodesMap.values().stream()
                        .filter(childNodeList -> !childNodeList.isEmpty())
                        .findFirst()
                        .ifPresent(childNodeList -> {
                            final Node node = childNodeList.get(0);
                            GWTParserUtil.createNodeAndAppend(settingsNode, node.getNodeName(), node.getNodeValue());
                            node.getParentNode().removeChild(node);
                        });
            }
            final List<Node> factMappingNodesList = GWTParserUtil.getNestedChildrenNodesList(document, SIMULATION_DESCRIPTOR_NODE, FACT_MAPPINGS_NODE, FACT_MAPPING_NODE);
            factMappingNodesList.forEach(factMappingNode -> GWTParserUtil.createNodeAndAppend(factMappingNode, FACT_MAPPING_VALUE_TYPE_NODE, NOT_EXPRESSION));
            final Node backgroundNode = GWTParserUtil.createNodeAndAppend(document.getElementsByTagName(SCENARIO_SIMULATION_MODEL_NODE).item(0), BACKGROUND_NODE, null);
            final Node simulationDescriptorNode = GWTParserUtil.createNodeAndAppend(backgroundNode, SIMULATION_DESCRIPTOR_NODE, null);
            final Node factMappingsNode = GWTParserUtil.createNodeAndAppend(simulationDescriptorNode, FACT_MAPPINGS_NODE, null);
            final Node factMappingNode = GWTParserUtil.createNodeAndAppend(factMappingsNode, FACT_MAPPING_NODE, null);
            GWTParserUtil.createNodeAndAppend(factMappingNode, FACT_MAPPING_VALUE_TYPE_NODE, NOT_EXPRESSION);
            final Node expressionElementsNode = GWTParserUtil.createNodeAndAppend(factMappingNode, EXPRESSION_ELEMENTS_NODE, null);
            ((Element) expressionElementsNode).setAttribute("class", "linked-list");
            final Node expressionIdentifierNode = GWTParserUtil.createNodeAndAppend(factMappingNode, EXPRESSION_IDENTIFIER_NODE, null);
            GWTParserUtil.createNodeAndAppend(expressionIdentifierNode, "name", "1|1");
            GWTParserUtil.createNodeAndAppend(expressionIdentifierNode, "type", "GIVEN");
            final Node factIdentifierNode = GWTParserUtil.createNodeAndAppend(factMappingNode, FACT_IDENTIFIER_NODE, null);
            GWTParserUtil.createNodeAndAppend(factIdentifierNode, "name", "Empty");
            GWTParserUtil.createNodeAndAppend(factIdentifierNode, "className", Void.class.getCanonicalName());
            GWTParserUtil.createNodeAndAppend(factMappingNode, "className", Void.class.getCanonicalName());
            GWTParserUtil.createNodeAndAppend(factMappingNode, "factAlias", "Instance 1");
            GWTParserUtil.createNodeAndAppend(factMappingNode, "expressionAlias", "PROPERTY 1");
            final Node scesimData = GWTParserUtil.createNodeAndAppend(backgroundNode, "scesimData", null);
            ((Element)scesimData).setAttribute("class", "linked-list");
            final Node backgroundData = GWTParserUtil.createNodeAndAppend(scesimData, BACKGROUND_DATA_NODE, null);
            final Node factMappingValues = GWTParserUtil.createNodeAndAppend(backgroundData, FACT_MAPPING_VALUES_NODE, null);
            final Node factMappingValue = GWTParserUtil.createNodeAndAppend(factMappingValues, FACT_MAPPING_VALUE_NODE, null);
            final Node factIdentifier = GWTParserUtil.createNodeAndAppend(factMappingValue, FACT_IDENTIFIER_NODE, null);
            GWTParserUtil.createNodeAndAppend(factIdentifier, "name", "Empty");
            GWTParserUtil.createNodeAndAppend(factIdentifier, "className", Void.class.getCanonicalName());
            final Node expressionIdentifier = GWTParserUtil.createNodeAndAppend(factMappingValue, EXPRESSION_IDENTIFIER_NODE, null);
            GWTParserUtil.createNodeAndAppend(expressionIdentifier, "name", "1|1");
            GWTParserUtil.createNodeAndAppend(expressionIdentifier, "type", "GIVEN");
            updateVersion(document, "1.8");
        };
    }

    private void replaceReference(List<Node> simulationFactMappingNodeList, Node containerNode, String referredNodeName) {
        final List<Node> referredNodesList = GWTParserUtil.getChildrenNodesList(containerNode, referredNodeName);
        if (!referredNodesList.isEmpty()) {
            Node referringNode = referredNodesList.get(0);
            String referenceAttribute = GWTParserUtil.getAttributeValue(referringNode, "reference");
            if (referenceAttribute != null) {
                String referredIndex = "1";
                if (referenceAttribute.contains("[") && referenceAttribute.contains("]")) {
                    referredIndex = referenceAttribute.substring(referenceAttribute.indexOf('[') + 1, referenceAttribute.indexOf(']'));
                }
                int index = Integer.parseInt(referredIndex) - 1;
                Node referredFactMapping = simulationFactMappingNodeList.get(index);
                Node referredNode = GWTParserUtil.getChildrenNodesList(referredFactMapping, referredNodeName).get(0);
                Node clonedNode = referredNode.cloneNode(true);
                containerNode.replaceChild(clonedNode, referringNode);
            }
        }
    }
}
