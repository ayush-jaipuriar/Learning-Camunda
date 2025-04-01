package com.example.disputeresolutionsystem.config;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.scripting.engine.ScriptEngineResolver;
import org.camunda.bpm.spring.boot.starter.configuration.impl.AbstractCamundaConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom configuration for the Camunda process engine to handle JavaScript scripting.
 */
@Slf4j
@Configuration
public class DisputeResolutionProcessEngineConfiguration {

    @Bean
    public AbstractCamundaConfiguration processEngineCustomConfiguration() {
        return new AbstractCamundaConfiguration() {
            @Override
            public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
                log.info("Configuring custom scripting for Camunda process engine");
                
                // Create a custom script engine resolver
                processEngineConfiguration.setScriptEngineResolver(new CustomScriptEngineResolver());
                
                // Enable script compilation
                processEngineConfiguration.setEnableScriptCompilation(true);
                
                log.info("Custom scripting configured for Camunda process engine");
            }
        };
    }

    /**
     * Custom script engine resolver that prioritizes the Nashorn engine for JavaScript.
     */
    private static class CustomScriptEngineResolver implements ScriptEngineResolver {
        private final ScriptEngineManager scriptEngineManager;

        public CustomScriptEngineResolver() {
            this.scriptEngineManager = new ScriptEngineManager();
        }

        public ScriptEngine getScriptEngine(String language) {
            return getScriptEngine(language, true);
        }
        
        public ScriptEngine getScriptEngine(String language, boolean cached) {
            if (language == null) {
                return null;
            }
            
            // Handle JavaScript specifically
            if (language.equalsIgnoreCase("javascript") || 
                language.equalsIgnoreCase("js") || 
                language.equalsIgnoreCase("ecmascript")) {
                
                // Try to get the Nashorn engine first
                ScriptEngine engine = scriptEngineManager.getEngineByName("nashorn");
                if (engine != null) {
                    return engine;
                }
                
                // Fall back to any JavaScript engine
                return scriptEngineManager.getEngineByName("JavaScript");
            }
            
            // For other languages, use the standard mechanism
            return scriptEngineManager.getEngineByName(language);
        }
        
        public void addScriptEngineFactory(ScriptEngineFactory scriptEngineFactory) {
            // Not implemented - we're using the default ScriptEngineManager behavior
        }
        
        public ScriptEngineManager getScriptEngineManager() {
            return this.scriptEngineManager;
        }
    }
} 