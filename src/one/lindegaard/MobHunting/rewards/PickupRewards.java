package one.lindegaard.MobHunting.rewards;

import one.lindegaard.BagOfGold.BagOfGold;
import one.lindegaard.BagOfGold.storage.PlayerSettings;
import one.lindegaard.MobHunting.Messages;
import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.compatibility.BagOfGoldCompat;
import one.lindegaard.MobHunting.compatibility.ProtocolLibCompat;
import one.lindegaard.MobHunting.compatibility.ProtocolLibHelper;
import one.lindegaard.MobHunting.util.Misc;

import org.bukkit.ChatColor;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;

public class PickupRewards {

	private MobHunting plugin;

	public PickupRewards(MobHunting plugin) {
		this.plugin = plugin;
	}

	public void rewardPlayer(Player player, Item item, CallBack callBack) {

		if (Reward.isReward(item)) {
			Reward reward = Reward.getReward(item);
			Messages.debug("The reward name is = %s", reward.getDisplayname());
			if (BagOfGoldCompat.isSupported() && reward.isBagOfGoldReward()) {
				Messages.debug("use BagOfGold deposit to pickup");
				PlayerSettings ps = BagOfGold.getInstance().getPlayerSettingsManager().getPlayerSettings(player);
				ps.setBalance(Misc.round(ps.getBalance()+reward.getMoney()));
				BagOfGold.getInstance().getPlayerSettingsManager().setPlayerSettings(player, ps);
				BagOfGold.getInstance().getDataStoreManager().updatePlayerSettings(player, ps);
			} else {
				// If not Gringotts
				if (reward.getMoney() != 0)
					if (!MobHunting.getConfigManager().dropMoneyOnGroundUseAsCurrency) {
						plugin.getRewardManager().depositPlayer(player, reward.getMoney());
						if (ProtocolLibCompat.isSupported())
							ProtocolLibHelper.pickupMoney(player, item);
						item.remove();
						callBack.setCancelled(true);
						plugin.getMessages().playerActionBarMessage(player, Messages.getString("mobhunting.moneypickup",
								"money", plugin.getRewardManager().format(reward.getMoney())));
					} else {
						Messages.debug("Looking for a BagOfGold");
						boolean found = false;
						HashMap<Integer, ? extends ItemStack> slots = player.getInventory()
								.all(item.getItemStack().getType());
						for (int slot : slots.keySet()) {
							ItemStack is = player.getInventory().getItem(slot);
							if (Reward.isReward(is)) {
								Reward rewardInSlot = Reward.getReward(is);
								if ((reward.isBagOfGoldReward() || reward.isItemReward())
										&& (rewardInSlot.getRewardUUID().equals(reward.getRewardUUID())
												&& rewardInSlot.getDisplayname().equals(reward.getDisplayname()))) {
									ItemMeta im = is.getItemMeta();
									Reward newReward = Reward.getReward(is);
									newReward.setMoney(Misc.round(newReward.getMoney() + reward.getMoney()));
									im.setLore(newReward.getHiddenLore());
									String displayName = MobHunting.getConfigManager().dropMoneyOnGroundItemtype
											.equalsIgnoreCase("ITEM")
													? plugin.getRewardManager().format(Misc.round(newReward.getMoney()))
													: newReward.getDisplayname() + " (" + plugin.getRewardManager()
															.format(Misc.round(newReward.getMoney())) + ")";
									im.setDisplayName(
											ChatColor.valueOf(MobHunting.getConfigManager().dropMoneyOnGroundTextColor)
													+ displayName);
									is.setItemMeta(im);
									is.setAmount(1);
									callBack.setCancelled(true);
									if (ProtocolLibCompat.isSupported())
										ProtocolLibHelper.pickupMoney(player, item);
									item.remove();
									Messages.debug("Added %s to item in slot %s, new value is %s (PickupRewards)",
											plugin.getRewardManager().format(Misc.round(reward.getMoney())), slot,
											plugin.getRewardManager().format(Misc.round(newReward.getMoney())));
									found = true;
									break;
								}
							}
						}
						Messages.debug("Found=%s",found);
						if (!found) {
							ItemStack is = item.getItemStack();
							ItemMeta im = is.getItemMeta();
							String displayName = MobHunting.getConfigManager().dropMoneyOnGroundItemtype
									.equalsIgnoreCase("ITEM")
											? plugin.getRewardManager().format(Misc.round(reward.getMoney()))
											: reward.getDisplayname() + " ("
													+ plugin.getRewardManager().format(Misc.round(reward.getMoney()))
													+ ")";
							im.setDisplayName(
									ChatColor.valueOf(MobHunting.getConfigManager().dropMoneyOnGroundTextColor)
											+ displayName);
							im.setLore(reward.getHiddenLore());
							is.setItemMeta(im);
							is.setAmount(1);
							item.setItemStack(is);
							callBack.setCancelled(true);
							if (ProtocolLibCompat.isSupported())
								ProtocolLibHelper.pickupMoney(player, item);
							item.remove();
							player.getInventory().addItem(is);
						}
					}
			}
			if (plugin.getRewardManager().getDroppedMoney().containsKey(item.getEntityId()))
				plugin.getRewardManager().getDroppedMoney().remove(item.getEntityId());
			if (reward.getMoney() == 0)
				Messages.debug("%s picked up a %s (# of rewards left=%s)", player.getName(), reward.getDisplayname(),
						plugin.getRewardManager().getDroppedMoney().size());
			else {
				Messages.debug("%s picked up a %s with a value:%s (# of rewards left=%s)(PickupRewards)",
						player.getName(), reward.getDisplayname(),
						plugin.getRewardManager().format(Misc.round(reward.getMoney())),
						plugin.getRewardManager().getDroppedMoney().size());
			}

		}
	}

	public interface CallBack {

		void setCancelled(boolean canceled);

	}

}
