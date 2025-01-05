package net.enelson.astract.regionlimiter.managers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import com.earth2me.essentials.User;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;

import net.enelson.astract.regionlimiter.ARegionLimiter;
import net.enelson.astract.regionlimiter.utils.Message;

public class CleanerManager {
	private BukkitTask task;

	public CleanerManager() {
		this.startTasker();
	}

	public void deInit() {
		if (this.task != null && !this.task.isCancelled())
			this.task.cancel();
	}
	
	public void reloadTask() {
		this.deInit();
		this.startTasker();
	}

	private void startTasker() {
		this.task = Bukkit.getScheduler().runTaskTimerAsynchronously(ARegionLimiter.getInstance(), new Runnable() {
			@Override
			public void run() {
				for (World world : Bukkit.getWorlds()) {
					if (ARegionLimiter.getInstance().getConfig().getList("cleaner.filter.ignore-worlds").contains(world.getName()))
						continue;
					
					List<ProtectedRegion> clean = new ArrayList<>();
					RegionContainer container = ARegionLimiter.getInstance().getWorldGuard().getPlatform().getRegionContainer();
					RegionManager regions = container.get(BukkitAdapter.adapt(world));
					regions.getRegions().forEach((k, region) -> {
						if(k.equals("__global__") || ARegionLimiter.getInstance().getConfig().getList("cleaner.filter.ignore-regions").contains(k)
								|| (ARegionLimiter.getInstance().getConfig().getBoolean("cleaner.filter.ignore-without-owners") && region.getOwners().size()==0))
							return;
						

						for (UUID uuid : region.getOwners().getUniqueIds()) { 
							if(!isWasLongAgo(uuid)) return;
						}
						
						for (UUID uuid : region.getMembers().getUniqueIds()) { 
							if(!isWasLongAgo(uuid)) return;
						}
						
						clean.add(region);
					});
					
					Bukkit.getScheduler().runTaskLater(ARegionLimiter.getInstance(), new Runnable() {
						@Override
						public void run() {
							for(ProtectedRegion region : clean) {
								regions.removeRegion(region.getId());
								if(ARegionLimiter.getInstance().getConfig().getBoolean("cleaner.log-in-console"))
									Bukkit.getLogger().info(Message.CLEANER_DELETE_REGION.getMessage());
							}
						}
					}, 1);
				}
			}
		}, 20 * 5, 20 * ARegionLimiter.getInstance().getConfig().getInt("cleaner.period"));
	}

	public boolean isWasLongAgo(UUID uuid) {
		User user = ARegionLimiter.getInstance().getEssentials().getUser(uuid);
		if (user != null) {
	        Date lastLoginDate = new Date(user.getLastLogin());
	        Date currentDate = new Date();

	        long differenceInMillis = currentDate.getTime() - lastLoginDate.getTime();
	        long differenceInDays = TimeUnit.DAYS.convert(differenceInMillis, TimeUnit.MILLISECONDS);

	        return differenceInDays > ARegionLimiter.getInstance().getConfig().getInt("cleaner.expire-time");
		}
		return false;
	}
}
