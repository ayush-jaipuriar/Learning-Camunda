package com.example.disputeresolutionsystem.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import java.util.List;

@Slf4j
@Configuration
public class JavaScriptEngineConfig {
    
    @PostConstruct
    public void init() {
        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        
        try {
            // Log all available script engines
            List<ScriptEngineFactory> factories = scriptEngineManager.getEngineFactories();
            if (factories.isEmpty()) {
                log.warn("No script engines found!");
            } else {
                log.info("Found {} script engines:", factories.size());
                for (ScriptEngineFactory factory : factories) {
                    log.info("Engine: {} ({}) - Language: {} ({})",
                        factory.getEngineName(),
                        factory.getEngineVersion(),
                        factory.getLanguageName(), 
                        factory.getLanguageVersion());
                }
            }
            
            // Check if JavaScript engine is available
            ScriptEngine jsEngine = scriptEngineManager.getEngineByName("JavaScript");
            ScriptEngine nashornEngine = scriptEngineManager.getEngineByName("nashorn");
            
            log.info("JavaScript engine available: {}", jsEngine != null);
            log.info("Nashorn engine available: {}", nashornEngine != null);
            
        } catch (Exception e) {
            log.error("Error checking script engines", e);
        }
    }
} 