package net.enelson.astract.regionlimiter.managers;

import org.bukkit.Bukkit;

import net.enelson.astract.regionlimiter.ARegionLimiter;

public class LimitManager {
	
	public LimitManager() {
		Bukkit.getPluginManager().registerEvents(new ClaimHandler(), ARegionLimiter.getInstance());
		Bukkit.getPluginManager().registerEvents(new RemoveHandler(), ARegionLimiter.getInstance());
	}
}
