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

package org.drools.workbench.screens.scenariosimulation.kogito.client.migration;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.xml.client.Document;
import org.drools.scenariosimulation.api.model.ScenarioSimulationModel;
import org.drools.workbench.screens.scenariosimulation.kogito.client.util.GWTParserUtil;

import static org.drools.scenariosimulation.api.utils.ConstantsHolder.BACKGROUND_NODE;
import static org.drools.scenariosimulation.api.utils.ConstantsHolder.SCESIM_MODEL_DESCRIPTOR_NODE;
import static org.drools.scenariosimulation.api.utils.ConstantsHolder.SETTINGS;
import static org.drools.scenariosimulation.api.utils.ConstantsHolder.SIMULATION_DESCRIPTOR_NODE;
import static org.drools.scenariosimulation.api.utils.ConstantsHolder.SIMULATION_NODE;

public class ScenarioSimulationXMLPersistence {

    private static final ScenarioSimulationXMLPersistence INSTANCE = new ScenarioSimulationXMLPersistence();
    private static final String CURRENT_VERSION = new ScenarioSimulationModel().getVersion();
    private static final RegExp VERSION_REGEX_PATTERN = RegExp.compile("version=\"([0-9]+\\.[0-9]+)");

    private MigrationStrategy migrationStrategy = new InMemoryMigrationStrategy();

    private ScenarioSimulationXMLPersistence() {

    }

    public static ScenarioSimulationXMLPersistence getInstance() {
        return INSTANCE;
    }

    public static String getCurrentVersion() {
        return CURRENT_VERSION;
    }

    public static String cleanUpUnusedNodes(String rawXml) {
        String toReturn = GWTParserUtil.cleanupNodes(rawXml, "Scenario", SIMULATION_DESCRIPTOR_NODE);
        for (String setting : SETTINGS) {
            toReturn = GWTParserUtil.cleanupNodes(toReturn, SIMULATION_DESCRIPTOR_NODE, setting);
        }
        toReturn = GWTParserUtil.replaceNodeName(GWTParserUtil.getDocument(toReturn), SIMULATION_NODE, "scenarios", "scesimData");
        toReturn = GWTParserUtil.replaceNodeName(GWTParserUtil.getDocument(toReturn), SIMULATION_NODE, SIMULATION_DESCRIPTOR_NODE, SCESIM_MODEL_DESCRIPTOR_NODE);
        toReturn = GWTParserUtil.replaceNodeName(GWTParserUtil.getDocument(toReturn), BACKGROUND_NODE, SIMULATION_DESCRIPTOR_NODE, SCESIM_MODEL_DESCRIPTOR_NODE);
        return toReturn;
    }

    public String migrate(final String rawXml) {
        if (rawXml == null || rawXml.trim().equals("")) {
            throw new IllegalArgumentException("Malformed file, content is empty!");
        }

        String migratedXml = migrateIfNecessary(rawXml);
        String cleanedXML = cleanUpUnusedNodes(migratedXml);

        return cleanedXML;
    }

    public String migrateIfNecessary(String rawXml) {
        String fileVersion = extractVersion(rawXml);
        ThrowingConsumer<Document> migrator = migrationStrategy.start();
        boolean supported;
        switch (fileVersion) {
            case "1.0": //FallThrough is intended
                migrator = migrator.andThen(migrationStrategy.from1_0to1_1());
            case "1.1": //FallThrough is intended
                migrator = migrator.andThen(migrationStrategy.from1_1to1_2());
            case "1.2": //FallThrough is intended
                migrator = migrator.andThen(migrationStrategy.from1_2to1_3());
            case "1.3": //FallThrough is intended
                migrator = migrator.andThen(migrationStrategy.from1_3to1_4());
            case "1.4": //FallThrough is intended
                migrator = migrator.andThen(migrationStrategy.from1_4to1_5());
            case "1.5": //FallThrough is intended
                migrator = migrator.andThen(migrationStrategy.from1_5to1_6());
            case "1.6": //FallThrough is intended
                migrator = migrator.andThen(migrationStrategy.from1_6to1_7());
            case "1.7":
                migrator = migrator.andThen(migrationStrategy.from1_7to1_8());
                supported = true;
                break;
            default:
                supported = CURRENT_VERSION.equals(fileVersion);
                break;
        }
        if (!supported) {
            throw new IllegalArgumentException(new StringBuilder().append("Version ").append(fileVersion)
                                                       .append(" of the file is not supported. Current version is ")
                                                       .append(CURRENT_VERSION).toString());
        }
        migrator = migrator.andThen(migrationStrategy.end());
        Document document = GWTParserUtil.getDocument(rawXml);
        migrator.accept(document);
        return GWTParserUtil.getString(document);
    }

    public String extractVersion(String rawXml) {
        MatchResult matchResult = VERSION_REGEX_PATTERN.exec(rawXml);

        if (matchResult != null) {
            return matchResult.getGroup(1);
        }
        throw new IllegalArgumentException("Impossible to extract version from the file");
    }

}
