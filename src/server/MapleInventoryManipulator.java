package server;

import client.MapleBuffStat;
import client.MapleClient;
import client.MapleQuestStatus;
import client.MapleTrait.MapleTraitType;
import client.character.MapleCharacter;
import client.PlayerStats;
import client.Skill;
import client.SkillEntry;
import client.SkillFactory;
import client.inventory.*;
import constants.GameConstants;
import net.packet.CField;
import net.packet.CSPacket;
import net.packet.CWvsContext;
import net.packet.CWvsContext.InfoPacket;
import net.packet.CWvsContext.InventoryPacket;

import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import server.quest.MapleQuest;
import tools.Pair;
import tools.StringUtil;

public class MapleInventoryManipulator {

    public static void addRing(MapleCharacter chr, int itemId, int ringId, int sn, String partner) {
        CashItemInfo csi = CashItemFactory.getInstance().getItem(sn);
        if (csi == null) {
            return;
        }
        Item ring = chr.getCashInventory().toItem(csi, ringId);
        if (ring == null || ring.getUniqueId() != ringId || ring.getUniqueId() <= 0 || ring.getItemId() != itemId) {
            return;
        }
        chr.getCashInventory().addToInventory(ring);
        chr.getClient().sendPacket(CSPacket.sendBoughtRings(GameConstants.isCrushRing(itemId), ring, sn, chr.getClient().getAccID(), partner));
    }

    public static boolean addbyItem(final MapleClient c, final Item item) {
        return addbyItem(c, item, false) >= 0;
    }

    public static short addbyItem(final MapleClient c, final Item item, final boolean fromcs) {
        final MapleInventoryType type = GameConstants.getInventoryType(item.getItemId());
        final short newSlot = c.getCharacter().getInventory(type).addItem(item);
        if (newSlot == -1) {
            if (!fromcs) {
                c.sendPacket(InventoryPacket.getInventoryFull());
                c.sendPacket(InventoryPacket.getShowInventoryFull());
            }
            return newSlot;
        }
        if (GameConstants.isHarvesting(item.getItemId())) {
            c.getCharacter().getStat().handleProfessionTool(c.getCharacter());
        }
        c.sendPacket(InventoryPacket.addInventorySlot(type, item));
        c.getCharacter().havePartyQuest(item.getItemId());
        return newSlot;
    }

    public static int getUniqueId(int itemId, MaplePet pet) {
        int uniqueid = -1;
        if (GameConstants.isPet(itemId)) {
            if (pet != null) {
                uniqueid = pet.getUniqueId();
            } else {
                uniqueid = MapleInventoryIdentifier.getInstance();
            }
        } else if (GameConstants.getInventoryType(itemId) == MapleInventoryType.CASH || MapleItemInformationProvider.getInstance().isCash(itemId)) { //less work to do
            uniqueid = MapleInventoryIdentifier.getInstance(); //shouldnt be generated yet, so put it here
        }
        return uniqueid;
    }

    public static boolean addById(MapleClient c, int itemId, short quantity, String gmLog) {
        return addById(c, itemId, quantity, null, null, 0, false, gmLog);
    }

    public static boolean addById(MapleClient c, int itemId, short quantity, String owner, String gmLog) {
        return addById(c, itemId, quantity, owner, null, 0, false, gmLog);
    }

    public static byte addId(MapleClient c, int itemId, short quantity, String owner, String gmLog) {
        return addId(c, itemId, quantity, owner, null, 0, false, gmLog);
    }

    public static boolean addById(MapleClient c, int itemId, short quantity, String owner, MaplePet pet, String gmLog) {
        return addById(c, itemId, quantity, owner, pet, 0, false, gmLog);
    }

    public static boolean addById(MapleClient c, int itemId, short quantity, String owner, MaplePet pet, long period, boolean hours, String gmLog) {
        return addId(c, itemId, quantity, owner, pet, period, hours, gmLog) >= 0;
    }

