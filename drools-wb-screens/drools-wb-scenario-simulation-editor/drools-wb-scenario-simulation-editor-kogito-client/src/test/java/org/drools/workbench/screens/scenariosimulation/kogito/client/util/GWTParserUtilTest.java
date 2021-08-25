/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
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
package org.drools.workbench.screens.scenariosimulation.kogito.client.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.XMLParser;

/**
 * To test GWTParserUtil, it is required to use the {@link GWTTestCase} framework.
 * This because, the native GWT XML API are used in the Parser, and this is the only
 * way to test it. Please notice, GWTTestCase relies on Junit3, therefore no @Test annotation
 * are required. Methods must have 'test' prefix.
 */
public class GWTParserUtilTest extends GWTTestCase {

    private static final String MAIN_NODE = "Main";
    private static final String MAIN_ATTRIBUTE_NAME = "mainattribute";
    private static final String ATTRIBUTE_VALUE = "default";
    private static final String TEST_NODE = "testnode";
    private static final String TEST_NODE_CONTENT = "testnodecontent";
    private static final String TEST_NODE_TOREMOVE_1 = "toremove1";
    private static final String TEST_NODE_TOREMOVE_2 = "toremove2";
    private static final String CHILD_NODE = "child";
    private static final String CHILD_ATTRIBUTE_NAME = "childattribute";
    private static final String OTHER_NODE = "othernode";
    private static final String OTHER_NODE_CONTENT_1 = "othernodecontent1";
    private static final String OTHER_NODE_CONTENT_2 = "othernodecontent2";
    private static final String NOT_EXISTING = "NOT_EXISTING";
    private static final String NESTING_NODE = "nesting";
    private static final String NESTED_NODE = "nested";

    private static final String XML =
            "<" + MAIN_NODE + " " + MAIN_ATTRIBUTE_NAME + " =\"" + ATTRIBUTE_VALUE + "\">" +
                "<" + TEST_NODE + ">" + TEST_NODE_CONTENT + "</" + TEST_NODE + ">" +
                "<" + CHILD_NODE + " " + CHILD_ATTRIBUTE_NAME + " =\"" + ATTRIBUTE_VALUE + "\">" +
                    "<" + TEST_NODE + ">" + TEST_NODE_TOREMOVE_1 + "</" + TEST_NODE + ">" +
                    "<" + OTHER_NODE + ">" + OTHER_NODE_CONTENT_1 + "</" + OTHER_NODE + ">" +
                    "<" + NESTING_NODE + ">" +
                        "<" + NESTED_NODE + "/>" +
                    "</" + NESTING_NODE + ">" +
                "</" + CHILD_NODE + ">" +
                "<" + CHILD_NODE + " " + CHILD_ATTRIBUTE_NAME + " =\"" + ATTRIBUTE_VALUE + "\">" +
                    "<" + TEST_NODE + ">" + TEST_NODE_TOREMOVE_2 + "</" + TEST_NODE + ">" +
                    "<" + OTHER_NODE + ">" + OTHER_NODE_CONTENT_2 + "</" + OTHER_NODE + ">" +
                    "<" + NESTING_NODE + ">" +
                        "<" + NESTED_NODE + "/>" +
                    "</" + NESTING_NODE + ">" +
                "</" + CHILD_NODE + ">" +
            "</" + MAIN_NODE + ">";

    public void testCleanupNodesString() {
        String retrieved = GWTParserUtil.cleanupNodes(XML, CHILD_NODE, TEST_NODE);
        assertNotNull(retrieved);
        Map<Node, List<Node>> childrenNodes = GWTParserUtil.getChildrenNodesMap(retrieved, MAIN_NODE, TEST_NODE);
        assertNotNull(childrenNodes);
        assertEquals(1, childrenNodes.size());
        Node keyNode = childrenNodes.keySet().iterator().next();
        assertEquals(MAIN_NODE, keyNode.getNodeName());
        List<Node> valueNodes = childrenNodes.get(keyNode);
        assertTrue(valueNodes != null && valueNodes.size() == 1);
        assertEquals(TEST_NODE, valueNodes.get(0).getNodeName());

        childrenNodes = GWTParserUtil.getChildrenNodesMap(retrieved, CHILD_NODE, OTHER_NODE);
        assertEquals(2, childrenNodes.size());
        childrenNodes.forEach((childKeyNode, childValueNodes) -> {
            assertNotNull(childKeyNode);
            assertEquals(CHILD_NODE, childKeyNode.getNodeName());
            assertTrue(childValueNodes != null && childValueNodes.size() == 1);
            assertEquals(OTHER_NODE, childValueNodes.get(0).getNodeName());
        });

        childrenNodes = GWTParserUtil.getChildrenNodesMap(retrieved, CHILD_NODE, TEST_NODE);
        childrenNodes.forEach((childKeyNode, childValueNodes) -> {
            assertNotNull(childKeyNode);
            assertEquals(CHILD_NODE, childKeyNode.getNodeName());
            assertTrue(childValueNodes != null && childValueNodes.isEmpty());
        });
    }

