/*
 * Copyright 2017 Netflix, Inc.
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
package com.netflix.conductor.tests.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.common.run.ExternalStorageLocation;
import com.netflix.conductor.common.utils.ExternalPayloadStorage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockExternalPayloadStorage implements ExternalPayloadStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockExternalPayloadStorage.class);
    public static final String INPUT_PAYLOAD_PATH = "payload/input";

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ExternalStorageLocation getLocation(Operation operation, PayloadType payloadType, String path) {
        ExternalStorageLocation location = new ExternalStorageLocation();
        location.setUri("http://some/uri");
        switch (payloadType) {
            case TASK_INPUT:
            case WORKFLOW_INPUT:
                location.setPath(INPUT_PAYLOAD_PATH);
                break;
            case WORKFLOW_OUTPUT:
                location.setPath("workflow/output");
                break;
        }
        return location;
    }

    @Override
    public void upload(String path, InputStream payload, long payloadSize) {
        try {
            URL filePathURL = MockExternalPayloadStorage.class.getResource("/input.json");
            File file = new File(filePathURL.toURI());
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                int read;
                byte[] bytes = new byte[1024];
                while ((read = payload.read(bytes)) != -1) {
                    outputStream.write(bytes, 0, read);
                }
                outputStream.flush();
            }
        } catch (Exception e) {
            // just handle this exception here and return empty map so that test will fail in case this exception is thrown
        } finally {
            try {
                if (payload != null) {
                    payload.close();
                }
            } catch (IOException e) {
                LOGGER.warn("Unable to close inputstream when writing to file");
            }
        }
    }

    @Override
    public InputStream download(String path) {
        try {
            Map<String, Object> payload = getPayload(path);
            String jsonString = objectMapper.writeValueAsString(payload);
            return new ByteArrayInputStream(jsonString.getBytes());
        } catch (IOException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getPayload(String path) {
        Map<String, Object> stringObjectMap = new HashMap<>();
        try {
            switch (path) {
                case "workflow/input":
                    stringObjectMap.put("param1", "p1 value");
                    stringObjectMap.put("param2", "p2 value");
                    return stringObjectMap;
                case "task/output":
                    InputStream opStream = MockExternalPayloadStorage.class.getResourceAsStream("/output.json");
                    return objectMapper.readValue(opStream, Map.class);
                case INPUT_PAYLOAD_PATH:
                    InputStream ipStream = MockExternalPayloadStorage.class.getResourceAsStream("/input.json");
                    return objectMapper.readValue(ipStream, Map.class);
            }
        } catch (IOException e) {
            // just handle this exception here and return empty map so that test will fail in case this exception is thrown
        }
        return stringObjectMap;
    }
}
