package rooftopfpmog;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.ui.overlay.infobox.Timer;
import net.runelite.client.game.SkillIconManager;
import net.runelite.api.SoundEffectID;


import javax.inject.Inject;
import java.time.temporal.ChronoUnit;
import java.awt.image.BufferedImage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@PluginDescriptor(
		name = "Rooftop Timer",
		description = "Starts a 2:15 timer after finishing a rooftop agility course"
)
public class rooftopfpmog extends Plugin
{
	private static final int COOLDOWN_SECONDS = 135;

	@Inject
	private Client client;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private RooftopTimerConfig config;

	@Inject
	private SkillIconManager skillIconManager;  // Use SkillIconManager for skill icons

	private Timer timer;
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	@Provides
	RooftopTimerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RooftopTimerConfig.class);
	}

	@Override
	protected void shutDown()
	{
		if (timer != null)
		{
			infoBoxManager.removeInfoBox(timer);
			timer = null;
		}
		scheduler.shutdownNow();
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		if (event.getMessage().contains("You have completed the course."))
		{
			startTimer();
		}
	}

	private void startTimer()
	{
		if (timer != null)
		{
			infoBoxManager.removeInfoBox(timer);
			timer = null;
		}

		BufferedImage image = skillIconManager.getSkillImage(Skill.AGILITY);  // Get Agility icon

		timer = new Timer(COOLDOWN_SECONDS, ChronoUnit.SECONDS, image, this);
		timer.setTooltip("Rooftop force-spawn cooldown"); // optional, remove if error
		infoBoxManager.addInfoBox(timer);


		infoBoxManager.addInfoBox(timer);

		// Schedule sound effect after cooldown
		scheduler.schedule(() ->
		{
			if (config.enableSound())
			{
				client.playSoundEffect(SoundEffectID.UI_BOOP);
			}
		}, COOLDOWN_SECONDS, TimeUnit.SECONDS);
	}

	@ConfigGroup("rooftoptimer")
	public interface RooftopTimerConfig extends Config
	{
		@ConfigItem(
				keyName = "enableSound",
				name = "Enable Sound",
				description = "Plays a sound when the timer ends"
		)
		default boolean enableSound()
		{
			return true;
		}
	}
}