    public void testCleanupNodesDocument() {
        Document document = GWTParserUtil.getDocument(XML);
        GWTParserUtil.cleanupNodes(document, CHILD_NODE, TEST_NODE);
        assertNotNull(document);
        Map<Node, List<Node>> childrenNodes = GWTParserUtil.getChildrenNodesMap(document, MAIN_NODE, TEST_NODE);
        assertNotNull(childrenNodes);
        assertEquals(1, childrenNodes.size());
        Node keyNode = childrenNodes.keySet().iterator().next();
        assertEquals(MAIN_NODE, keyNode.getNodeName());
        List<Node> valueNodes = childrenNodes.get(keyNode);
        assertTrue(valueNodes != null && valueNodes.size() == 1);
        assertEquals(TEST_NODE, valueNodes.get(0).getNodeName());

        childrenNodes = GWTParserUtil.getChildrenNodesMap(document, CHILD_NODE, OTHER_NODE);
        assertEquals(2, childrenNodes.size());
        childrenNodes.forEach((childKeyNode, childValueNodes) -> {
            assertNotNull(childKeyNode);
            assertEquals(CHILD_NODE, childKeyNode.getNodeName());
            assertTrue(childValueNodes != null && childValueNodes.size() == 1);
            assertEquals(OTHER_NODE, childValueNodes.get(0).getNodeName());
        });

        childrenNodes = GWTParserUtil.getChildrenNodesMap(document, CHILD_NODE, TEST_NODE);
        childrenNodes.forEach((childKeyNode, childValueNodes) -> {
            assertNotNull(childKeyNode);
            assertEquals(CHILD_NODE, childKeyNode.getNodeName());
            assertTrue(childValueNodes != null && childValueNodes.isEmpty());
        });
    }

    public void testReplaceNodeText() {
        final String replacement = "replacement";
        Document document = GWTParserUtil.getDocument(XML);
        GWTParserUtil.replaceNodeText(document, MAIN_NODE, TEST_NODE, TEST_NODE_CONTENT, replacement);
        final Map<Node, List<Node>> retrieved = GWTParserUtil.getChildrenNodesMap(document, MAIN_NODE, TEST_NODE);
        assertNotNull(retrieved);
        assertEquals(1, retrieved.size());
        List<Node> testNodes = retrieved.values().iterator().next();
        assertNotNull(testNodes);
        assertEquals(1, testNodes.size());
        assertEquals(1, testNodes.get(0).getChildNodes().getLength());
        assertEquals(replacement, testNodes.get(0).getFirstChild().getNodeValue());
    }

    public void testReplaceNodeName() {
        final String replacement = "replacement";
        Document document = GWTParserUtil.getDocument(XML);
        GWTParserUtil.replaceNodeName(document, MAIN_NODE, TEST_NODE, replacement);
        final Map<Node, List<Node>> retrieved = GWTParserUtil.getChildrenNodesMap(document, MAIN_NODE, replacement);
        assertNotNull(retrieved);
        assertEquals(1, retrieved.size());
        List<Node> testNodes = retrieved.values().iterator().next();
        assertNotNull(testNodes);
        assertEquals(1, testNodes.size());
        assertEquals("replacement", testNodes.get(0).getNodeName());
        assertEquals(1, testNodes.get(0).getChildNodes().getLength());
        assertEquals(TEST_NODE_CONTENT, testNodes.get(0).getFirstChild().getNodeValue());
    }

