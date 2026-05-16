package moe.kyuunex.fumo_utils.modules;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import moe.kyuunex.fumo_utils.FumoUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.joml.Vector3d;

import java.util.Set;

public class EntityInfo extends Module {
    private static final Color BACKGROUND = new Color(0, 0, 0, 75);
    private static final Color TEXT = new Color(255, 255, 255);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> nearbyOnly = sgGeneral.add(new BoolSetting.Builder()
        .name("nearby-only")
        .description("Nearby entities only.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> howClose = sgGeneral.add(new IntSetting.Builder()
        .name("max-distance")
        .description("Max distance of entities to render the IDs of.")
        .defaultValue(30)
        .range(0, 30000)
        .sliderRange(0, 30)
        .visible(nearbyOnly::get)
        .build()
    );

    private final Setting<Boolean> filterEntities = sgGeneral.add(new BoolSetting.Builder()
        .name("filter-entities")
        .description("Filter entities or show all of them.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("What entities to render the IDs of.")
        .visible(filterEntities::get)
        .build()
    );

    private final Setting<Boolean> showEntityId = sgGeneral.add(new BoolSetting.Builder()
        .name("show-entity-id")
        .description("Show Entity IDs.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showNametags = sgGeneral.add(new BoolSetting.Builder()
        .name("show-nametags")
        .description("Show custom entity names too.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("The scale of the text.")
        .defaultValue(1)
        .min(0)
        .build()
    );

    private final Setting<Boolean> filterWithIds = sgGeneral.add(new BoolSetting.Builder()
        .name("filter-with-IDs")
        .description("Filter tags based on entity IDs")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> minimumID = sgGeneral.add(new IntSetting.Builder()
        .name("minimum-id")
        .description("Don't render entity tags bellow this ID")
        .defaultValue(-1)
        .sliderRange(-1, 10000)
        .visible(filterWithIds::get)
        .build()
    );

    private final Setting<Integer> maximumID = sgGeneral.add(new IntSetting.Builder()
        .name("maximum-id")
        .description("Don't render entity tags above this ID")
        .defaultValue(100000)
        .sliderRange(-1, 100000)
        .visible(filterWithIds::get)
        .build()
    );

    private final Vector3d pos = new Vector3d();

    public EntityInfo() {
        super(FumoUtils.CATEGORY, "entity-info", "Shows entity info like ID or names.");
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (filterWithIds.get()) {
                int min = minimumID.get();
                int max = maximumID.get();
                int id = entity.getId();

                if (id < min || id > max) {
                    continue;
                }
            }

            if (filterEntities.get()) {
                if (!entities.get().contains(entity.getType())) continue;
            }
            if (nearbyOnly.get()) {
                if (!PlayerUtils.isWithinCamera(entity, howClose.get())) continue;
            }
            Utils.set(pos, entity, event.tickDelta);
            pos.add(0, entity.getEyeHeight(entity.getPose()) + 0.75, 0);
            if (NametagUtils.to2D(pos, scale.get())) {
                List<String> visibleInfo = new ArrayList<>();

                if (showEntityId.get()) {
                    visibleInfo.add(String.format("%,d", entity.getId()));
                }
                if (showNametags.get() && entity.getCustomName() != null) {
                    visibleInfo.add(entity.getCustomName().getString());
                }

                if (!visibleInfo.isEmpty()) {
                    renderNametag(String.join(" - ", visibleInfo));
                }
            }
        }
    }

    private void renderNametag(String name) {
        TextRenderer text = TextRenderer.get();

        NametagUtils.begin(pos);
        text.beginBig();

        double w = text.getWidth(name);

        double x = -w / 2;
        double y = -text.getHeight();

        Renderer2D.COLOR.begin();
        Renderer2D.COLOR.quad(x - 1, y - 1, w + 2, text.getHeight() + 2, BACKGROUND);
        Renderer2D.COLOR.render();

        text.render(name, x, y, TEXT);

        text.end();
        NametagUtils.end();
    }
}
