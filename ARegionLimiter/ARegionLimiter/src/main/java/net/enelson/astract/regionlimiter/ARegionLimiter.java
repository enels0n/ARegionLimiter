package net.enelson.astract.regionlimiter;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.earth2me.essentials.Essentials;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StringFlag;

import net.enelson.astract.regionlimiter.commands.CommandManager;
import net.enelson.astract.regionlimiter.managers.CleanerManager;
import net.enelson.astract.regionlimiter.managers.LimitManager;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;

public class ARegionLimiter extends JavaPlugin {

	private static ARegionLimiter plugin;
	private File file;
	private YamlConfiguration config;
	private File fileLocale;
	private YamlConfiguration configLocale;
	private CleanerManager cleaner;
	private LimitManager limiter;
	private LuckPerms luckperms;
	private Essentials essentials;
	private WorldGuard worldguard;
	private WorldEditPlugin worldedit;
	private StringFlag RL_CHILDS;
	private StringFlag RL_PARENT;
	private Map<Flag<?>, String> parentAutoFlags = new HashMap<>();
	private Map<Flag<?>, String> childAutoFlags = new HashMap<>();

	public void onEnable() {
		plugin = this;
		this.essentials = (Essentials)Bukkit.getPluginManager().getPlugin("Essentials");
		
		Plugin wg = Bukkit.getPluginManager().getPlugin("WorldGuard");
		Plugin we = Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
		if(wg == null || we == null) {
			Bukkit.getLogger().warning("WorldGuard or WorldEdit are not installed! The plugin has been disabled.");
			return;
		}
		
		this.luckperms = null;
		if(Bukkit.getServer().getPluginManager().getPlugin("LuckPerms") != null) {
			this.luckperms = LuckPermsProvider.get();;
		}
		
		this.worldguard = WorldGuard.getInstance();
		this.worldedit = (WorldEditPlugin)we;
		
		this.reloadConfig();
		
		this.cleaner = new CleanerManager();
		this.limiter = new LimitManager();
		
		this.getCommand("aregionlimiter").setExecutor(new CommandManager());
	}
	
    @Override
    public void onLoad() {
    	RL_CHILDS = new StringFlag("rl-childs");
    	RL_PARENT = new StringFlag("rl-parent");
    	WorldGuard.getInstance().getFlagRegistry().register(RL_CHILDS);
    	WorldGuard.getInstance().getFlagRegistry().register(RL_PARENT);
    }
	
	public void ondisable() {
		this.cleaner.deInit();
	}
	
	public static ARegionLimiter getInstance() {
		return plugin;
	}
	
	public CleanerManager getCleanerManager() {
		return this.cleaner;
	}
	
	public Essentials getEssentials() {
		return this.essentials;
	}
	
	public WorldGuard getWorldGuard() {
		return this.worldguard;
	}
	
	public WorldEditPlugin getWorldEdit() {
		return this.worldedit;
	}
	
	public LuckPerms getLuckPerms() {
		return this.luckperms;
	}
	
	public YamlConfiguration getConfig() {
		return this.config;
	}
	
	public YamlConfiguration getLocaleConfig() {
		return this.configLocale;
	}
	
	public StringFlag getChildsFlag() {
		return this.RL_CHILDS;
	}
	
	public Map<Flag<?>, String> getParentAutoFlags() {
		return this.parentAutoFlags;
	}
	
	public Map<Flag<?>, String> getChildAutoFlags() {
		return this.childAutoFlags;
	}
	
	public StringFlag getParentFlag() {
		return this.RL_PARENT;
	}
	
	public void reloadConfig() {
		this.file = new File(getDataFolder(), "config.yml");
		if (!this.file.exists()) saveResource("config.yml", true);
		this.config = YamlConfiguration.loadConfiguration(this.file);
		
		this.fileLocale = new File(getDataFolder(), "lang.yml");
		if (!this.fileLocale.exists()) saveResource("lang.yml", true);
		this.configLocale = YamlConfiguration.loadConfiguration(this.fileLocale);
		
		if(this.config.getBoolean("cleaner.enabled")) {
			if(this.essentials != null)
				this.cleaner = new CleanerManager();
			else
				Bukkit.getLogger().warning("Essentials is not installed - cleaner cannot be launched.");
		}

		ConfigurationSection autoflagsSection = this.config.getConfigurationSection("limit.in-global-default-flags");
		if (autoflagsSection != null) {
			for (String flagStr : autoflagsSection.getKeys(false)) {
				Flag<?> flag = Flags.fuzzyMatchFlag(WorldGuard.getInstance().getFlagRegistry(), flagStr);
				if (flag != null) {
					parentAutoFlags.put(flag, autoflagsSection.getString(flagStr));
				}
			}
		}

		autoflagsSection = this.config.getConfigurationSection("limit.in-own-region-default-flags");
		if (autoflagsSection != null) {
			for (String flagStr : autoflagsSection.getKeys(false)) {
				Flag<?> flag = Flags.fuzzyMatchFlag(WorldGuard.getInstance().getFlagRegistry(), flagStr);
				if (flag != null) {
					childAutoFlags.put(flag, autoflagsSection.getString(flagStr));
				}
			}
		}
	}
}