    public void testReplaceNestingNodeName() {
        final String replacement = "replacement";
        Document document = GWTParserUtil.getDocument(XML);
        GWTParserUtil.replaceNodeName(document, CHILD_NODE, NESTING_NODE, replacement);
        final Map<Node, List<Node>> retrieved = GWTParserUtil.getChildrenNodesMap(document, CHILD_NODE, replacement);
        assertNotNull(retrieved);
        assertEquals(2, retrieved.size());
        Iterator<List<Node>> iterator = retrieved.values().iterator();
        while (iterator.hasNext()) {
            List<Node> testNodes = iterator.next();
            assertNotNull(testNodes);
            assertEquals(1, testNodes.size());
            assertEquals("replacement", testNodes.get(0).getNodeName());
            assertEquals(1, testNodes.get(0).getChildNodes().getLength());
            assertEquals(NESTED_NODE, testNodes.get(0).getFirstChild().getNodeName());
        }
    }

    public void testGetAttributeValuesByNode() {
        Document document = GWTParserUtil.getDocument(XML);
        Map<Node, String> retrieved = GWTParserUtil.getAttributeValues(document, MAIN_NODE, MAIN_ATTRIBUTE_NAME);
        assertNotNull(retrieved);
        assertEquals(1, retrieved.size());
        assertEquals(ATTRIBUTE_VALUE, retrieved.values().toArray()[0]);
        retrieved = GWTParserUtil.getAttributeValues(document, MAIN_NODE, NOT_EXISTING);
        assertNotNull(retrieved);
        assertTrue(retrieved.isEmpty());
        retrieved = GWTParserUtil.getAttributeValues(document, CHILD_NODE, CHILD_ATTRIBUTE_NAME);
        assertNotNull(retrieved);
        assertEquals(2, retrieved.size());
        retrieved.values().forEach(attributeValue -> assertEquals(ATTRIBUTE_VALUE, attributeValue));
        retrieved = GWTParserUtil.getAttributeValues(document, CHILD_NODE, NOT_EXISTING);
        assertNotNull(retrieved);
        assertTrue(retrieved.isEmpty());
    }

    //TODO
    public void GetAllAttributeValues() {
        Document document = GWTParserUtil.getDocument(XML);
        Map<Node, String> retrieved = GWTParserUtil.getAttributeValues(document, MAIN_ATTRIBUTE_NAME);
        assertNotNull(retrieved);
        assertEquals(1, retrieved.size());
        assertEquals(ATTRIBUTE_VALUE, retrieved.values().toArray()[0]);
        retrieved = GWTParserUtil.getAttributeValues(document, CHILD_ATTRIBUTE_NAME);
        assertNotNull(retrieved);
        assertEquals(2, retrieved.size());
        retrieved.values().forEach(attributeValue -> assertEquals(ATTRIBUTE_VALUE, attributeValue));
    }

    public void testSetAttributeValue() {
        final String newValue = "NEW_VALUE";
        Document document = GWTParserUtil.getDocument(XML);
        GWTParserUtil.setAttributeValue(document, MAIN_NODE, MAIN_ATTRIBUTE_NAME, newValue);
        Map<Node, String> retrieved = GWTParserUtil.getAttributeValues(document, MAIN_NODE, MAIN_ATTRIBUTE_NAME);
        assertEquals(retrieved.values().toArray()[0], newValue);
        GWTParserUtil.setAttributeValue(document, MAIN_NODE, NOT_EXISTING, newValue);
        retrieved = GWTParserUtil.getAttributeValues(document, MAIN_NODE, NOT_EXISTING);
        assertTrue(retrieved.isEmpty());
        GWTParserUtil.setAttributeValue(document, CHILD_NODE, CHILD_ATTRIBUTE_NAME, newValue);
        retrieved = GWTParserUtil.getAttributeValues(document, CHILD_NODE, CHILD_ATTRIBUTE_NAME);
        assertEquals(2, retrieved.size());
        retrieved.values().forEach(attributeValue -> assertEquals(newValue, attributeValue));
    }

