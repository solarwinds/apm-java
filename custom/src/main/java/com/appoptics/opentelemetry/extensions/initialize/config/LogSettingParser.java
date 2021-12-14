package com.appoptics.opentelemetry.extensions.initialize.config;

import com.tracelytics.ext.json.JSONException;
import com.tracelytics.ext.json.JSONObject;
import com.tracelytics.joboe.config.ConfigParser;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.config.InvalidConfigException;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;
import com.tracelytics.logging.setting.LogSetting;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LogSettingParser implements ConfigParser<String, LogSetting> {
    private static final Logger LOGGER = LoggerFactory.getLogger();
    public static final String LEVEL_KEY = "level";
    public static final String STDOUT_KEY = "stdout";
    public static final String STDERR_KEY = "stderr";
    public static final String FILE_KEY = "file";
    public static final String FILE_LOCATION_KEY = "location";
    public static final String FILE_MAX_SIZE_KEY = "maxSize";
    public static final String FILE_MAX_BACKUP_KEY = "maxBackup";

    private static final List<String> VALID_CONFIG_KEYS = Arrays.asList(LEVEL_KEY, STDERR_KEY, STDOUT_KEY, FILE_KEY);
    private static final List<String> VALID_FILE_CONFIG_KEYS = Arrays.asList(FILE_LOCATION_KEY, FILE_MAX_SIZE_KEY, FILE_MAX_BACKUP_KEY);

    public static final LogSettingParser INSTANCE = new LogSettingParser();
    static final boolean DEFAULT_STDOUT_ENABLED = true;
    static final boolean DEFAULT_STDERR_ENABLED = true;

    public static final int MAX_BACKUP_MIN = 1;
    public static final int MAX_BACKUP_MAX = 100;
    public static final int MAX_SIZE_MIN = 1;
    public static final int MAX_SIZE_MAX = 1024;


    private LogSettingParser() {
    }


    public LogSetting convert(String javaValue) throws InvalidConfigException {
        String stringValue = javaValue.trim();
        if (javaValue.startsWith("{")) {
            return convertJsonValue(stringValue);
        } else {
            return convertStringValue(stringValue);
        }
    }

    private LogSetting convertStringValue(String stringValue) {
        Logger.Level level = Logger.Level.fromLabel(stringValue);
        //keep the existing behavior, not throwing exceptions on null level (invalid string)
        if (level == null) { //but do warn about it
            LOGGER.warn("Agent logging level value [" + stringValue + "] is invalid. Using default logging level instead...");
        }

        return new LogSetting(level, true, true, null, null, null);

    }

    private LogSetting convertJsonValue(String stringValue) throws InvalidConfigException {
        try {
            JSONObject jsonObject = new JSONObject(stringValue);

            checkUnknownKeys(jsonObject, VALID_CONFIG_KEYS);

            Logger.Level level = Logger.Level.fromLabel(jsonObject.getString(LEVEL_KEY));
            //since this is the new format, for invalid level value, we can throw exception
            if (level == null) {
                throw new InvalidConfigException("Agent logging level value [" + stringValue + "] defined in " + getKeyDisplayString(LEVEL_KEY) + " is invalid");
            }

            boolean stdOutEnabled = getEnabled(jsonObject, STDOUT_KEY, DEFAULT_STDOUT_ENABLED);
            boolean stdErrEnabled = getEnabled(jsonObject, STDERR_KEY, DEFAULT_STDERR_ENABLED);

            Object fileRawObject = jsonObject.opt(FILE_KEY);

            String locationString = null;
            Integer maxSize = null;
            Integer maxBackup = null;
            Path logFilePath = null;

            if (fileRawObject != null) {
                if (fileRawObject instanceof JSONObject) {
                    JSONObject fileObject = (JSONObject) fileRawObject;
                    checkUnknownKeys(fileObject, VALID_FILE_CONFIG_KEYS);

                    locationString = fileObject.getString(FILE_LOCATION_KEY); //mandatory

                    try {
                        logFilePath = Paths.get(locationString);
//                        if (!logFilePath.isAbsolute()) { //then use the agent directory as the base
//                            if (ResourceDirectory.getAgentDirectory() != null) {
//                                logFilePath = Paths.get(ResourceDirectory.getAgentDirectory(), logFilePath.toString());
//                            }
//                        }


                        maxSize = fileObject.has(FILE_MAX_SIZE_KEY) ? fileObject.getInt(FILE_MAX_SIZE_KEY) : null;
                        maxBackup = fileObject.has(FILE_MAX_BACKUP_KEY) ? fileObject.getInt(FILE_MAX_BACKUP_KEY) : null;

                        if (maxSize != null) {
                            if (maxSize < MAX_SIZE_MIN || maxSize > MAX_SIZE_MAX) {
                                throw new InvalidConfigException("Invalid value [" + maxSize + "] for " + getKeyDisplayString(FILE_MAX_SIZE_KEY) + ". The value should be in range of [" + MAX_SIZE_MIN + " , " + MAX_SIZE_MAX + "]");
                            }
                        }

                        if (maxBackup != null) {
                            if (maxBackup < MAX_BACKUP_MIN || maxBackup > MAX_BACKUP_MAX) {
                                throw new InvalidConfigException("Invalid value [" + maxBackup + "] for " + getKeyDisplayString(FILE_MAX_BACKUP_KEY) + ". The value should be in range of [" + MAX_BACKUP_MIN + " , " + MAX_BACKUP_MAX + "]");
                            }
                        }
                    } catch (InvalidPathException e) {
                        throw new InvalidConfigException("Log file path [" + locationString + "] is invalid : " + e.getMessage(), e);
                    }
                } else {
                    throw new InvalidConfigException("Expect json value for key [" + FILE_KEY + "] but found value [" + fileRawObject + "]");
                }
            }

            //validate whether stderr or stdout can be disabled
            if (locationString == null) {
                if (!stdErrEnabled) {
                    throw new InvalidConfigException("Cannot disable stderr on logging while no log file is defined in [" + ConfigProperty.AGENT_LOGGING.getConfigFileKey() + "]");
                }
                if (!stdOutEnabled) {
                    throw new InvalidConfigException("Cannot disable stdout on logging while no log file is defined in [" + ConfigProperty.AGENT_LOGGING.getConfigFileKey() + "]");
                }
            }

            return new LogSetting(level, stdOutEnabled, stdErrEnabled, logFilePath, maxSize, maxBackup);
        } catch (JSONException e) {
            throw new InvalidConfigException("Failed parsing log settings from config file: " + e.getMessage(), e);
        }
    }

    private static void checkUnknownKeys(JSONObject jsonObject, List<String> validKeys) throws InvalidConfigException {
        List<String> unknownKeys = new ArrayList<String>(jsonObject.keySet());
        unknownKeys.removeAll(validKeys);

        if (!unknownKeys.isEmpty()) {
            throw new InvalidConfigException("Found unknown key(s) [" + String.join(", ", unknownKeys) + "], valid keys are [" + String.join(", ", validKeys) + "]");
        }
    }

    private boolean getEnabled(JSONObject jsonObject, String key, boolean defaultValue) throws InvalidConfigException {
        try {
            String stringValue = jsonObject.optString(key, null);
            if (stringValue == null) {
                return defaultValue;
            } else {
                return ModeStringToBooleanParser.INSTANCE.convert(stringValue);
            }
        } catch (InvalidConfigException e) {
            throw new InvalidConfigException("Failed to parse " + getKeyDisplayString(key) + " : " + e.getMessage(), e);
        }
    }

    private String getKeyDisplayString(String key) {
        return "[" + key +  "] of [" + ConfigProperty.AGENT_LOGGING.getConfigFileKey() + "]";
    }

}
