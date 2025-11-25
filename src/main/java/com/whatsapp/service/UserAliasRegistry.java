package com.whatsapp.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro centralizado para mapear connectionIds a nombres de usuario legibles.
 */
public class UserAliasRegistry {
    private static final UserAliasRegistry INSTANCE = new UserAliasRegistry();
    private final ConcurrentHashMap<String, String> aliases = new ConcurrentHashMap<>();

    private UserAliasRegistry() {
    }

    public static UserAliasRegistry getInstance() {
        return INSTANCE;
    }

    public void registerAlias(String connectionId, String alias) {
        if (connectionId == null || alias == null || alias.isBlank()) {
            return;
        }
        aliases.put(connectionId, alias);
    }

    public void removeAlias(String connectionId) {
        if (connectionId != null) {
            aliases.remove(connectionId);
        }
    }

    public String getAliasOrDefault(String connectionId) {
        if (connectionId == null) {
            return "";
        }
        return aliases.getOrDefault(connectionId, connectionId);
    }

    public Map<String, String> snapshot() {
        return Collections.unmodifiableMap(new HashMap<>(aliases));
    }
}