    public void testCreateNodes() {
        final String newNodeName = "NEW_NODE_NAME";
        final String newNodeValue = "NEW_NODE_VALUE";
        Document document = GWTParserUtil.getDocument(XML);
        Map<Node, Node> retrieved = GWTParserUtil.createNodes(document, MAIN_NODE, newNodeName, newNodeValue);
        assertEquals(1, retrieved.size());
        Node created = (Node) retrieved.values().toArray()[0];
        assertNotNull(created);
        assertEquals(newNodeName, created.getNodeName());
        assertEquals(1, created.getChildNodes().getLength());
        assertEquals(Node.TEXT_NODE, created.getFirstChild().getNodeType());
        assertEquals(newNodeValue, created.getFirstChild().getNodeValue());
        retrieved = GWTParserUtil.createNodes(document, MAIN_NODE, newNodeName, null);
        assertEquals(1, retrieved.size());
        created = (Node) retrieved.values().toArray()[0];
        assertNotNull(created);
        assertEquals(newNodeName, created.getNodeName());
        assertFalse(created.hasChildNodes());

        retrieved = GWTParserUtil.createNodes(document, CHILD_NODE, newNodeName, newNodeValue);
        assertEquals(2, retrieved.size());
        retrieved.forEach((key, createdNode) -> {
            assertNotNull(createdNode);
            assertEquals(newNodeName, createdNode.getNodeName());
            assertEquals(1, createdNode.getChildNodes().getLength());
            assertEquals(Node.TEXT_NODE, createdNode.getFirstChild().getNodeType());
            assertEquals(newNodeValue, createdNode.getFirstChild().getNodeValue());
        });
        retrieved = GWTParserUtil.createNodes(document, CHILD_NODE, newNodeName, null);
        assertEquals(2, retrieved.size());
        retrieved.forEach((key, createdNode) -> {
            assertNotNull(createdNode);
            assertEquals(newNodeName, createdNode.getNodeName());
            assertFalse(createdNode.hasChildNodes());
        });
    }

    public void testCreateNestedNodes() {
        final String newNodeName = "NEW_NODE_NAME";
        final String newNodeValue = "NEW_NODE_VALUE";
        Document document = GWTParserUtil.getDocument(XML);
        Map<Node, Node> retrieved = GWTParserUtil.createNestedNodes(document, MAIN_NODE, TEST_NODE, newNodeName, newNodeValue);
        assertEquals(1, retrieved.size());
        Node created = (Node) retrieved.values().toArray()[0];
        assertNotNull(created);
        assertEquals(newNodeName, created.getNodeName());
        assertEquals(1, created.getChildNodes().getLength());
        assertEquals(Node.TEXT_NODE, created.getFirstChild().getNodeType());
        assertEquals(newNodeValue, created.getFirstChild().getNodeValue());
        retrieved = GWTParserUtil.createNestedNodes(document, MAIN_NODE, TEST_NODE, newNodeName, null);
        assertEquals(1, retrieved.size());
        created = (Node) retrieved.values().toArray()[0];
        assertNotNull(created);
        assertEquals(newNodeName, created.getNodeName());
        assertFalse(created.hasChildNodes());

        retrieved = GWTParserUtil.createNestedNodes(document, MAIN_NODE, CHILD_NODE, newNodeName, newNodeValue);
        assertEquals(2, retrieved.size());
        retrieved.forEach((key, createdNode) -> {
            assertNotNull(createdNode);
            assertEquals(newNodeName, createdNode.getNodeName());
            assertEquals(1, createdNode.getChildNodes().getLength());
            assertEquals(Node.TEXT_NODE, createdNode.getFirstChild().getNodeType());
            assertEquals(newNodeValue, createdNode.getFirstChild().getNodeValue());
        });
        retrieved = GWTParserUtil.createNestedNodes(document, MAIN_NODE, CHILD_NODE, newNodeName, null);
        assertEquals(2, retrieved.size());
        retrieved.forEach((key, createdNode) -> {
            assertNotNull(createdNode);
            assertEquals(newNodeName, createdNode.getNodeName());
            assertFalse(createdNode.hasChildNodes());
        });
    }

