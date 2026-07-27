package exloran.luxel.client.config;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Bagimliliksiz (Cloth Config gerektirmeyen) ayarlar ekrani.
 * <p>
 * Her satir, degeri {@link LuxelConfig} singleton'i uzerinde DOGRUDAN degistirir
 * ve degisiklikten hemen sonra diske kaydeder ({@code config.save()}). LightSourceManager
 * her tick {@code LuxelConfig.get()} ile ayni singleton'u okudugu icin, yapilan degisiklik
 * ekstra bir "reload" islemine gerek kalmadan bir sonraki tick'te motora yansir.
 * <p>
 * "Diskten Yeniden Yukle" butonu, config dosyasi oyun disinda elle duzenlendiyse
 * (ornegin baska bir dosya yoneticisiyle) degisiklikleri anlik olarak ekrana ve
 * motora uygulamak icin vardir.
 */
public final class LuxelConfigScreen extends Screen {

	private final Screen parent;
	private LuxelConfig config;

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
		int y = 32;
		int rowHeight = 22;
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

		this.addDrawableChild(toggleButton(centerX, y, buttonWidth, "luxel.config.debug_mode",
				() -> config.debugMode, v -> config.debugMode = v));
		y += rowHeight;

		this.addDrawableChild(cycleButton(centerX, y, buttonWidth, "luxel.config.quality",
				LuxelConfig.Quality.values(), () -> config.lightQuality, v -> config.lightQuality = v));
		y += rowHeight;

		this.addDrawableChild(intSlider(centerX, y, buttonWidth, "luxel.config.max_distance",
				4, 48, () -> config.maxLightDistance, v -> config.maxLightDistance = v));
		y += rowHeight;

		this.addDrawableChild(intSlider(centerX, y, buttonWidth, "luxel.config.update_interval",
				1, 10, () -> config.updateIntervalTicks, v -> config.updateIntervalTicks = v));
		y += rowHeight;

		y += 6;
		this.addDrawableChild(ButtonWidget.builder(Text.translatable("luxel.config.reload"), button -> {
			this.config = LuxelConfig.reload();
			this.clearAndInit();
		}).dimensions(centerX - buttonWidth / 2, y, buttonWidth, 20).build());
		y += rowHeight + 4;

		this.addDrawableChild(ButtonWidget.builder(Text.translatable("luxel.config.done"), button -> {
			config.save();
			this.close();
		}).dimensions(centerX - buttonWidth / 2, y, buttonWidth, 20).build());
	}

	private ButtonWidget toggleButton(int centerX, int y, int width, String key,
			BooleanSupplier getter, Consumer<Boolean> setter) {
		boolean current = getter.getAsBoolean();
		return ButtonWidget.builder(boolLabel(key, current), button -> {
			boolean next = !getter.getAsBoolean();
			setter.accept(next);
			config.save();
			button.setMessage(boolLabel(key, next));
		}).dimensions(centerX - width / 2, y, width, 20).build();
	}

	private <E extends Enum<E>> ButtonWidget cycleButton(int centerX, int y, int width, String key,
			E[] values, Supplier<E> getter, Consumer<E> setter) {
		return ButtonWidget.builder(enumLabel(key, getter.get()), button -> {
			E current = getter.get();
			E next = values[(current.ordinal() + 1) % values.length];
			setter.accept(next);
			config.save();
			button.setMessage(enumLabel(key, next));
		}).dimensions(centerX - width / 2, y, width, 20).build();
	}

	private SliderWidget intSlider(int centerX, int y, int width, String key,
			int min, int max, IntSupplier getter, IntConsumer setter) {
		double initialProgress = clampProgress((getter.getAsInt() - min) / (double) (max - min));
		return new SliderWidget(centerX - width / 2, y, width, 20, intLabel(key, getter.getAsInt()), initialProgress) {
			@Override
			protected void updateMessage() {
				int value = min + (int) Math.round(this.value * (max - min));
				this.setMessage(intLabel(key, value));
			}

			@Override
			protected void applyValue() {
				int value = min + (int) Math.round(this.value * (max - min));
				setter.accept(value);
				config.save();
			}
		};
	}

	private double clampProgress(double value) {
		if (Double.isNaN(value)) {
			return 0.0;
		}
		return Math.max(0.0, Math.min(1.0, value));
	}

	private Text boolLabel(String key, boolean value) {
		return Text.translatable(key).append(": ").append(Text.translatable(value ? "luxel.config.on" : "luxel.config.off"));
	}

	private <E extends Enum<E>> Text enumLabel(String key, E value) {
		return Text.translatable(key).append(": " + value.name());
	}

	private Text intLabel(String key, int value) {
		return Text.translatable(key).append(": " + value);
	}

	@Override
	public void close() {
		config.save();
		if (this.client != null) {
			this.client.setScreen(parent);
		}
	}
}
