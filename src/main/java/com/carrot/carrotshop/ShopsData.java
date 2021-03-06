package com.carrot.carrotshop;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Stack;
import java.util.UUID;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.carrot.carrotshop.serializer.CurrencySerializer;
import com.carrot.carrotshop.serializer.InventorySerializer;
import com.carrot.carrotshop.shop.*;
import com.google.common.reflect.TypeToken;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializerCollection;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;


public class ShopsData {
	private static File carrotshopsFile;
	private static ConfigurationNode shopsNode;
	private static ConfigurationLoader<CommentedConfigurationNode> loader;
	private static Map<Location<World>, List<Shop>> shops = new HashMap<>();
	private static Hashtable<UUID, Stack<Location<World>>> storedLocations = new Hashtable<>();
	private static HashSet<UUID> soldThings = new HashSet<>();
	private static Currency currency = null;

	public static void init(File rootDir) throws IOException
	{
		carrotshopsFile = new File(rootDir, "shops.json");
		rootDir.mkdirs();
		carrotshopsFile.createNewFile();

		TypeSerializerCollection serializers = TypeSerializers.getDefaultSerializers().newChild();
		serializers.registerType(TypeToken.of(Inventory.class), new InventorySerializer());
		serializers.registerType(TypeToken.of(Currency.class), new CurrencySerializer());
		ConfigurationOptions options = ConfigurationOptions.defaults().setSerializers(serializers);

		loader = HoconConfigurationLoader.builder().setFile(carrotshopsFile).build();
		shopsNode = loader.load(options);
	}

	public static void load() {
		boolean hasErrors = false;

		if (CarrotShop.getEcoService() != null) {
			String candidateCurrencyID = shopsNode.getNode("globalconfig", "currency").getString();
			for (Currency cur : CarrotShop.getEcoService().getCurrencies()) {
				if (cur.getId().equals(candidateCurrencyID)) {
					currency = cur;
				}
			}
		}

		for (ConfigurationNode soldThingsNode : shopsNode.getNode("notifications", "soldthings").getChildrenList()) {
			try {
				soldThings.add(soldThingsNode.getValue(TypeToken.of(UUID.class)));
			} catch (ObjectMappingException e) {
				e.printStackTrace();
				hasErrors = true;
			}
		}

		for (ConfigurationNode shopNode : shopsNode.getNode("shops").getChildrenList()) {
			try {
				pushShop((Shop) shopNode.getNode("shop").getValue(TypeToken.of(Class.forName(shopNode.getNode("type").getString()))));
			} catch (Exception e) {
				e.printStackTrace();
				hasErrors = true;
			}
		}
		if (hasErrors)
			CarrotShop.getLogger().warn(Lang.CONSOLE_ERROR_LOAD);
	}

	public static void unload() {
		save();
		storedLocations.clear();
		shops.forEach((location,shoplist) -> {
			shoplist.forEach((shop) -> {
				shop.setReset();
			});
		});
		shops.clear();
	}