    public void testCreateNodeAtPosition() {
        String newNodeName = "NEW_NODE_NAME_0";
        String newNodeValue = "NEW_NODE_VALUE_=";
        Document document = GWTParserUtil.getDocument(XML);
        Map<Node, List<Node>> testNodesMap = GWTParserUtil.getChildrenNodesMap(document, MAIN_NODE, TEST_NODE);
        assertEquals(1, testNodesMap.size());
        Node mainNode = testNodesMap.keySet().iterator().next();
        Node retrieved = GWTParserUtil.createNodeAtPosition(mainNode, newNodeName, newNodeValue, null);
        assertNotNull(retrieved);
        assertEquals(newNodeName, retrieved.getNodeName());
        assertEquals(1, retrieved.getChildNodes().getLength());
        assertEquals(Node.TEXT_NODE, retrieved.getFirstChild().getNodeType());
        assertEquals(newNodeValue, retrieved.getFirstChild().getNodeValue());
        assertEquals(retrieved, mainNode.getChildNodes().item(mainNode.getChildNodes().getLength() - 1));
        newNodeName = "NEW_NODE_NAME_1";
        newNodeValue = "NEW_NODE_VALUE_1";
        retrieved = GWTParserUtil.createNodeAtPosition(mainNode, newNodeName, newNodeValue, 0);
        assertNotNull(retrieved);
        assertEquals(newNodeName, retrieved.getNodeName());
        assertEquals(1, retrieved.getChildNodes().getLength());
        assertEquals(Node.TEXT_NODE, retrieved.getFirstChild().getNodeType());
        assertEquals(newNodeValue, retrieved.getFirstChild().getNodeValue());
        assertEquals(retrieved, mainNode.getChildNodes().item(0));
        newNodeName = "NEW_NODE_NAME_2";
        newNodeValue = "NEW_NODE_VALUE_2";
        retrieved = GWTParserUtil.createNodeAtPosition(mainNode, newNodeName, newNodeValue, 2);
        assertNotNull(retrieved);
        assertEquals(newNodeName, retrieved.getNodeName());
        assertEquals(1, retrieved.getChildNodes().getLength());
        assertEquals(Node.TEXT_NODE, retrieved.getFirstChild().getNodeType());
        assertEquals(newNodeValue, retrieved.getFirstChild().getNodeValue());
        assertEquals(retrieved, mainNode.getChildNodes().item(2));
    }

    public void testCreateNodeAndAppend() {
        String newNodeName0 = "NEW_NODE_NAME_0";
        String newNodeValue0 = "NEW_NODE_VALUE_=";
        Document document = GWTParserUtil.getDocument(XML);
        Map<Node, List<Node>> testNodesMap = GWTParserUtil.getChildrenNodesMap(document, MAIN_NODE, TEST_NODE);
        assertEquals(1, testNodesMap.size());
        Node mainNode = testNodesMap.keySet().iterator().next();
        int startingChildNodes = mainNode.getChildNodes().getLength();
        Node retrieved = GWTParserUtil.createNodeAndAppend(mainNode, newNodeName0, newNodeValue0);
        assertNotNull(retrieved);
        assertEquals(newNodeName0, retrieved.getNodeName());
        assertEquals(1, retrieved.getChildNodes().getLength());
        assertEquals(Node.TEXT_NODE, retrieved.getFirstChild().getNodeType());
        assertEquals(newNodeValue0, retrieved.getFirstChild().getNodeValue());
        assertEquals(retrieved, mainNode.getChildNodes().item(mainNode.getChildNodes().getLength() - 1));
        assertEquals(startingChildNodes + 1, mainNode.getChildNodes().getLength());
        String newNodeName1 = "NEW_NODE_NAME_1";
        String newNodeValue1 = "NEW_NODE_VALUE_1";
        retrieved = GWTParserUtil.createNodeAndAppend(mainNode, newNodeName1, newNodeValue1);
        assertNotNull(retrieved);
        assertEquals(newNodeName1, retrieved.getNodeName());
        assertEquals(1, retrieved.getChildNodes().getLength());
        assertEquals(Node.TEXT_NODE, retrieved.getFirstChild().getNodeType());
        assertEquals(newNodeValue1, retrieved.getFirstChild().getNodeValue());
        assertEquals(retrieved, mainNode.getChildNodes().item(mainNode.getChildNodes().getLength() - 1));
        assertEquals(startingChildNodes + 2, mainNode.getChildNodes().getLength());
        String newNodeName2 = "NEW_NODE_NAME_2";
        String newNodeValue2 = "NEW_NODE_VALUE_2";
        retrieved = GWTParserUtil.createNodeAndAppend(mainNode, newNodeName2, newNodeValue2);
        assertNotNull(retrieved);
        assertEquals(newNodeName2, retrieved.getNodeName());
        assertEquals(1, retrieved.getChildNodes().getLength());
        assertEquals(Node.TEXT_NODE, retrieved.getFirstChild().getNodeType());
        assertEquals(newNodeValue2, retrieved.getFirstChild().getNodeValue());
        assertEquals(retrieved, mainNode.getChildNodes().item(mainNode.getChildNodes().getLength() - 1));
        assertEquals(startingChildNodes + 3, mainNode.getChildNodes().getLength());

        assertEquals(newNodeName0, mainNode.getChildNodes().item(startingChildNodes).getNodeName());
        assertEquals(1, mainNode.getChildNodes().item(startingChildNodes).getChildNodes().getLength());
        assertEquals(Node.TEXT_NODE, mainNode.getChildNodes().item(startingChildNodes).getFirstChild().getNodeType());
        assertEquals(newNodeValue0, mainNode.getChildNodes().item(startingChildNodes).getFirstChild().getNodeValue());
        assertEquals(newNodeName1, mainNode.getChildNodes().item(startingChildNodes + 1).getNodeName());
        assertEquals(1, mainNode.getChildNodes().item(startingChildNodes + 1).getChildNodes().getLength());
        assertEquals(Node.TEXT_NODE, mainNode.getChildNodes().item(startingChildNodes + 1).getFirstChild().getNodeType());
        assertEquals(newNodeValue1, mainNode.getChildNodes().item(startingChildNodes + 1).getFirstChild().getNodeValue());
        assertEquals(newNodeName2, mainNode.getChildNodes().item(startingChildNodes + 2).getNodeName());
        assertEquals(1, mainNode.getChildNodes().item(startingChildNodes + 2).getChildNodes().getLength());
        assertEquals(Node.TEXT_NODE, mainNode.getChildNodes().item(startingChildNodes + 2).getFirstChild().getNodeType());
        assertEquals(newNodeValue2, mainNode.getChildNodes().item(startingChildNodes + 2).getFirstChild().getNodeValue());
    }

