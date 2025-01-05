package net.enelson.astract.regionlimiter.utils;

import org.bukkit.entity.Player;

import me.clip.placeholderapi.PlaceholderAPI;
import net.enelson.astract.regionlimiter.ARegionLimiter;
import net.md_5.bungee.api.ChatColor;

public enum Message {

	CLEANER_DELETE_REGION("cleaner-delete-region", "Region \"%region%\" has been deleted."),
	CLAIM_MIN_X("claim-min-x", "&3&l「ʀᴇɢɪᴏɴ」&8▹ &bThe size of the selected area along the X axis (&dfrom west to east&b) must not be less than %size%."),
	CLAIM_MAX_X("claim-max-x", "&3&l「ʀᴇɢɪᴏɴ」&8▹ &bThe size of the selected area along the X axis (&dfrom west to east&b) must not be more than %size%."),
	CLAIM_MIN_Y("claim-min-y", "&3&l「ʀᴇɢɪᴏɴ」&8▹ &bThe size of the selected area along the Y axis (&dby height&b) must not be less than %size%."),
	CLAIM_MAX_Y("claim-max-y", "&3&l「ʀᴇɢɪᴏɴ」&8▹ &bThe size of the selected area along the Y axis (&dby height&b) must not be more than %size%."),
	CLAIM_MIN_Z("claim-min-z", "&3&l「ʀᴇɢɪᴏɴ」&8▹ &bThe size of the selected area along the Z axis (&dfrom north to south&b) must not be less than %size%."),
	CLAIM_MAX_Z("claim-max-z", "&3&l「ʀᴇɢɪᴏɴ」&8▹ &bThe size of the selected area along the Z axis (&dfrom north to south&b) must not be more than %size%."),
	CLAIM_CURRENT_SIZE("current-size", "&3&l「ʀᴇɢɪᴏɴ」&8▹ &bCurrent size &7(x,y,z)&b: &8(&2%x%&8,&2%y%&8,&2%z%&8)"),
	UNKNOWN_REGION_TYPE("unknown-region-type", "&3&l「ʀᴇɢɪᴏɴ」&8▹ &cUnknown region type."),
	MAX_COUNT_IN_GLOBAL("max-count-in-global", "&3&l「ʀᴇɢɪᴏɴ」&8▹ &cYou have reached the maximum number of global regions."),
	MAX_COUNT_IN_OWN_REGION("max-count-in-own-region", "&3&l「ʀᴇɢɪᴏɴ」&8▹ &cYou have reached the maximum number of child regions."),
	REGION_EXIST("region-exist", "&3&l「ʀᴇɢɪᴏɴ」&8▹ &cA region with that name already exists."),
	SUCCESS_IN_GLOBAL("success-in-global", "&3&l「ʀᴇɢɪᴏɴ」&8▹ &aRegion named \"&6%region%&a\" has been successfully created."),
	SUCCESS_IN_OWN_REGION("success-in-own-region", "&3&l「ʀᴇɢɪᴏɴ」&8▹ &aChild region named \"&6%region%&a\" has been successfully created."),
	ANOTHER_REGION_AFFECTED("another-region-affected", "&3&l「ʀᴇɢɪᴏɴ」&8▹ &cAnother region has been affected."),
	NAME_NOT_ENTERED("name-not-entered", "&3&l「ʀᴇɢɪᴏɴ」&8▹ &cRegion name has not been entered."),
	INVALID_WORLD("invalid-world", "&3&l「ʀᴇɢɪᴏɴ」&8▹ &cSuch a world does not exist: &6%world%&c."),
	INVALID_REGION("invalid-region", "&3&l「ʀᴇɢɪᴏɴ」&8▹ &cRegion \"&6%region%&c\" does not exist in the world \"&6%world%&c\"."),
	INVALID_NAME("invalid-name", "&3&l「ʀᴇɢɪᴏɴ」&8▹ &cInvalid region name. Only Latin characters, numbers, and the underscore _ are allowed, with a length between 3 and 20 characters."),
	NO_PERM_REMOVE("no-perm-remove", "&3&l「ʀᴇɢɪᴏɴ」&8▹ &cYou do not have permission to delete this region."),
	REMOVE_REGION("remove-region", "&3&l「ʀᴇɢɪᴏɴ」&8▹ &aRegion \"&6%region%&a\" has removed."),
	REMOVE_REGION_PARENT("remove-region-parent", "&3&l「ʀᴇɢɪᴏɴ」&8▹ &aRegion \"&6%region%&a\" and all child regions removed (&e%children%&a)."),
	REMOVE_REGION_CHILD("remove-region-child", "&3&l「ʀᴇɢɪᴏɴ」&8▹ &aChild region \"&6%region%&a\" has removed."),
	SOMETHING_WRONG("something-wrong", "&3&l「ʀᴇɢɪᴏɴ」&8▹ &cSomething went wrong. Contact the administrator.");
	
	private String path;
	private String message;
	
	Message(String path, String message) {
		this.path = path;
		this.message = message;
	}
	
	public String getMessage() {
		String result = ARegionLimiter.getInstance().getLocaleConfig().getString(this.path);
		if(result != null) return result;
		return this.message;
	}
	
	public String getDefautMessage() {
		return this.message;
	}
	
	public String getMessageWithPlaceholders(Player player) {
		return ChatColor.translateAlternateColorCodes('&', PlaceholderAPI.setPlaceholders(player, this.getMessage()));
	}
}
