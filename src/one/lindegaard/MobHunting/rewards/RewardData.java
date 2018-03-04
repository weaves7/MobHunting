package one.lindegaard.MobHunting.rewards;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;

import one.lindegaard.MobHunting.config.ConfigManager;
import one.lindegaard.MobHunting.mobs.MobPlugin;

public class RewardData {
	private final static int PROGRESS_ACHIEVEMENT_LEVEL1 = 50;
	private MobPlugin mobPluginName;
	private String mobType = "";
	private String mobName = "";

	private boolean mobEnabled = true;
	private String money = "5";
	private double chance = 1;
	private String message = "";

	private List<HashMap<String, String>> consoleRunCommand = new ArrayList<HashMap<String, String>>();

	@Deprecated
	private int propability = 100;
	@Deprecated
	private int propabilityBase = 100;
	private int progressAchievementLevel1 = PROGRESS_ACHIEVEMENT_LEVEL1;

	// McMMO
	private double mcMMOSkillRewardChance = 0.02;
	private int mcMMOSkillRewardAmount = 1;

	public RewardData() {
		super();
	}

	public RewardData(MobPlugin pluginName, String mobType, String mobName, boolean mobEnabled, String rewardPrize,
			double chance, String message, List<HashMap<String, String>> cmd, int mcmmo_xp, double mcmmo_chance) {

		this.mobPluginName = pluginName;
		this.mobType = mobType;
		this.mobName = mobName;

		this.setMobEnabled(mobEnabled);
		this.money = rewardPrize;
		this.chance = chance;
		this.message = message;

		this.consoleRunCommand = cmd != null ? cmd : new ArrayList<HashMap<String, String>>();
		this.setMcMMOSkillRewardAmount(mcmmo_xp);
		this.setMcMMOSkillRewardChance(mcmmo_chance);
	}

	// **************************************************************************
	// Getters and Setters
	// **************************************************************************
	public MobPlugin getMobPlugin() {
		return mobPluginName;
	}

	public void setMobPlugin(MobPlugin mobPlugin) {
		this.mobPluginName = mobPlugin;
	}

	public String getMobType() {
		return mobType;
	}

	public void setMobType(String type) {
		this.mobType = type;
	}

	public String getMobName() {
		if (mobName == null || mobName.equals(""))
			return mobType;
		return mobName;
	}

	public void setMobName(String mobName) {
		this.mobName = mobName;
	}

	public boolean isMobEnabled() {
		return mobEnabled;
	}

	public void setMobEnabled(boolean mobEnabled) {
		this.mobEnabled = mobEnabled;
	}

	public String getRewardPrize() {
		return money;
	}

	public void setRewardPrize(String rewardPrize) {
		this.money = rewardPrize;
	}

	public String getRewardDescription() {
		return message;
	}

	public void setRewardDescription(String rewardDescription) {
		this.message = rewardDescription;
	}

	public List<HashMap<String, String>> getConsoleRunCommand() {
		return consoleRunCommand;
	}

	public void setConsoleRunCommand(List<HashMap<String, String>> consoleRunCommand) {
		this.consoleRunCommand = consoleRunCommand;
	}

	public int getPropability() {
		return propability;
	}

	public void setPropability(int propability) {
		this.propability = propability;
	}

	public int getPropabilityBase() {
		return propabilityBase;
	}

	public void setPropabilityBase(int propabilityBase) {
		this.propabilityBase = propabilityBase;
	}

	public double getChance() {
		return chance;
	}

	public void setChance(int chance) {
		this.chance = chance;
	}

	public int getAchivementLevel1() {
		return progressAchievementLevel1;
	}

	public void setAchivementLevel1(int achivementLevel1) {
		this.progressAchievementLevel1 = achivementLevel1;
	}

	// **************************************************************************
	// Load & Save
	// **************************************************************************
	public void save(ConfigurationSection section) {
		section.set("plugin", mobPluginName.toString());

		section.set("name", mobName);
		section.set("enabled", mobEnabled);
		section.set("money.amount", money);
		section.set("money.chance", chance);
		section.set("message", message);

		section.set("commands", consoleRunCommand != null ? consoleRunCommand : new ArrayList<>());

		section.set("mcmmo.chance", mcMMOSkillRewardChance);
		section.set("mcmmo.xp", mcMMOSkillRewardAmount);
		section.set("achievement_level1", progressAchievementLevel1);

	}

	@SuppressWarnings("unchecked")
	public void read(ConfigurationSection section) throws InvalidConfigurationException, IllegalStateException {
		mobPluginName = MobPlugin.valueOf(section.get("plugin").toString());
		mobName = section.getString("mobName");

		if (section.get("enabled") != null) {
			mobEnabled = section.getBoolean("enabled", true);
			money = section.getString("money.amount", "5");
			chance = section.getDouble("money.chance", 1);
			message = section.getString("message", "You got a reward");

			if (section.get("commands") != null)
				consoleRunCommand = (List<HashMap<String, String>>) section.get("commands");
			else
				consoleRunCommand = new ArrayList<>();

			mcMMOSkillRewardChance = section.getDouble("mcmmo.chance", 0.02);
			mcMMOSkillRewardAmount = section.getInt("mcmmo.xp", 1);

			progressAchievementLevel1 = section.getInt("achivement_level1", PROGRESS_ACHIEVEMENT_LEVEL1);
		} else {
			// enabled is missing, convert data
			mobEnabled = true;
			money = section.getString("rewardPrize");
			if (section.get("chance") != null)
				chance = section.getDouble("chance");
			else {
				propability = section.getInt("propability");
				propabilityBase = section.getInt("propabilityBase");
				chance = propability / propabilityBase;
			}
			message = section.getString("rewardDescription", "");

			if (section.get("consoleRunCommand") instanceof String)
				consoleRunCommand = ConfigManager.convertCommands((String) section.get("consoleRunCommand"),
						Double.valueOf(chance));

			mcMMOSkillRewardChance = section.getDouble("mcmmo_chance", 0.02);
			mcMMOSkillRewardAmount = section.getInt("mcmmo_xp", 1);

			progressAchievementLevel1 = section.getInt("achivement_level1", PROGRESS_ACHIEVEMENT_LEVEL1);
		}
	}

	public double getMcMMOSkillRewardChance() {
		return mcMMOSkillRewardChance;
	}

	public void setMcMMOSkillRewardChance(double mcMMOSkillRewardChance) {
		this.mcMMOSkillRewardChance = mcMMOSkillRewardChance;
	}

	public int getMcMMOSkillRewardAmount() {
		return mcMMOSkillRewardAmount;
	}

	public void setMcMMOSkillRewardAmount(int mcMMOSkillRewardAmount) {
		this.mcMMOSkillRewardAmount = mcMMOSkillRewardAmount;
	}

}
