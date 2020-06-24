package io.quarkus.funqy.runtime.bindings.brasilia;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "funqy.brasilia", phase = ConfigPhase.RUN_TIME)
public class FunqyBrasiliaConfig {

    @ConfigGroup
    public static class FunctionMapping {

        /**
         *
         */
        @ConfigItem
        public Optional<String> name;

        /**
         *
         */
        @ConfigItem
        public Optional<String> version;

        /**
         *
         */
        @ConfigItem
        public Optional<String> config;
    }

    /**
     *
     */
    @ConfigItem
    public Map<String, FunctionMapping> mapping;

    /**
     *
     */
    @ConfigItem(defaultValue = "quarkus-app")
    public Optional<String> appId;

    /**
     * 
     */
    @ConfigItem(defaultValue = "1.0.0")
    public Optional<String> appVersion;
}
