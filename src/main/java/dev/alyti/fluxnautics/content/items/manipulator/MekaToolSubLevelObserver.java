package dev.alyti.fluxnautics.content.items.manipulator;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelObserver;
import net.minecraft.server.level.ServerLevel;

public class MekaToolSubLevelObserver implements SubLevelObserver {
    private final ServerLevel level;


    public MekaToolSubLevelObserver(ServerLevel level) {
        this.level = level;
    }


    /**
     * Called every tick for each {@link SubLevelContainer}.
     *
     * @param subLevels the sub-level container that is ticking
     */
    @Override
    public void tick(SubLevelContainer subLevels) {
        this.getPhysicsHandler().tick();
    }


    public MekaToolServerHandler getPhysicsHandler() {
        return MekaToolServerHandler.get(this.level);
    }
}