    public void testGetChildrenNodesFromDocument() {
        Document document = GWTParserUtil.getDocument(XML);
        Map<Node, List<Node>> retrieved = GWTParserUtil.getChildrenNodesMap(document, MAIN_NODE, TEST_NODE);
        assertNotNull(retrieved);
        assertEquals(1, retrieved.size());
        Node keyNode = retrieved.keySet().iterator().next();
        assertNotNull(keyNode);
        assertEquals(MAIN_NODE, keyNode.getNodeName());
        List<Node> valueNodes = retrieved.get(keyNode);
        assertTrue(valueNodes != null && valueNodes.size() == 1);
        assertEquals(TEST_NODE, valueNodes.get(0).getNodeName());
        retrieved = GWTParserUtil.getChildrenNodesMap(document, MAIN_NODE, NOT_EXISTING);
        assertNotNull(retrieved);
        assertEquals(1, retrieved.size());
        assertTrue(retrieved.values().iterator().next().isEmpty());
        retrieved = GWTParserUtil.getChildrenNodesMap(document, MAIN_NODE, CHILD_NODE);
        assertNotNull(retrieved);
        assertEquals(1, retrieved.size());
        keyNode = retrieved.keySet().iterator().next();
        assertNotNull(keyNode);
        assertEquals(MAIN_NODE, keyNode.getNodeName());
        valueNodes = retrieved.get(keyNode);
        assertTrue(valueNodes != null && valueNodes.size() == 2);
        valueNodes.forEach(childNode -> assertEquals(CHILD_NODE, childNode.getNodeName()));
        List<String> nodeToTest = Arrays.asList(TEST_NODE, OTHER_NODE);
        for (String childNodeName : nodeToTest) {
            retrieved = GWTParserUtil.getChildrenNodesMap(XML, CHILD_NODE, childNodeName);
            assertNotNull(retrieved);
            assertEquals(2, retrieved.size());
            retrieved.forEach((childKeyNode, childValueNodes) -> {
                assertNotNull(childKeyNode);
                assertEquals(CHILD_NODE, childKeyNode.getNodeName());
                assertTrue(childValueNodes != null && childValueNodes.size() == 1);
                assertEquals(childNodeName, childValueNodes.get(0).getNodeName());
            });
        }
    }

