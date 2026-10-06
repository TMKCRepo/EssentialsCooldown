package com.tmkc.essentialscooldown;

import java.util.Set;

/**
 * Supplies the command labels (name + aliases) a server actually knows for a
 * command, so a hand-written config entry cannot be dodged through an alias
 * the author did not know about.
 */
interface AliasResolver {

    /**
     * Every label the server registers for {@code command} (lower-case, namespace
     * stripped), or an empty set if the command is unknown.
     */
    Set<String> aliasesFor(String command);
}
