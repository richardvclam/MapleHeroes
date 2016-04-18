package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import net.AbstractMaplePacketHandler;
import net.RecvPacketOpcode;
import tools.data.LittleEndianAccessor;
import tools.packet.CField;
import tools.packet.CWvsContext;

public class UseItemEffectHandler extends AbstractMaplePacketHandler {

	public UseItemEffectHandler(RecvPacketOpcode recv) {
		super(recv);
	}

	@Override
	public void handlePacket(LittleEndianAccessor lea, MapleClient c, MapleCharacter chr) {
		final int itemId = lea.readInt();
		
		Item toUse = chr.getInventory((itemId == 4290001) || (itemId == 4290000) ? MapleInventoryType.ETC : MapleInventoryType.CASH).findById(itemId);
        if ((toUse == null) || (toUse.getItemId() != itemId) || (toUse.getQuantity() < 1)) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        if (itemId != 5510000) {
            chr.setItemEffect(itemId);
        }
        chr.getMap().broadcastMessage(chr, CField.itemEffect(chr.getId(), itemId), false);
	}

}