	public static void save() {
		boolean hasErrors = false;

		if (CarrotShop.getEcoService() != null) {
			shopsNode.getNode("globalconfig", "currency").setValue(getCurrency().getId());
		}
		
		shopsNode.getNode("notifications").removeChild("soldthings");
		for (UUID uuid : soldThings) {
			try {
				shopsNode.getNode("notifications", "soldthings").getAppendedNode().setValue(TypeToken.of(UUID.class), uuid);
			} catch (ObjectMappingException e) {
				e.printStackTrace();
			}
		}

		shopsNode.removeChild("shops");
		for (Entry<Location<World>, List<Shop>> entry : shops.entrySet()) {
			for (Shop shop : entry.getValue()) {
				if (entry.getKey().equals(shop.getLocation())) {
					ConfigurationNode shopNode = shopsNode.getNode("shops").getAppendedNode();
					try {
						if (shop instanceof iTrade)
							shopNode.getNode("shop").setValue(TypeToken.of(iTrade.class), (iTrade) shop);
						else if (shop instanceof iBuy)
							shopNode.getNode("shop").setValue(TypeToken.of(iBuy.class), (iBuy) shop);
						else if (shop instanceof iSell)
							shopNode.getNode("shop").setValue(TypeToken.of(iSell.class), (iSell) shop);
						else if (shop instanceof aTrade)
							shopNode.getNode("shop").setValue(TypeToken.of(aTrade.class), (aTrade) shop);
						else if (shop instanceof aBuy)
							shopNode.getNode("shop").setValue(TypeToken.of(aBuy.class), (aBuy) shop);
						else if (shop instanceof aSell)
							shopNode.getNode("shop").setValue(TypeToken.of(aSell.class), (aSell) shop);
						else if (shop instanceof Trade)
							shopNode.getNode("shop").setValue(TypeToken.of(Trade.class), (Trade) shop);
						else if (shop instanceof Buy)
							shopNode.getNode("shop").setValue(TypeToken.of(Buy.class), (Buy) shop);
						else if (shop instanceof Sell)
							shopNode.getNode("shop").setValue(TypeToken.of(Sell.class), (Sell) shop);
						else if (shop instanceof Bank)
							shopNode.getNode("shop").setValue(TypeToken.of(Bank.class), (Bank) shop);
						else if (shop instanceof Heal)
							shopNode.getNode("shop").setValue(TypeToken.of(Heal.class), (Heal) shop);
						else if (shop instanceof DeviceOn)
							shopNode.getNode("shop").setValue(TypeToken.of(DeviceOn.class), (DeviceOn) shop);
						else if (shop instanceof DeviceOff)
							shopNode.getNode("shop").setValue(TypeToken.of(DeviceOff.class), (DeviceOff) shop);
						else if (shop instanceof Toggle)
							shopNode.getNode("shop").setValue(TypeToken.of(Toggle.class), (Toggle) shop);
						else if (shop instanceof aDeviceOn)
							shopNode.getNode("shop").setValue(TypeToken.of(aDeviceOn.class), (aDeviceOn) shop);
						else if (shop instanceof aDeviceOff)
							shopNode.getNode("shop").setValue(TypeToken.of(aDeviceOff.class), (aDeviceOff) shop);
						else if (shop instanceof aToggle)
							shopNode.getNode("shop").setValue(TypeToken.of(aToggle.class), (aToggle) shop);
						else
							continue;
						shopNode.getNode("type").setValue(shop.getClass().getName());
					} catch (ObjectMappingException e) {
						e.printStackTrace();
						hasErrors = true;
					}
				}
			}
		}
		try {
			loader.save(shopsNode);
		} catch (Exception e) {
			e.printStackTrace();
			hasErrors = true;
		}
		if (hasErrors)
			CarrotShop.getLogger().error("Unable to save all CarrotShops");
	}

	private static void pushShop(Shop shop) {
		for (Location<World> location : shop.getLocations()) {
			List<Shop> shopLoc = shops.getOrDefault(location, new ArrayList<>());
			shopLoc.add(shop);
			shops.put(location, shopLoc);
		}
		shop.update();
	}

	public static void addShop(Shop shop) {
		pushShop(shop);
		save();
	}	

	public static void delShop(Shop shop) {
		shop.setReset();
		for (Location<World> location : shop.getLocations()) {
			List<Shop> shopLoc = shops.get(location);
			if (shopLoc == null)
				continue;
			shopLoc.remove(shop);
			if (shopLoc.isEmpty())
				shops.remove(location);
			else
				shops.put(location, shopLoc);
		}
		save();
	}

	public static Optional<List<Shop>> getShops(Location<World> loc) {
		if (shops.containsKey(loc))
			return Optional.of(shops.get(loc));
		return Optional.empty();
	}

	public static void storeItemLocation(Player player, Location<World> loc) {
		Stack<Location<World>> items = storedLocations.getOrDefault(player.getUniqueId(), new Stack<>());
		if (items.contains(loc)) {
			items.remove(loc);
			player.sendMessage(Text.of(Lang.LOCATION_REMOVED));
			return ;
		} else {
			items.push(loc);
			player.sendMessage(Text.of(Lang.LOCATION_STORED));
		}
		storedLocations.put(player.getUniqueId(), items);
	}

	public static Stack<Location<World>> getItemLocations(Player player) {
		return storedLocations.getOrDefault(player.getUniqueId(), new Stack<>());
	}

	public static void clearItemLocations(Player player) {
		storedLocations.remove(player.getUniqueId());

	}

	public static void setCurrency(Currency cur) {
		currency = cur;
		save();
	}

	public static Currency getCurrency() {
		if (CarrotShop.getEcoService() == null)
			return null;
		if (currency != null)
			return currency;
		return CarrotShop.getEcoService().getDefaultCurrency();
	}

	public static boolean hasMultipleCurrencies() {
		return CarrotShop.getEcoService() != null && CarrotShop.getEcoService().getCurrencies().size() > 1;
	}

	public static void soldSomethingOffline(UUID uuid) {
		if (!soldThings.contains(uuid)) {
			soldThings.add(uuid);
			save();
		}
	}

	public static boolean hasSoldSomethingOffline(UUID uuid) {
		boolean ret = false;

		if (soldThings.contains(uuid)) {
			ret = true;
			soldThings.remove(uuid);
			save();
		}
		return ret;
	}
}
