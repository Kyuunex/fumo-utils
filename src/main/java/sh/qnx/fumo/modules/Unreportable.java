package sh.qnx.fumo.modules;

import sh.qnx.fumo.FumoUtils;
import meteordevelopment.meteorclient.systems.modules.Module;


public class Unreportable extends Module {
    public Unreportable() {
        super(FumoUtils.CATEGORY, "unreportable", "Makes your chat messages unreportable.");
    }
}