    public void testGetChildrenNodesFromNode() {
        Document document = GWTParserUtil.getDocument(XML);
        Map<Node, List<Node>> retrieved = GWTParserUtil.getChildrenNodesMap(document, MAIN_NODE, CHILD_NODE);
        assertNotNull(retrieved);
        assertEquals(1, retrieved.size());
        Node mainNode = (Node) retrieved.keySet().toArray()[0];
        assertEquals(MAIN_NODE, mainNode.getNodeName());
        List<Node> nodes = retrieved.get(mainNode);
        nodes.forEach(childNode -> assertEquals(CHILD_NODE, childNode.getNodeName()));
        retrieved = GWTParserUtil.getChildrenNodesMap(nodes.get(0), NESTING_NODE, NESTED_NODE);
        assertNotNull(retrieved);
        assertEquals(1, retrieved.size());
        Node childNode = (Node) retrieved.keySet().toArray()[0];
        assertEquals(NESTING_NODE, childNode.getNodeName());
        nodes = retrieved.get(childNode);
        assertNotNull(nodes);
        assertEquals(1, nodes.size());
        assertEquals(NESTED_NODE, nodes.get(0).getNodeName());
    }

    public void testGetNestedChildrenNodesMap() {
        Document document = GWTParserUtil.getDocument(XML);
        Map<Node, List<Node>> retrieved = GWTParserUtil.getNestedChildrenNodesMap(document, MAIN_NODE, CHILD_NODE, TEST_NODE);
        assertNotNull(retrieved);
        assertEquals(2, retrieved.size());
        retrieved.forEach((childNode, testNodes) -> {
            assertEquals(CHILD_NODE, childNode.getNodeName());
            assertEquals(1, testNodes.size());
            assertEquals(TEST_NODE, testNodes.get(0).getNodeName());
        });
        retrieved = GWTParserUtil.getNestedChildrenNodesMap(document, CHILD_NODE, NESTING_NODE, NESTED_NODE);
        assertNotNull(retrieved);
        assertEquals(2, retrieved.size());
        retrieved.forEach((nestingNode, nestedNodes) -> {
            assertEquals(NESTING_NODE, nestingNode.getNodeName());
            assertEquals(1, nestedNodes.size());
            assertEquals(NESTED_NODE, nestedNodes.get(0).getNodeName());
        });
    }

    public void testGetNestedChildrenNodesList() {
        Document document = GWTParserUtil.getDocument(XML);
        List<Node> retrieved = GWTParserUtil.getNestedChildrenNodesList(document, MAIN_NODE, CHILD_NODE, TEST_NODE);
        assertNotNull(retrieved);
        assertEquals(2, retrieved.size());
        retrieved.forEach(testNode -> assertEquals(TEST_NODE, testNode.getNodeName()));
        retrieved = GWTParserUtil.getNestedChildrenNodesList(document, CHILD_NODE, NESTING_NODE, NESTED_NODE);
        assertNotNull(retrieved);
        assertEquals(2, retrieved.size());
        retrieved.forEach(nestedNode -> assertEquals(NESTED_NODE, nestedNode.getNodeName()));
    }

    public void testGetDocument() {
        Document retrieved = GWTParserUtil.getDocument(XML);
        assertNotNull(retrieved);
    }

    public void testGetString() {
        Document document = XMLParser.createDocument();
        document.appendChild(document.createElement("CREATED"));
        String retrieved = GWTParserUtil.getString(document);
        assertNotNull(retrieved);
        assertTrue(retrieved.contains("CREATED"));
    }

    public void testAsStream() {
        Document document = GWTParserUtil.getDocument(XML);
        final NodeList mainNodeList = document.getElementsByTagName("Main");
        commonCheckNodeStream(mainNodeList);
        final NodeList childNodesList = mainNodeList.item(0).getChildNodes();
        commonCheckNodeStream(childNodesList);
        final NodeList innerNodesList = childNodesList.item(0).getChildNodes();
        commonCheckNodeStream(innerNodesList);
    }

    private void commonCheckNodeStream(NodeList src) {
        assertEquals(src.getLength(), GWTParserUtil.asStream(src).count());
        AtomicInteger counter = new AtomicInteger();
        final Stream<Node> nodeStream = GWTParserUtil.asStream(src);
        nodeStream.forEach(node -> assertEquals(src.item(counter.getAndIncrement()), node));
    }

    @Override
    public String getModuleName() {
        return GWTParserUtilTest.class.getName();
    }
}