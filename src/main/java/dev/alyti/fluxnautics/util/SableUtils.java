package dev.alyti.fluxnautics.util;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;

import java.util.UUID;

public class SableUtils {
    public static ServerSubLevel resolveRootAssembly(ServerSubLevel subLevel) {
        ServerSubLevel current = subLevel;
        while (true) {
            UUID splitFrom = current.getSplitFromSubLevel();
            if (splitFrom == null) {
                return current;
            }
            SubLevel parent = SubLevelContainer.getContainer(current.getLevel()).getSubLevel(splitFrom);
            if (!(parent instanceof ServerSubLevel parentServer)) {
                return current;
            }
            current = parentServer;
        }
    }
}