    public static byte addId(MapleClient c, int itemId, short quantity, String owner, MaplePet pet, long period, boolean hours, String gmLog) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if ((ii.isPickupRestricted(itemId) && c.getCharacter().haveItem(itemId, 1, true, false)) || (!ii.itemExists(itemId))) {
            c.sendPacket(InventoryPacket.getInventoryFull());
            c.sendPacket(InventoryPacket.showItemUnavailable());
            return -1;
        }
        if (itemId >= 4031332 && itemId <= 4031341) {
            c.sendPacket(CField.getGameMessage((short) 8, "Hint: Use @event to exchange a certificate of straight wins."));
        }
        final MapleInventoryType type = GameConstants.getInventoryType(itemId);
        int uniqueid = getUniqueId(itemId, pet);
        short newSlot = -1;
        if (!type.equals(MapleInventoryType.EQUIP)) {
            final short slotMax = ii.getSlotMax(itemId);
            final List<Item> existing = c.getCharacter().getInventory(type).listById(itemId);
            if (!GameConstants.isRechargable(itemId)) {
                if (existing.size() > 0) { // first update all existing slots to slotMax
                    Iterator<Item> i = existing.iterator();
                    while (quantity > 0) {
                        if (i.hasNext()) {
                            Item eItem = (Item) i.next();
                            short oldQ = eItem.getQuantity();
                            if (oldQ < slotMax && (eItem.getOwner().equals(owner) || owner == null) && eItem.getExpiration() == -1) {
                                short newQ = (short) Math.min(oldQ + quantity, slotMax);
                                quantity -= (newQ - oldQ);
                                eItem.setQuantity(newQ);
                                c.sendPacket(InventoryPacket.updateInventorySlot(type, eItem, false));
                            }
                        } else {
                            break;
                        }
                    }
                }
                Item nItem;
                // add new slots if there is still something left
                while (quantity > 0) {
                    short newQ = (short) Math.min(quantity, slotMax);
                    if (newQ != 0) {
                        quantity -= newQ;
                        nItem = new Item(itemId, (byte) 0, newQ, (byte) 0, uniqueid);
                        newSlot = c.getCharacter().getInventory(type).addItem(nItem);
                        if (newSlot == -1) {
                            c.sendPacket(InventoryPacket.getInventoryFull());
                            c.sendPacket(InventoryPacket.getShowInventoryFull());
                            return -1;
                        }
                        if (gmLog != null) {
                            nItem.setGMLog(gmLog);
                        }
                        if (owner != null) {
                            nItem.setOwner(owner);
                        }
                        if (period > 0) {
                            nItem.setExpiration(System.currentTimeMillis() + (period * (hours ? 1 : 24) * 60 * 60 * 1000));
                        }
                        if (pet != null) {
                            nItem.setPet(pet);
                            pet.setInventoryPosition(newSlot);
                            c.getCharacter().addPet(pet);
                        }
                        c.sendPacket(InventoryPacket.addInventorySlot(type, nItem));
                        if (GameConstants.isRechargable(itemId) && quantity == 0) {
                            break;
                        }
                    } else {
                        c.getCharacter().havePartyQuest(itemId);
                        c.sendPacket(CWvsContext.enableActions());
                        return (byte) newSlot;
                    }
                }
            } else {
                // Throwing Stars and Bullets - Add all into one slot regardless of quantity.
                final Item nItem = new Item(itemId, (byte) 0, quantity, (byte) 0, uniqueid);
                newSlot = c.getCharacter().getInventory(type).addItem(nItem);

                if (newSlot == -1) {
                    c.sendPacket(InventoryPacket.getInventoryFull());
                    c.sendPacket(InventoryPacket.getShowInventoryFull());
                    return -1;
                }
                if (period > 0) {
                    nItem.setExpiration(System.currentTimeMillis() + (period * 24 * 60 * 60 * 1000));
                }
                if (gmLog != null) {
                    nItem.setGMLog(gmLog);
                }
                c.sendPacket(InventoryPacket.addInventorySlot(type, nItem));
                c.sendPacket(CWvsContext.enableActions());
            }
        } else {
            if (quantity == 1) {
                final Item nEquip = ii.getEquipById(itemId, uniqueid);
                if (owner != null) {
                    nEquip.setOwner(owner);
                }
                if (gmLog != null) {
                    nEquip.setGMLog(gmLog);
                }
                if (period > 0) {
                    nEquip.setExpiration(System.currentTimeMillis() + (period * 24 * 60 * 60 * 1000));
                }
                newSlot = c.getCharacter().getInventory(type).addItem(nEquip);
                if (newSlot == -1) {
                    c.sendPacket(InventoryPacket.getInventoryFull());
                    c.sendPacket(InventoryPacket.getShowInventoryFull());
                    return -1;
                }
                c.sendPacket(InventoryPacket.addInventorySlot(type, nEquip));
                if (GameConstants.isHarvesting(itemId)) {
                    c.getCharacter().getStat().handleProfessionTool(c.getCharacter());
                }
            } else {
                throw new InventoryException("Trying to create equip with non-one quantity");
            }
        }
        c.getCharacter().havePartyQuest(itemId);
        return (byte) newSlot;
    }

    public static Item addbyId_Gachapon(final MapleClient c, final int itemId, short quantity) {
        if (c.getCharacter().getInventory(MapleInventoryType.EQUIP).getNextFreeSlot() == -1 || c.getCharacter().getInventory(MapleInventoryType.USE).getNextFreeSlot() == -1 || c.getCharacter().getInventory(MapleInventoryType.ETC).getNextFreeSlot() == -1 || c.getCharacter().getInventory(MapleInventoryType.SETUP).getNextFreeSlot() == -1) {
            return null;
        }
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if ((ii.isPickupRestricted(itemId) && c.getCharacter().haveItem(itemId, 1, true, false)) || (!ii.itemExists(itemId))) {
            c.sendPacket(InventoryPacket.getInventoryFull());
            c.sendPacket(InventoryPacket.showItemUnavailable());
            return null;
        }
        final MapleInventoryType type = GameConstants.getInventoryType(itemId);

        if (!type.equals(MapleInventoryType.EQUIP)) {
            short slotMax = ii.getSlotMax(itemId);
            final List<Item> existing = c.getCharacter().getInventory(type).listById(itemId);

            if (!GameConstants.isRechargable(itemId)) {
                Item nItem = null;
                boolean recieved = false;

                if (existing.size() > 0) { // first update all existing slots to slotMax
                    Iterator<Item> i = existing.iterator();
                    while (quantity > 0) {
                        if (i.hasNext()) {
                            nItem = (Item) i.next();
                            short oldQ = nItem.getQuantity();

                            if (oldQ < slotMax) {
                                recieved = true;

                                short newQ = (short) Math.min(oldQ + quantity, slotMax);
                                quantity -= (newQ - oldQ);
                                nItem.setQuantity(newQ);
                                c.sendPacket(InventoryPacket.updateInventorySlot(type, nItem, false));
                            }
                        } else {
                            break;
                        }
                    }
                }
                // add new slots if there is still something left
                while (quantity > 0) {
                    short newQ = (short) Math.min(quantity, slotMax);
                    if (newQ != 0) {
                        quantity -= newQ;
                        nItem = new Item(itemId, (byte) 0, newQ, (byte) 0);
                        final short newSlot = c.getCharacter().getInventory(type).addItem(nItem);
                        if (newSlot == -1 && recieved) {
                            return nItem;
                        } else if (newSlot == -1) {
                            return null;
                        }
                        recieved = true;
                        c.sendPacket(InventoryPacket.addInventorySlot(type, nItem));
                        if (GameConstants.isRechargable(itemId) && quantity == 0) {
                            break;
                        }
                    } else {
                        break;
                    }
                }
                if (recieved) {
                    c.getCharacter().havePartyQuest(nItem.getItemId());
                    return nItem;
                }
            } else {
                // Throwing Stars and Bullets - Add all into one slot regardless of quantity.
                final Item nItem = new Item(itemId, (byte) 0, quantity, (byte) 0);
                final short newSlot = c.getCharacter().getInventory(type).addItem(nItem);

                if (newSlot == -1) {
                    return null;
                }
                c.sendPacket(InventoryPacket.addInventorySlot(type, nItem));
                c.getCharacter().havePartyQuest(nItem.getItemId());
                return nItem;
            }
        } else {
            if (quantity == 1) {
                final Item item = ii.randomizeStats((Equip) ii.getEquipById(itemId));
                final short newSlot = c.getCharacter().getInventory(type).addItem(item);

                if (newSlot == -1) {
                    return null;
                }
                c.sendPacket(InventoryPacket.addInventorySlot(type, item, true));
                c.getCharacter().havePartyQuest(item.getItemId());
                return item;
            } else {
                throw new InventoryException("Trying to create equip with non-one quantity");
            }
        }
        return null;
    }

    public static boolean addFromDrop(final MapleClient c, final Item item, final boolean show) {
        return addFromDrop(c, item, show, false);
    }

    public static boolean addFromDrop(final MapleClient c, Item item, final boolean show, final boolean enhance) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

        if (c.getCharacter() == null || (ii.isPickupRestricted(item.getItemId()) && c.getCharacter().haveItem(item.getItemId(), 1, true, false)) || (!ii.itemExists(item.getItemId()))) {
            c.sendPacket(InventoryPacket.getInventoryFull());
            c.sendPacket(InventoryPacket.showItemUnavailable());
            return false;
        }
        final int before = c.getCharacter().itemQuantity(item.getItemId());
        short quantity = item.getQuantity();
        final MapleInventoryType type = GameConstants.getInventoryType(item.getItemId());

        if (!type.equals(MapleInventoryType.EQUIP)) {
            final short slotMax = ii.getSlotMax(item.getItemId());
            final List<Item> existing = c.getCharacter().getInventory(type).listById(item.getItemId());
            if (!GameConstants.isRechargable(item.getItemId())) {
                if (quantity <= 0) { //wth
                    c.sendPacket(InventoryPacket.getInventoryFull());
                    c.sendPacket(InventoryPacket.showItemUnavailable());
                    return false;
                }
                if (existing.size() > 0) { // first update all existing slots to slotMax
                    Iterator<Item> i = existing.iterator();
                    while (quantity > 0) {
                        if (i.hasNext()) {
                            final Item eItem = (Item) i.next();
                            final short oldQ = eItem.getQuantity();
                            if (oldQ < slotMax && item.getOwner().equals(eItem.getOwner()) && item.getExpiration() == eItem.getExpiration()) {
                                final short newQ = (short) Math.min(oldQ + quantity, slotMax);
                                quantity -= (newQ - oldQ);
                                eItem.setQuantity(newQ);
                                c.sendPacket(InventoryPacket.updateInventorySlot(type, eItem, true));
                            }
                        } else {
                            break;
                        }
                    }
                }
                // add new slots if there is still something left
                while (quantity > 0) {
                    final short newQ = (short) Math.min(quantity, slotMax);
                    quantity -= newQ;
                    final Item nItem = new Item(item.getItemId(), (byte) 0, newQ, item.getFlag());
                    nItem.setExpiration(item.getExpiration());
                    nItem.setOwner(item.getOwner());
                    nItem.setPet(item.getPet());
                    nItem.setGMLog(item.getGMLog());
                    short newSlot = c.getCharacter().getInventory(type).addItem(nItem);
                    if (newSlot == -1) {
                        c.sendPacket(InventoryPacket.getInventoryFull());
                        c.sendPacket(InventoryPacket.getShowInventoryFull());
                        item.setQuantity((short) (quantity + newQ));
                        return false;
                    }
                    c.sendPacket(InventoryPacket.addInventorySlot(type, nItem, true));
                }
            } else {
                // Throwing Stars and Bullets - Add all into one slot regardless of quantity.
                final Item nItem = new Item(item.getItemId(), (byte) 0, quantity, item.getFlag());
                nItem.setExpiration(item.getExpiration());
                nItem.setOwner(item.getOwner());
                nItem.setPet(item.getPet());
                nItem.setGMLog(item.getGMLog());
                final short newSlot = c.getCharacter().getInventory(type).addItem(nItem);
                if (newSlot == -1) {
                    c.sendPacket(InventoryPacket.getInventoryFull());
                    c.sendPacket(InventoryPacket.getShowInventoryFull());
                    return false;
                }
                c.sendPacket(InventoryPacket.addInventorySlot(type, nItem));
                c.sendPacket(CWvsContext.enableActions());
            }
        } else {
            if (quantity == 1) {
                if (enhance) {
                    item = checkEnhanced(item, c.getCharacter());
                }
                final short newSlot = c.getCharacter().getInventory(type).addItem(item);

                if (newSlot == -1) {
                    c.sendPacket(InventoryPacket.getInventoryFull());
                    c.sendPacket(InventoryPacket.getShowInventoryFull());
                    return false;
                }
                c.sendPacket(InventoryPacket.addInventorySlot(type, item, true));
                if (GameConstants.isHarvesting(item.getItemId())) {
                    c.getCharacter().getStat().handleProfessionTool(c.getCharacter());
                }
            } else {
                throw new RuntimeException("Trying to create equip with non-one quantity");
            }
        }
        if (item.getQuantity() >= 50 && item.getItemId() == 2340000) {
            c.setMonitored(true);
        }
        //if (before == 0) {
        //    switch (item.getItemId()) {
        //        case AramiaFireWorks.KEG_ID:
        //            c.getPlayer().dropMessage(5, "You have gained a Powder Keg.");
        //            break;
        //        case AramiaFireWorks.SUN_ID:
        //            c.getPlayer().dropMessage(5, "You have gained a Warm Sun.");
        //            break;
        //       case AramiaFireWorks.DEC_ID:
        //            c.getPlayer().dropMessage(5, "You have gained a Tree Decoration.");
        //            break;
        //    }
        //}
        c.getCharacter().havePartyQuest(item.getItemId());
        if (show) {
            c.sendPacket(InfoPacket.getShowItemGain(item.getItemId(), item.getQuantity()));
        }
        return true;
    }

    private static Item checkEnhanced(final Item before, final MapleCharacter chr) {
        if (before instanceof Equip) {
            final Equip eq = (Equip) before;
            if (eq.getState() == 0 && (eq.getUpgradeSlots() >= 1 || eq.getLevel() >= 1) && GameConstants.canScroll(eq.getItemId()) && Randomizer.nextInt(100) >= 80) { //20% chance of pot?
                eq.resetPotential();
            }
        }
        return before;
    }

    public static boolean checkSpace(final MapleClient c, final int itemid, int quantity, final String owner) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (c.getCharacter() == null || (ii.isPickupRestricted(itemid) && c.getCharacter().haveItem(itemid, 1, true, false)) || (!ii.itemExists(itemid))) {
            c.sendPacket(CWvsContext.enableActions());
            return false;
        }
        if (quantity <= 0 && !GameConstants.isRechargable(itemid)) {
            return false;
        }
        final MapleInventoryType type = GameConstants.getInventoryType(itemid);
        if (c == null || c.getCharacter() == null || c.getCharacter().getInventory(type) == null) { //wtf is causing this?
            return false;
        }
        if (!type.equals(MapleInventoryType.EQUIP)) {
            final short slotMax = ii.getSlotMax(itemid);
            final List<Item> existing = c.getCharacter().getInventory(type).listById(itemid);
            if (!GameConstants.isRechargable(itemid)) {
                if (existing.size() > 0) { // first update all existing slots to slotMax
                    for (Item eItem : existing) {
                        final short oldQ = eItem.getQuantity();
                        if (oldQ < slotMax && owner != null && owner.equals(eItem.getOwner())) {
                            final short newQ = (short) Math.min(oldQ + quantity, slotMax);
                            quantity -= (newQ - oldQ);
                        }
                        if (quantity <= 0) {
                            break;
                        }
                    }
                }
            }
            // add new slots if there is still something left
            final int numSlotsNeeded;
            if (slotMax > 0 && !GameConstants.isRechargable(itemid)) {
                numSlotsNeeded = (int) (Math.ceil(((double) quantity) / slotMax));
            } else {
                numSlotsNeeded = 1;
            }
            return !c.getCharacter().getInventory(type).isFull(numSlotsNeeded - 1);
        } else {
            return !c.getCharacter().getInventory(type).isFull();
        }
    }

    public static boolean removeFromSlot(final MapleClient c, final MapleInventoryType type, final short slot, final short quantity, final boolean fromDrop) {
        return removeFromSlot(c, type, slot, quantity, fromDrop, false);
    }

    public static boolean removeFromSlot(final MapleClient c, final MapleInventoryType type, final short slot, short quantity, final boolean fromDrop, final boolean consume) {
        if (c.getCharacter() == null || c.getCharacter().getInventory(type) == null) {
            return false;
        }
        final Item item = c.getCharacter().getInventory(type).getItem(slot);
        if (item != null) {
            final boolean allowZero = consume && GameConstants.isRechargable(item.getItemId());
            c.getCharacter().getInventory(type).removeItem(slot, quantity, allowZero);
            if (GameConstants.isHarvesting(item.getItemId())) {
                c.getCharacter().getStat().handleProfessionTool(c.getCharacter());
            }

            if (item.getQuantity() == 0 && !allowZero) {
                c.sendPacket(InventoryPacket.clearInventoryItem(type, item.getPosition(), fromDrop));
            } else {
                c.sendPacket(InventoryPacket.updateInventorySlot(type, (Item) item, fromDrop));
            }
            return true;
        }
        return false;
    }

    public static boolean removeById(final MapleClient c, final MapleInventoryType type, final int itemId, final int quantity, final boolean fromDrop, final boolean consume) {
        int remremove = quantity;
        if (c.getCharacter() == null || c.getCharacter().getInventory(type) == null) {
            return false;
        }
        for (Item item : c.getCharacter().getInventory(type).listById(itemId)) {
            int theQ = item.getQuantity();
            if (remremove <= theQ && removeFromSlot(c, type, item.getPosition(), (short) remremove, fromDrop, consume)) {
                remremove = 0;
                break;
            } else if (remremove > theQ && removeFromSlot(c, type, item.getPosition(), item.getQuantity(), fromDrop, consume)) {
                remremove -= theQ;
            }
        }
        return remremove <= 0;
    }

    public static boolean removeFromSlot_Lock(final MapleClient c, final MapleInventoryType type, final short slot, short quantity, final boolean fromDrop, final boolean consume) {
        if (c.getCharacter() == null || c.getCharacter().getInventory(type) == null) {
            return false;
        }
        final Item item = c.getCharacter().getInventory(type).getItem(slot);
        if (item != null) {
            if (ItemFlag.LOCK.check(item.getFlag()) || ItemFlag.UNTRADABLE.check(item.getFlag())) {
                return false;
            }
            return removeFromSlot(c, type, slot, quantity, fromDrop, consume);
        }
        return false;
    }

    public static boolean removeById_Lock(final MapleClient c, final MapleInventoryType type, final int itemId) {
        for (Item item : c.getCharacter().getInventory(type).listById(itemId)) {
            if (removeFromSlot_Lock(c, type, item.getPosition(), (short) 1, false, false)) {
                return true;
            }
        }
        return false;
    }

    public static void move(final MapleClient c, final MapleInventoryType type, final short src, final short dst) {
        if (src < 0 || dst < 0 || src == dst || type == MapleInventoryType.EQUIPPED) {
            return;
        }
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final Item source = c.getCharacter().getInventory(type).getItem(src);
        final Item initialTarget = c.getCharacter().getInventory(type).getItem(dst);
        if (source == null) {
            return;
        }
        boolean bag = false, switchSrcDst = false, bothBag = false;
        short eqIndicator = -1;
        if (dst > c.getCharacter().getInventory(type).getSlotLimit()) {
            if (type == MapleInventoryType.ETC && dst > 100 && dst % 100 != 0) {
                final int eSlot = c.getCharacter().getExtendedSlot((dst / 100) - 1);
                if (eSlot > 0) {
                    final MapleStatEffect ee = ii.getItemEffect(eSlot);
                    if (dst % 100 > ee.getSlotCount() || ee.getType() != ii.getBagType(source.getItemId()) || ee.getType() <= 0) {
                        c.getCharacter().dropMessage(1, "You may not move that item to the bag.");
                        c.sendPacket(CWvsContext.enableActions());
                        return;
                    } else {
                        eqIndicator = 0;
                        bag = true;
                    }
                } else {
                    c.getCharacter().dropMessage(1, "You may not move it to that bag.");
                    c.sendPacket(CWvsContext.enableActions());
                    return;
                }
            } else {
                c.getCharacter().dropMessage(1, "You may not move it there.");
                c.sendPacket(CWvsContext.enableActions());
                return;
            }
        }
        if (src > c.getCharacter().getInventory(type).getSlotLimit() && type == MapleInventoryType.ETC && src > 100 && src % 100 != 0) {
            //source should be not null so not much checks are needed
            if (!bag) {
                switchSrcDst = true;
                eqIndicator = 0;
                bag = true;
            } else {
                bothBag = true;
            }
        }
        short olddstQ = -1;
        if (initialTarget != null) {
            olddstQ = initialTarget.getQuantity();
        }
        final short oldsrcQ = source.getQuantity();
        final short slotMax = ii.getSlotMax(source.getItemId());
        c.getCharacter().getInventory(type).move(src, dst, slotMax);
        if (GameConstants.isHarvesting(source.getItemId())) {
            c.getCharacter().getStat().handleProfessionTool(c.getCharacter());
        }
        if (!type.equals(MapleInventoryType.EQUIP) && initialTarget != null
                && initialTarget.getItemId() == source.getItemId()
                && initialTarget.getOwner().equals(source.getOwner())
                && initialTarget.getExpiration() == source.getExpiration()
                && !GameConstants.isRechargable(source.getItemId())
                && !type.equals(MapleInventoryType.CASH)) {
            if (GameConstants.isHarvesting(initialTarget.getItemId())) {
                c.getCharacter().getStat().handleProfessionTool(c.getCharacter());
            }
            if ((olddstQ + oldsrcQ) > slotMax) {
                c.sendPacket(InventoryPacket.moveAndMergeWithRestInventoryItem(type, src, dst, (short) ((olddstQ + oldsrcQ) - slotMax), slotMax, bag, switchSrcDst, bothBag));
            } else {
                c.sendPacket(InventoryPacket.moveAndMergeInventoryItem(type, src, dst, ((Item) c.getCharacter().getInventory(type).getItem(dst)).getQuantity(), bag, switchSrcDst, bothBag));
            }
        } else {
            c.sendPacket(InventoryPacket.moveInventoryItem(type, switchSrcDst ? dst : src, switchSrcDst ? src : dst, eqIndicator, bag, bothBag));
        }
    }

    public static void equip(final MapleClient c, final short src, short dst) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final MapleCharacter chr = c.getCharacter();
        if (chr == null || (GameConstants.GMS && dst == -55)) {
            return;
        }
        c.getCharacter().getStat().recalcLocalStats(c.getCharacter());
        final PlayerStats statst = c.getCharacter().getStat();
        statst.recalcLocalStats(c.getCharacter());
        Equip source = (Equip) chr.getInventory(MapleInventoryType.EQUIP).getItem(src);
        Equip target;

        if (source == null || source.getDurability() == 0 || GameConstants.isHarvesting(source.getItemId())) {
            c.sendPacket(CWvsContext.enableActions());
            return;
        }
        if (GameConstants.isGMEquip(source.getItemId()) && !c.getCharacter().isStaff()) {
            c.getCharacter().dropMessage(1, "Only Game Masters are allowed to use this item.");
            c.getCharacter().removeAll(source.getItemId(), false);
            c.sendPacket(CWvsContext.enableActions());
            return;
        }
        //if (GameConstants.isMadeByGM(c, source.getItemId(), src) && !c.getPlayer().isStaff()) {
        // c.getPlayer().dropMessage(1, "You are not allowed to use GM-Made equips.");
        // c.sendPacket(CWvsContext.enableActions());
        // return;
        //}
        if (GameConstants.isOverPoweredEquip(c, source.getItemId(), src) && !c.getCharacter().isStaff()) {
            c.getCharacter().dropMessage(1, "It seems that the item is way too over powered, please report to the Admin if you think that the system is wrong.");
            //c.getPlayer().removeAll(source.getItemId(), false); //System might be wrong
            c.sendPacket(CWvsContext.enableActions());
            return;
        }
        if (!c.getCharacter().isGM()) {
            if (source.getItemId() == 1112663 || source.getItemId() == 1112586) {
                c.getCharacter().dropMessage(1, "White Angelic Blessing, and Dark Angelic Blessing are currently not working.");
                c.sendPacket(CWvsContext.enableActions());
                return;
            }
        }

        final Map<String, Integer> stats = ii.getEquipStats(source.getItemId());

        if (stats == null) {
            c.sendPacket(CWvsContext.enableActions());
            return;
        }
        if (dst > -1200 && dst < -999 && !GameConstants.isEvanDragonItem(source.getItemId()) && !GameConstants.isMechanicItem(source.getItemId())) {
            c.sendPacket(CWvsContext.enableActions());
            return;
    // } else if ((dst <= -1200 || (dst >= -999 && dst < -99)) && !stats.containsKey("cash")) {
            // c.sendPacket(CWvsContext.enableActions());
            // return;
            // }
        } else if (((dst < -5003) || ((dst >= -999) && (dst < -99))) && (!stats.containsKey("cash"))) {
            c.sendPacket(CWvsContext.enableActions());
            return;
        } else if (dst > -1400 && dst <= -1300 && c.getCharacter().getAndroid() == null) {
            c.sendPacket(CWvsContext.enableActions());
            return;
        } else if (dst > -5000 && dst <= -1400) {
            c.sendPacket(CWvsContext.enableActions());
            return;
        } else if (dst > -5100 && dst <= -5000 && source.getItemId() / 10000 != 120) {
            c.sendPacket(CWvsContext.enableActions());
            return;
        }
        if (!ii.canEquip(stats, source.getItemId(), chr.getLevel(), chr.getJob(), chr.getFame(), statst.getTotalStr(), statst.getTotalDex(), statst.getTotalLuk(), statst.getTotalInt(), c.getCharacter().getStat().levelBonus, source.getReqLevel())) {
            c.sendPacket(CWvsContext.enableActions());
            return;
        }
        if (GameConstants.isWeapon(source.getItemId()) && dst != -110 &&  dst != -10 && dst != -11) {
            c.sendPacket(CWvsContext.enableActions());
            return;
        }
        if (dst == -18 && !GameConstants.isMountItemAvailable(source.getItemId(), c.getCharacter().getJob())) {
            c.sendPacket(CWvsContext.enableActions());
            return;
        }
        if (dst == -118 && source.getItemId() / 10000 != 190) {
            c.sendPacket(CWvsContext.enableActions());
            return;
        }
        //totem2
        if ((dst <= -5000) && (dst > -5003) && (source.getItemId() / 10000 != 120)) {
            c.sendPacket(CWvsContext.enableActions());
            return;
        }
        if (dst == -59) { //pendant
            MapleQuestStatus stat = c.getCharacter().getQuestNoAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT));
            if (stat == null || stat.getCustomData() == null || Long.parseLong(stat.getCustomData()) < System.currentTimeMillis()) {
                c.sendPacket(CWvsContext.enableActions());
                return;
            }
        }
        if (GameConstants.isKatara(source.getItemId()) || source.getItemId() / 10000 == 135) {
            dst = (byte) -10; //shield slot
        }
          if (source.getItemId() == 1342069) {
            dst = (byte) -110; //cash shield slot
        } 
        if (GameConstants.isEvanDragonItem(source.getItemId()) && (chr.getJob() < 2200 || chr.getJob() > 2218)) {
            c.sendPacket(CWvsContext.enableActions());
            return;
        }

        if (GameConstants.isMechanicItem(source.getItemId()) && (chr.getJob() < 3500 || chr.getJob() > 3512)) {
            c.sendPacket(CWvsContext.enableActions());
            return;
        }

        if (source.getItemId() / 1000 == 1112) { //ring
            for (RingSet s : RingSet.values()) {
                if (s.id.contains(Integer.valueOf(source.getItemId()))) {
                    List<Integer> theList = c.getCharacter().getInventory(MapleInventoryType.EQUIPPED).listIds();
                    for (Integer i : s.id) {
                        if (theList.contains(i)) {
                            c.getCharacter().dropMessage(1, "You may not equip this item because you already have a " + (StringUtil.makeEnumHumanReadable(s.name())) + " equipped.");
                            c.sendPacket(CWvsContext.enableActions());
                            return;
                        }
                    }
                }
            }
        }

        switch (dst) {
            case -6: { // Top
                final Item top = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -5);
                if (top != null && GameConstants.isOverall(top.getItemId())) {
                    if (chr.getInventory(MapleInventoryType.EQUIP).isFull()) {
                        c.sendPacket(InventoryPacket.getInventoryFull());
                        c.sendPacket(InventoryPacket.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -5, chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
                }
                break;
            }
            case -5: {
                final Item top = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -5);
                final Item bottom = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -6);
                if (top != null && GameConstants.isOverall(source.getItemId())) {
                    if (chr.getInventory(MapleInventoryType.EQUIP).isFull(bottom != null && GameConstants.isOverall(source.getItemId()) ? 1 : 0)) {
                        c.sendPacket(InventoryPacket.getInventoryFull());
                        c.sendPacket(InventoryPacket.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -5, chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
                }
                if (bottom != null && GameConstants.isOverall(source.getItemId())) {
                    if (chr.getInventory(MapleInventoryType.EQUIP).isFull()) {
                        c.sendPacket(InventoryPacket.getInventoryFull());
                        c.sendPacket(InventoryPacket.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -6, chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
                }
                break;
            }
            case -10: { // Shield
                Item weapon = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
                if (GameConstants.isKatara(source.getItemId())) {
                    if ((chr.getJob() != 900 && !GameConstants.isDualBlade(chr.getJob())) || weapon == null || !GameConstants.isDagger(weapon.getItemId())) {
                        c.sendPacket(InventoryPacket.getInventoryFull());
                        c.sendPacket(InventoryPacket.getShowInventoryFull());
                        return;
                    }
                } else if (weapon != null && GameConstants.isTwoHanded(weapon.getItemId()) && !GameConstants.isSpecialShield(source.getItemId())) {
                    if (chr.getInventory(MapleInventoryType.EQUIP).isFull()) {
                        c.sendPacket(InventoryPacket.getInventoryFull());
                        c.sendPacket(InventoryPacket.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -11, chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
                }
                break;
            }
                        case -110: { // Shield
                Item weapon = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
                if (GameConstants.isKatara(source.getItemId())) {
                    if ((chr.getJob() != 900 && !GameConstants.isDualBlade(chr.getJob())) || weapon == null || !GameConstants.isDagger(weapon.getItemId())) {
                        c.sendPacket(InventoryPacket.getInventoryFull());
                        c.sendPacket(InventoryPacket.getShowInventoryFull());
                        return;
                    }
                } else if (weapon != null && GameConstants.isTwoHanded(weapon.getItemId()) && !GameConstants.isSpecialShield(source.getItemId())) {
                    if (chr.getInventory(MapleInventoryType.EQUIP).isFull()) {
                        c.sendPacket(InventoryPacket.getInventoryFull());
                        c.sendPacket(InventoryPacket.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -110, chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
                }
                break;
            }
            case -11: { // Weapon
                Item shield = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -10);
                if (shield != null && GameConstants.isTwoHanded(source.getItemId()) && !GameConstants.isSpecialShield(shield.getItemId())) {
                    if (chr.getInventory(MapleInventoryType.EQUIP).isFull()) {
                        c.sendPacket(InventoryPacket.getInventoryFull());
                        c.sendPacket(InventoryPacket.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -10, chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
                }
                break;
            }
        }
        source = (Equip) chr.getInventory(MapleInventoryType.EQUIP).getItem(src); // Equip
        target = (Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem(dst); // Currently equipping
        if (source == null) {
            c.sendPacket(CWvsContext.enableActions());
            return;
        }
        short flag = source.getFlag();
        if (stats.get("equipTradeBlock") != null || source.getItemId() / 10000 == 167) { // Block trade when equipped.
            if (!ItemFlag.UNTRADABLE.check(flag)) {
                flag |= ItemFlag.UNTRADABLE.getValue();
                source.setFlag(flag);
                c.sendPacket(InventoryPacket.updateSpecialItemUse_(source, MapleInventoryType.EQUIP.getType(), c.getCharacter()));
            }
        }
        if (source.getItemId() / 10000 == 166) {
            if (source.getAndroid() == null) {
                int uid = MapleInventoryIdentifier.getInstance();
                source.setUniqueId(uid);
                source.setAndroid(MapleAndroid.create(source.getItemId(), uid));
                flag = (short) (flag | ItemFlag.LOCK.getValue());
                flag = (short) (flag | ItemFlag.UNTRADABLE.getValue());
                flag = (short) (flag | ItemFlag.ANDROID_ACTIVATED.getValue());
                source.setFlag(flag);
                c.sendPacket(CWvsContext.InventoryPacket.updateSpecialItemUse_(source, MapleInventoryType.EQUIP.getType(), c.getCharacter()));
            }
            chr.removeAndroid();
            chr.setAndroid(source.getAndroid());
        } else if ((dst <= -1300) && (chr.getAndroid() != null)) {
            chr.setAndroid(chr.getAndroid());
        }
        if (source.getCharmEXP() > 0 && !ItemFlag.CHARM_EQUIPPED.check(flag)) {
            chr.getTrait(MapleTraitType.charm).addExp(source.getCharmEXP(), chr);
            source.setCharmEXP((short) 0);
            flag |= ItemFlag.CHARM_EQUIPPED.getValue();
            source.setFlag(flag);
            c.sendPacket(InventoryPacket.updateSpecialItemUse_(source, GameConstants.getInventoryType(source.getItemId()).getType(), c.getCharacter()));
        }

        chr.getInventory(MapleInventoryType.EQUIP).removeSlot(src);
        if (target != null) {
            chr.getInventory(MapleInventoryType.EQUIPPED).removeSlot(dst);
        }
        source.setPosition(dst);
        chr.getInventory(MapleInventoryType.EQUIPPED).addFromDB(source);
        if (target != null) {
            target.setPosition(src);
            chr.getInventory(MapleInventoryType.EQUIP).addFromDB(target);
        }
        if (GameConstants.isWeapon(source.getItemId())) {
            c.getCharacter().cancelEffectFromBuffStat(MapleBuffStat.BOOSTER);
            c.getCharacter().cancelEffectFromBuffStat(MapleBuffStat.SPIRIT_CLAW);
            c.getCharacter().cancelEffectFromBuffStat(MapleBuffStat.SOULARROW);
            c.getCharacter().cancelEffectFromBuffStat(MapleBuffStat.WK_CHARGE);
            c.getCharacter().cancelEffectFromBuffStat(MapleBuffStat.LIGHTNING_CHARGE);
        }
        if (source.getItemId() / 10000 == 190 || source.getItemId() / 10000 == 191) {
            c.getCharacter().cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
            c.getCharacter().cancelEffectFromBuffStat(MapleBuffStat.MECH_CHANGE);
        } else if (GameConstants.isReverseItem(source.getItemId())) {
            // chr.finishAchievement(9);
        } else if (GameConstants.isTimelessItem(source.getItemId())) {
            //chr.finishAchievement(10);
        } else if (stats.containsKey("reqLevel") && stats.get("reqLevel") >= 140) {
            // chr.finishAchievement(41);
        } else if (stats.containsKey("reqLevel") && stats.get("reqLevel") >= 130) {
            //chr.finishAchievement(40);
        } else if (source.getItemId() == 1122017) {
            chr.startFairySchedule(true, true);
        }
        if (source.getState() >= 17) {
            final Map<Skill, SkillEntry> ss = new HashMap<>();
            int[] potentials = {source.getPotential1(), source.getPotential2(), source.getPotential3(), source.getBonusPotential1(), source.getBonusPotential2()};
            for (int i : potentials) {
                if (i > 0) {
                    StructItemOption pot = ii.getPotentialInfo(i).get(ii.getReqLevel(source.getItemId()) / 10);
                    if (pot != null && pot.get("skillID") > 0) {
                        ss.put(SkillFactory.getSkill(PlayerStats.getSkillByJob(pot.get("skillID"), c.getCharacter().getJob())), new SkillEntry((byte) 1, (byte) 0, -1));
                    }
                }
            }
            c.getCharacter().changeSkillLevel_Skip(ss, true);
        }
        if (source.getSocketState() > 15) {
            final Map<Skill, SkillEntry> ss = new HashMap<>();
            int[] sockets = {source.getSocket1(), source.getSocket2(), source.getSocket3()};
            for (int i : sockets) {
                if (i > 0) {
                    StructItemOption soc = ii.getSocketInfo(i);
                    if (soc != null && soc.get("skillID") > 0) {
                        ss.put(SkillFactory.getSkill(PlayerStats.getSkillByJob(soc.get("skillID"), c.getCharacter().getJob())), new SkillEntry((byte) 1, (byte) 0, -1));
                    }
                }
            }
            c.getCharacter().changeSkillLevel_Skip(ss, true);
        }
        c.sendPacket(InventoryPacket.moveInventoryItem(MapleInventoryType.EQUIP, src, dst, (byte) 2, false, false));
        chr.equipChanged();
    }

    public static void unequip(final MapleClient c, final short src, final short dst) {
        Equip source = (Equip) c.getCharacter().getInventory(MapleInventoryType.EQUIPPED).getItem(src);
        Equip target = (Equip) c.getCharacter().getInventory(MapleInventoryType.EQUIP).getItem(dst);

        if (dst < 0 || source == null || (GameConstants.GMS && src == -55)) {
            return;
        }
        if (target != null && src <= 0) { // do not allow switching with equip
            c.sendPacket(InventoryPacket.getInventoryFull());
            return;
        }
        c.getCharacter().getInventory(MapleInventoryType.EQUIPPED).removeSlot(src);
        if (target != null) {
            c.getCharacter().getInventory(MapleInventoryType.EQUIP).removeSlot(dst);
        }
        source.setPosition(dst);
        c.getCharacter().getInventory(MapleInventoryType.EQUIP).addFromDB(source);
        if (target != null) {
            target.setPosition(src);
            c.getCharacter().getInventory(MapleInventoryType.EQUIPPED).addFromDB(target);
        }
        if (src == -11 && c.getCharacter().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -111) != null) {
            Equip cashitem = (Equip) c.getCharacter().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -111);
            c.getCharacter().getInventory(MapleInventoryType.EQUIPPED).removeSlot((short) -111);
            cashitem.setPosition(c.getCharacter().getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
            c.getCharacter().getInventory(MapleInventoryType.EQUIP).addFromDB(cashitem);
        }

        if (GameConstants.isWeapon(source.getItemId())) {
            c.getCharacter().cancelEffectFromBuffStat(MapleBuffStat.BOOSTER);
            c.getCharacter().cancelEffectFromBuffStat(MapleBuffStat.SPIRIT_CLAW);
            c.getCharacter().cancelEffectFromBuffStat(MapleBuffStat.SOULARROW);
            c.getCharacter().cancelEffectFromBuffStat(MapleBuffStat.WK_CHARGE);
            c.getCharacter().cancelEffectFromBuffStat(MapleBuffStat.LIGHTNING_CHARGE);
        } else if (source.getItemId() / 10000 == 190 || source.getItemId() / 10000 == 191) {
            c.getCharacter().cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
            c.getCharacter().cancelEffectFromBuffStat(MapleBuffStat.MECH_CHANGE);
        } else if (source.getItemId() / 10000 == 166 || source.getItemId() / 10000 == 167) {
            c.getCharacter().removeAndroid();
        } else if (src <= -1300 && c.getCharacter().getAndroid() != null) {
            c.getCharacter().setAndroid(c.getCharacter().getAndroid());
        } else if (source.getItemId() == 1122017) {
            c.getCharacter().cancelFairySchedule(true);
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (source.getState() >= 17) {
            final Map<Skill, SkillEntry> ss = new HashMap<>();
            int[] potentials = {source.getPotential1(), source.getPotential2(), source.getPotential3(), source.getBonusPotential1(), source.getBonusPotential2()};
            for (int i : potentials) {
                if (i > 0) {
                    StructItemOption pot = ii.getPotentialInfo(i).get(ii.getReqLevel(source.getItemId()) / 10);
                    if (pot != null && pot.get("skillID") > 0) {
                        ss.put(SkillFactory.getSkill(PlayerStats.getSkillByJob(pot.get("skillID"), c.getCharacter().getJob())), new SkillEntry((byte) 0, (byte) 0, -1));
                    }
                }
            }
            c.getCharacter().changeSkillLevel_Skip(ss, true);
        }
        if (source.getSocketState() > 15) {
            final Map<Skill, SkillEntry> ss = new HashMap<>();
            int[] sockets = {source.getSocket1(), source.getSocket2(), source.getSocket3()};
            for (int i : sockets) {
                if (i > 0) {
                    StructItemOption soc = ii.getSocketInfo(i);
                    if (soc != null && soc.get("skillID") > 0) {
                        ss.put(SkillFactory.getSkill(PlayerStats.getSkillByJob(soc.get("skillID"), c.getCharacter().getJob())), new SkillEntry((byte) 1, (byte) 0, -1));
                    }
                }
            }
            c.getCharacter().changeSkillLevel_Skip(ss, true);
        }
        c.sendPacket(InventoryPacket.moveInventoryItem(MapleInventoryType.EQUIP, src, dst, (byte) 1, false, false));
        c.getCharacter().equipChanged();
    }

    public static boolean drop(final MapleClient c, MapleInventoryType type, final short src, final short quantity) {
        return drop(c, type, src, quantity, false);
    }

    public static boolean drop(final MapleClient c, MapleInventoryType type, final short src, short quantity, final boolean npcInduced) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (src < 0) {
            type = MapleInventoryType.EQUIPPED;
        }
        if (c.getCharacter() == null || c.getCharacter().getMap() == null) {
            return false;
        }
        final Item source = c.getCharacter().getInventory(type).getItem(src);
        if (quantity < 0 || source == null || (GameConstants.GMS && src == -55) || (!npcInduced && GameConstants.isPet(source.getItemId())) || (quantity == 0 && !GameConstants.isRechargable(source.getItemId())) || c.getCharacter().inPVP()) {
            c.sendPacket(CWvsContext.enableActions());
            return false;
        }

        final short flag = source.getFlag();
        if (quantity > source.getQuantity() && !GameConstants.isRechargable(source.getItemId())) {
            c.sendPacket(CWvsContext.enableActions());
            return false;
        }
        if (ItemFlag.LOCK.check(flag) || (quantity != 1 && type == MapleInventoryType.EQUIP)) { // hack
            c.sendPacket(CWvsContext.enableActions());
            return false;
        }
        final Point dropPos = new Point(c.getCharacter().getPosition());
        c.getCharacter().getCheatTracker().checkDrop();
        if (quantity < source.getQuantity() && !GameConstants.isRechargable(source.getItemId())) {
            final Item target = source.copy();
            target.setQuantity(quantity);
            source.setQuantity((short) (source.getQuantity() - quantity));
            c.sendPacket(InventoryPacket.dropInventoryItemUpdate(type, source));

            if (ii.isDropRestricted(target.getItemId()) || ii.isAccountShared(target.getItemId())) {
                if (ItemFlag.KARMA_EQ.check(flag)) {
                    target.setFlag((byte) (flag - ItemFlag.KARMA_EQ.getValue()));
                    c.getCharacter().getMap().spawnItemDrop(c.getCharacter(), c.getCharacter(), target, dropPos, true, true);
                } else if (ItemFlag.KARMA_USE.check(flag)) {
                    target.setFlag((byte) (flag - ItemFlag.KARMA_USE.getValue()));
                    c.getCharacter().getMap().spawnItemDrop(c.getCharacter(), c.getCharacter(), target, dropPos, true, true);
                } else if (GameConstants.isAnyDropMap(c.getCharacter().getMapId())) {
                    c.getCharacter().getMap().spawnItemDrop(c.getCharacter(), c.getCharacter(), target, dropPos, true, true);
                } else {
                    c.getCharacter().getMap().disappearingItemDrop(c.getCharacter(), c.getCharacter(), target, dropPos);
                }
            } else {
                if ((GameConstants.isPet(source.getItemId()) || ItemFlag.UNTRADABLE.check(flag)) && !GameConstants.isAnyDropMap(c.getCharacter().getMapId())) {
                    c.getCharacter().getMap().disappearingItemDrop(c.getCharacter(), c.getCharacter(), target, dropPos);
                } else {
                    c.getCharacter().getMap().spawnItemDrop(c.getCharacter(), c.getCharacter(), target, dropPos, true, true);
                }
            }
        } else {
            c.getCharacter().getInventory(type).removeSlot(src);
            if (GameConstants.isHarvesting(source.getItemId())) {
                c.getCharacter().getStat().handleProfessionTool(c.getCharacter());
            }
            c.sendPacket(InventoryPacket.dropInventoryItem((src < 0 ? MapleInventoryType.EQUIP : type), src));
            if (src < 0) {
                c.getCharacter().equipChanged();
            }
            if (ii.isDropRestricted(source.getItemId()) || ii.isAccountShared(source.getItemId())) {
                if (ItemFlag.KARMA_EQ.check(flag)) {
                    source.setFlag((byte) (flag - ItemFlag.KARMA_EQ.getValue()));
                    c.getCharacter().getMap().spawnItemDrop(c.getCharacter(), c.getCharacter(), source, dropPos, true, true);
                } else if (ItemFlag.KARMA_USE.check(flag)) {
                    source.setFlag((byte) (flag - ItemFlag.KARMA_USE.getValue()));
                    c.getCharacter().getMap().spawnItemDrop(c.getCharacter(), c.getCharacter(), source, dropPos, true, true);
                } else if (GameConstants.isAnyDropMap(c.getCharacter().getMapId())) {
                    c.getCharacter().getMap().spawnItemDrop(c.getCharacter(), c.getCharacter(), source, dropPos, true, true);
                } else {
                    c.getCharacter().getMap().disappearingItemDrop(c.getCharacter(), c.getCharacter(), source, dropPos);
                }
            } else {
                if ((GameConstants.isPet(source.getItemId()) || ItemFlag.UNTRADABLE.check(flag)) && !GameConstants.isAnyDropMap(c.getCharacter().getMapId())) {
                    c.getCharacter().getMap().disappearingItemDrop(c.getCharacter(), c.getCharacter(), source, dropPos);
                } else {
                    c.getCharacter().getMap().spawnItemDrop(c.getCharacter(), c.getCharacter(), source, dropPos, true, true);
                }
            }
        }
        return true;
    }

    public static String searchId(int type, String search) {
        String result = "";
        MapleData data = null;
        MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/" + "String.wz"));
        //result += "<<Type: " + type + " | Search: " + search + ">>";
        switch (type) {
            case 1:
                List<String> retNpcs = new ArrayList<>();
                data = dataProvider.getData("Npc.img");
                List<Pair<Integer, String>> npcPairList = new LinkedList<>();
                for (MapleData npcIdData : data.getChildren()) {
                    npcPairList.add(new Pair<>(Integer.parseInt(npcIdData.getName()), MapleDataTool.getString(npcIdData.getChildByPath("name"), "NO-NAME")));
                }
                for (Pair<Integer, String> npcPair : npcPairList) {
                    if (npcPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                        retNpcs.add(npcPair.getLeft() + " - " + npcPair.getRight());
                    }
                }
                if (retNpcs != null && retNpcs.size() > 0) {
                    for (String singleRetNpc : retNpcs) {
                        result += singleRetNpc;
                    }
                } else {
                    result += "No NPC's Found";
                }
                break;
            case 2:
                List<String> retMaps = new ArrayList<>();
                data = dataProvider.getData("Map.img");
                List<Pair<Integer, String>> mapPairList = new LinkedList<>();
                for (MapleData mapAreaData : data.getChildren()) {
                    for (MapleData mapIdData : mapAreaData.getChildren()) {
                        mapPairList.add(new Pair<>(Integer.parseInt(mapIdData.getName()), MapleDataTool.getString(mapIdData.getChildByPath("streetName"), "NO-NAME") + " - " + MapleDataTool.getString(mapIdData.getChildByPath("mapName"), "NO-NAME")));
                    }
                }
                for (Pair<Integer, String> mapPair : mapPairList) {
                    if (mapPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                        retMaps.add(mapPair.getLeft() + " - " + mapPair.getRight());
                    }
                }
                if (retMaps != null && retMaps.size() > 0) {
                    for (String singleRetMap : retMaps) {
                        result += singleRetMap;
                    }
                } else {
                    result += "No Maps Found";
                }
                break;
            case 3:
                List<String> retMobs = new ArrayList<>();
                data = dataProvider.getData("Mob.img");
                List<Pair<Integer, String>> mobPairList = new LinkedList<>();
                for (MapleData mobIdData : data.getChildren()) {
                    mobPairList.add(new Pair<>(Integer.parseInt(mobIdData.getName()), MapleDataTool.getString(mobIdData.getChildByPath("name"), "NO-NAME")));
                }
                for (Pair<Integer, String> mobPair : mobPairList) {
                    if (mobPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                        retMobs.add(mobPair.getLeft() + " - " + mobPair.getRight());
                    }
                }
                if (retMobs != null && retMobs.size() > 0) {
                    for (String singleRetMob : retMobs) {
                        result += singleRetMob;
                    }
                } else {
                    result += "No Mobs Found";
                }
                break;
            case 4:
                List<String> retItems = new ArrayList<>();
                for (ItemInformation itemPair : MapleItemInformationProvider.getInstance().getAllItems()) {
                    if (itemPair != null && itemPair.name != null && itemPair.name.toLowerCase().contains(search.toLowerCase())) {
                        retItems.add("\r\n#b" + itemPair.itemId + " " + " #k- " + " #r#z" + itemPair.itemId + "##k");
                    }
                }
                if (retItems != null && retItems.size() > 0) {
                    for (String singleRetItem : retItems) {
                        if (result.length() < 10000) {
                            result += singleRetItem;
                        } else {
                            result += "\r\n#bCouldn't load all items, there are too many results.#k";
                            return result;
                        }
                    }
                } else {
                    result += "No Items Found";
                }
                break;
            case 5:
                List<String> retQuests = new ArrayList<>();
                for (MapleQuest itemPair : MapleQuest.getAllInstances()) {
                    if (itemPair.getName().length() > 0 && itemPair.getName().toLowerCase().contains(search.toLowerCase())) {
                        retQuests.add(itemPair.getId() + " - " + itemPair.getName());
                    }
                }
                if (retQuests != null && retQuests.size() > 0) {
                    for (String singleRetQuest : retQuests) {
                        result += singleRetQuest;
                    }
                } else {
                    result += "No Quests Found";
                }
                break;
            case 6:
                List<String> retSkills = new ArrayList<>();
                for (Skill skil : SkillFactory.getAllSkills()) {
                    if (skil.getName() != null && skil.getName().toLowerCase().contains(search.toLowerCase())) {
                        retSkills.add(skil.getId() + " - " + skil.getName());
                    }
                }
                if (retSkills != null && retSkills.size() > 0) {
                    for (String singleRetSkill : retSkills) {
                        result += singleRetSkill;
                    }
                } else {
                    result += "No Skills Found";
                }
                break;
            default:
                result += "Invalid Type";
        }
        return result;
    }
}
