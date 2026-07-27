package exloran.luxel.client.config;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Basit, bagimliliksiz (Cloth Config gerektirmeyen) ayarlar ekrani.
 * Her satir bir ayari acip kapatir veya bir sonraki degere gecirir;
 * degisiklikler aninda {@link LuxelConfig} uzerinden diske yazilir.
 */
public final class LuxelConfigScreen extends Screen {

	private final Screen parent;
	private final LuxelConfig config;

	private LuxelConfigScreen(Screen parent) {
		super(Text.translatable("luxel.config.title"));
		this.parent = parent;
		this.config = LuxelConfig.get();
	}

	public static LuxelConfigScreen create(Screen parent) {
		return new LuxelConfigScreen(parent);
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int y = 40;
		int rowHeight = 24;
		int buttonWidth = 300;

		this.addDrawableChild(toggleButton(centerX, y, buttonWidth, "luxel.config.mod_enabled",
				() -> config.modEnabled, v -> config.modEnabled = v));
		y += rowHeight;

		this.addDrawableChild(toggleButton(centerX, y, buttonWidth, "luxel.config.dynamic_lights",
				() -> config.dynamicLightsEnabled, v -> config.dynamicLightsEnabled = v));
		y += rowHeight;

		this.addDrawableChild(toggleButton(centerX, y, buttonWidth, "luxel.config.other_players",
				() -> config.otherPlayersLight, v -> config.otherPlayersLight = v));
		y += rowHeight;

		this.addDrawableChild(toggleButton(centerX, y, buttonWidth, "luxel.config.mob_light",
				() -> config.mobLight, v -> config.mobLight = v));
		y += rowHeight;

		this.addDrawableChild(toggleButton(centerX, y, buttonWidth, "luxel.config.dropped_item_light",
				() -> config.droppedItemLight, v -> config.droppedItemLight = v));
		y += rowHeight;

		this.addDrawableChild(toggleButton(centerX, y, buttonWidth, "luxel.config.colored_light",
				() -> config.coloredLight, v -> config.coloredLight = v));
		y += rowHeight;

		this.addDrawableChild(toggleButton(centerX, y, buttonWidth, "luxel.config.performance_mode",
				() -> config.performanceMode, v -> config.performanceMode = v));
		y += rowHeight;

		this.addDrawableChild(toggleButton(centerX, y, buttonWidth, "luxel.config.shader_compat",
				() -> config.shaderCompatMode, v -> config.shaderCompatMode = v));
		y += rowHeight;

		this.addDrawableChild(toggleButton(centerX, y, buttonWidth, "luxel.config.debug_mode",
				() -> config.debugMode, v -> config.debugMode = v));
		y += rowHeight;

		this.addDrawableChild(cycleButton(centerX, y, buttonWidth, "luxel.config.quality",
				LuxelConfig.Quality.values(), () -> config.lightQuality, v -> config.lightQuality = v));
		y += rowHeight;

		y += 10;
		this.addDrawableChild(ButtonWidget.builder(Text.translatable("luxel.config.done"), button -> {
			config.save();
			this.close();
		}).dimensions(centerX - buttonWidth / 2, y, buttonWidth, 20).build());
	}

	private ButtonWidget toggleButton(int centerX, int y, int width, String key,
			java.util.function.BooleanSupplier getter, java.util.function.Consumer<Boolean> setter) {
		boolean current = getter.getAsBoolean();
		return ButtonWidget.builder(labelFor(key, current), button -> {
			boolean next = !getter.getAsBoolean();
			setter.accept(next);
			button.setMessage(labelFor(key, next));
		}).dimensions(centerX - width / 2, y, width, 20).build();
	}

	private <E extends Enum<E>> ButtonWidget cycleButton(int centerX, int y, int width, String key,
			E[] values, java.util.function.Supplier<E> getter, java.util.function.Consumer<E> setter) {
		return ButtonWidget.builder(enumLabel(key, getter.get()), button -> {
			E current = getter.get();
			E next = values[(current.ordinal() + 1) % values.length];
			setter.accept(next);
			button.setMessage(enumLabel(key, next));
		}).dimensions(centerX - width / 2, y, width, 20).build();
	}

	private Text labelFor(String key, boolean value) {
		return Text.translatable(key).append(": ").append(Text.translatable(value ? "luxel.config.on" : "luxel.config.off"));
	}

	private <E extends Enum<E>> Text enumLabel(String key, E value) {
		return Text.translatable(key).append(": " + value.name());
	}

	@Override
	public void close() {
		config.save();
		if (this.client != null) {
			this.client.setScreen(parent);
		}
	}
}
