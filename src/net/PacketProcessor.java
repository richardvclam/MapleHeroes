/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation version 3 as published by
 the Free Software Foundation. You may not use, modify or distribute
 this program under any other version of the GNU Affero General Public
 License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net;

import java.util.LinkedHashMap;
import java.util.Map;
import net.server.handlers.*;
import net.server.login.handlers.*;
import net.server.channel.handlers.*;
import net.server.channel.chat.handlers.*;

public final class PacketProcessor {

    private final static Map<String, PacketProcessor> instances = new LinkedHashMap<>();
    private MaplePacketHandler[] handlers;

    private PacketProcessor() {
        int maxRecvOp = 0;
        for (RecvPacketOpcode op : RecvPacketOpcode.values()) {
            if (op.getValue() > maxRecvOp) {
                maxRecvOp = op.getValue();
            }
        }
        handlers = new MaplePacketHandler[maxRecvOp + 1];
    }

    public MaplePacketHandler getHandler(short packetId) {
        if (packetId > handlers.length) {
            return null;
        }
        MaplePacketHandler handler = handlers[packetId];
        if (handler != null) {
            return handler;
        }
        return null;
    }
    
    public void registerHandler(RecvPacketOpcode code, MaplePacketHandler handler) {
        try {
            handlers[code.getValue()] = handler;
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Error registering handler - " + code.name());
        }
    }

    public synchronized static PacketProcessor getProcessor(int world, int channel) {
        final String lolpair = world + " " + channel;
        PacketProcessor processor = instances.get(lolpair);
        if (processor == null) {
            processor = new PacketProcessor();
            processor.reset(channel);
            instances.put(lolpair, processor);
        }
        return processor;
    }

    public void reset(int channel) {
        handlers = new MaplePacketHandler[handlers.length];
        
        registerHandler(RecvPacketOpcode.AUTH_REQUEST, new AuthRequestHandler(RecvPacketOpcode.AUTH_REQUEST));
        registerHandler(RecvPacketOpcode.PONG, new PongHandler(RecvPacketOpcode.PONG));
        registerHandler(RecvPacketOpcode.CRASH_INFO, new CrashHandler(RecvPacketOpcode.CRASH_INFO));
    	// Login Handlers
    	registerHandler(RecvPacketOpcode.CLIENT_HELLO, new ClientHelloHandler(RecvPacketOpcode.CLIENT_HELLO));
    	registerHandler(RecvPacketOpcode.CLIENT_REQUEST, new ClientRequestHandler(RecvPacketOpcode.CLIENT_REQUEST));
    	registerHandler(RecvPacketOpcode.LOGIN_PASSWORD, new LoginPasswordHandler(RecvPacketOpcode.LOGIN_PASSWORD));
    	registerHandler(RecvPacketOpcode.VIEW_SERVERLIST, new ViewServerListHandler(RecvPacketOpcode.VIEW_SERVERLIST));
        registerHandler(RecvPacketOpcode.REDISPLAY_SERVERLIST, new ServerlistRequestHandler(RecvPacketOpcode.REDISPLAY_SERVERLIST));
        registerHandler(RecvPacketOpcode.CHARLIST_REQUEST, new CharlistRequestHandler(RecvPacketOpcode.CHARLIST_REQUEST));
        registerHandler(RecvPacketOpcode.CHAR_SELECT, new CharSelectWithPicHandler(RecvPacketOpcode.CHAR_SELECT));
        registerHandler(RecvPacketOpcode.PLAYER_LOGGEDIN, new PlayerLoggedInHandler(RecvPacketOpcode.PLAYER_LOGGEDIN));
        registerHandler(RecvPacketOpcode.ACCEPT_TOS, new AcceptToSHandler(RecvPacketOpcode.ACCEPT_TOS)); // Doesn't do anything yet.
        registerHandler(RecvPacketOpcode.SERVERLIST_REQUEST, new ServerlistRequestHandler(RecvPacketOpcode.SERVERLIST_REQUEST));
        registerHandler(RecvPacketOpcode.SERVERSTATUS_REQUEST, new ServerStatusRequestHandler(RecvPacketOpcode.SERVERSTATUS_REQUEST));
        registerHandler(RecvPacketOpcode.CHECK_CHAR_NAME, new CheckCharNameHandler(RecvPacketOpcode.CHECK_CHAR_NAME));
        registerHandler(RecvPacketOpcode.CREATE_CHAR, new CreateCharHandler(RecvPacketOpcode.CREATE_CHAR));
        registerHandler(RecvPacketOpcode.DELETE_CHAR, new DeleteCharHandler(RecvPacketOpcode.DELETE_CHAR));
        registerHandler(RecvPacketOpcode.CHAR_SELECT_NO_PIC, new CharSelectWithNoPicHandler(RecvPacketOpcode.CHAR_SELECT_NO_PIC));
        registerHandler(RecvPacketOpcode.CHANGE_PIC_REQUEST, new ChangePicHandler(RecvPacketOpcode.CHANGE_PIC_REQUEST));
    	// Channel Handlers
    	registerHandler(RecvPacketOpcode.CHANGE_MAP, new ChangeMapHandler(RecvPacketOpcode.CHANGE_MAP));
        registerHandler(RecvPacketOpcode.CHANGE_CHANNEL, new ChangeChannelHandler(RecvPacketOpcode.CHANGE_CHANNEL));
        
        registerHandler(RecvPacketOpcode.MOVE_PLAYER, new MovePlayerHandler(RecvPacketOpcode.MOVE_PLAYER));
        registerHandler(RecvPacketOpcode.CANCEL_CHAIR, new CancelChairHandler(RecvPacketOpcode.CANCEL_CHAIR));
        registerHandler(RecvPacketOpcode.USE_CHAIR, new UseChairHandler(RecvPacketOpcode.USE_CHAIR));
        registerHandler(RecvPacketOpcode.CLOSE_RANGE_ATTACK, new CloseRangeDamageHandler(RecvPacketOpcode.CLOSE_RANGE_ATTACK));
        registerHandler(RecvPacketOpcode.RANGED_ATTACK, new RangedAttackHandler(RecvPacketOpcode.RANGED_ATTACK));
        registerHandler(RecvPacketOpcode.MAGIC_ATTACK, new MagicDamageHandler(RecvPacketOpcode.MAGIC_ATTACK));
        registerHandler(RecvPacketOpcode.PASSIVE_ENERGY, new CloseRangeDamageHandler(RecvPacketOpcode.PASSIVE_ENERGY));
        registerHandler(RecvPacketOpcode.TAKE_DAMAGE, new TakeDamageHandler(RecvPacketOpcode.TAKE_DAMAGE));
        registerHandler(RecvPacketOpcode.GENERAL_CHAT, new GeneralChatHandler(RecvPacketOpcode.GENERAL_CHAT));
        registerHandler(RecvPacketOpcode.FACE_EXPRESSION, new FaceExpressionHandler(RecvPacketOpcode.FACE_EXPRESSION));
        
        registerHandler(RecvPacketOpcode.NPC_TALK, new NPCTalkHandler(RecvPacketOpcode.NPC_TALK));
        registerHandler(RecvPacketOpcode.NPC_TALK_MORE, new NPCTalkMoreHandler(RecvPacketOpcode.NPC_TALK_MORE));
        registerHandler(RecvPacketOpcode.NPC_SHOP, new NPCShopHandler(RecvPacketOpcode.NPC_SHOP));
        registerHandler(RecvPacketOpcode.STORAGE, new StorageHandler(RecvPacketOpcode.STORAGE));
        
        registerHandler(RecvPacketOpcode.ITEM_SORT, new ItemSortHandler(RecvPacketOpcode.ITEM_SORT));
        registerHandler(RecvPacketOpcode.ITEM_GATHER, new ItemGatherHandler(RecvPacketOpcode.ITEM_GATHER));
        registerHandler(RecvPacketOpcode.ITEM_MOVE, new ItemMoveHandler(RecvPacketOpcode.ITEM_MOVE));
        registerHandler(RecvPacketOpcode.USE_ITEM, new UseItemHandler(RecvPacketOpcode.USE_ITEM));
        registerHandler(RecvPacketOpcode.CANCEL_ITEM_EFFECT, new CancelItemEffectHandler(RecvPacketOpcode.CANCEL_ITEM_EFFECT));
        
        registerHandler(RecvPacketOpcode.DISTRIBUTE_AP, new DistributeAPHandler(RecvPacketOpcode.DISTRIBUTE_AP));
        registerHandler(RecvPacketOpcode.AUTO_ASSIGN_AP, new AutoAssignAPHandler(RecvPacketOpcode.AUTO_ASSIGN_AP));
        registerHandler(RecvPacketOpcode.HEAL_OVER_TIME, new HealOverTimeHandler(RecvPacketOpcode.HEAL_OVER_TIME));
        registerHandler(RecvPacketOpcode.DISTRIBUTE_SP, new DistributeSPHandler(RecvPacketOpcode.DISTRIBUTE_SP));
        
        registerHandler(RecvPacketOpcode.SPECIAL_MOVE, new SpecialMoveHandler(RecvPacketOpcode.SPECIAL_MOVE));
        registerHandler(RecvPacketOpcode.CANCEL_BUFF, new CancelBuffHandler(RecvPacketOpcode.CANCEL_BUFF));
        registerHandler(RecvPacketOpcode.MESO_DROP, new MesoDropHandler(RecvPacketOpcode.MESO_DROP));
        
        registerHandler(RecvPacketOpcode.SPECIAL_PORTAL, new SpecialPortalHandler(RecvPacketOpcode.SPECIAL_PORTAL));
        registerHandler(RecvPacketOpcode.USE_INNER_PORTAL, new InnerPortalHandler(RecvPacketOpcode.USE_INNER_PORTAL));
        registerHandler(RecvPacketOpcode.QUEST_ACTION, new QuestActionHandler(RecvPacketOpcode.QUEST_ACTION));
        
        registerHandler(RecvPacketOpcode.ADMIN_CHAT, new AdminChatHandler(RecvPacketOpcode.ADMIN_CHAT));
        registerHandler(RecvPacketOpcode.PARTYCHAT, new PartyChatHandler(RecvPacketOpcode.PARTYCHAT));
        registerHandler(RecvPacketOpcode.COMMAND, new CommandHandler(RecvPacketOpcode.COMMAND));
        registerHandler(RecvPacketOpcode.MESSENGER, new MessengerHandler(RecvPacketOpcode.MESSENGER));
        
        registerHandler(RecvPacketOpcode.PLAYER_INTERACTION, new PlayerInteractionHandler(RecvPacketOpcode.PLAYER_INTERACTION));
        registerHandler(RecvPacketOpcode.PARTY_OPERATION, new PartyOperationHandler(RecvPacketOpcode.PARTY_OPERATION));
        registerHandler(RecvPacketOpcode.PARTY_REQUEST, new PartyRequestHandler(RecvPacketOpcode.PARTY_REQUEST));
        registerHandler(RecvPacketOpcode.ALLOW_PARTY_INVITE, new AllowPartyInviteHandler(RecvPacketOpcode.ALLOW_PARTY_INVITE));
        
        registerHandler(RecvPacketOpcode.BUDDYLIST_MODIFY, new BuddylistModifyHandler(RecvPacketOpcode.BUDDYLIST_MODIFY));
        registerHandler(RecvPacketOpcode.CHANGE_KEYMAP, new KeymapChangeHandler(RecvPacketOpcode.CHANGE_KEYMAP)); 
        
        registerHandler(RecvPacketOpcode.MOVE_LIFE, new MoveLifeHandler(RecvPacketOpcode.MOVE_LIFE));
        
        registerHandler(RecvPacketOpcode.NPC_ACTION, new NPCAnimation(RecvPacketOpcode.NPC_ACTION));
        registerHandler(RecvPacketOpcode.ITEM_PICKUP, new ItemPickupHandler(RecvPacketOpcode.ITEM_PICKUP));
        
        registerHandler(RecvPacketOpcode.DAMAGE_REACTOR, new DamageReactorHandler(RecvPacketOpcode.DAMAGE_REACTOR));
        
        registerHandler(RecvPacketOpcode.SPECIAL_STAT, new SpecialStatHandler(RecvPacketOpcode.SPECIAL_STAT));
        
        
        /*
        registerHandler(RecvPacketOpcode.USE_CASH_ITEM, new UseCashItemHandler());
        
        registerHandler(RecvPacketOpcode.USE_RETURN_SCROLL, new UseItemHandler());
        registerHandler(RecvPacketOpcode.USE_UPGRADE_SCROLL, new ScrollHandler());
        registerHandler(RecvPacketOpcode.USE_SUMMON_BAG, new UseSummonBag());
          
        registerHandler(RecvPacketOpcode.CHAR_INFO_REQUEST, new CharInfoRequestHandler());

        registerHandler(RecvPacketOpcode.GIVE_FAME, new GiveFameHandler());
        
        registerHandler(RecvPacketOpcode.USE_DOOR, new DoorHandler());
        registerHandler(RecvPacketOpcode.ENTER_MTS, new EnterMTSHandler());
        registerHandler(RecvPacketOpcode.ENTER_CASHSHOP, new EnterCashShopHandler());
        registerHandler(RecvPacketOpcode.DAMAGE_SUMMON, new DamageSummonHandler());
        registerHandler(RecvPacketOpcode.MOVE_SUMMON, new MoveSummonHandler());
        registerHandler(RecvPacketOpcode.SUMMON_ATTACK, new SummonDamageHandler());
        
        registerHandler(RecvPacketOpcode.USE_ITEMEFFECT, new UseItemEffectHandler());
        
        registerHandler(RecvPacketOpcode.GUILD_OPERATION, new GuildOperationHandler());
        registerHandler(RecvPacketOpcode.DENY_GUILD_REQUEST, new DenyGuildRequestHandler());
        registerHandler(RecvPacketOpcode.BBS_OPERATION, new BBSOperationHandler());
        registerHandler(RecvPacketOpcode.SKILL_EFFECT, new SkillEffectHandler());
        registerHandler(RecvPacketOpcode.MESSENGER, new MessengerHandler());
        
        registerHandler(RecvPacketOpcode.CHECK_CASH, new TouchingCashShopHandler());
        registerHandler(RecvPacketOpcode.CASHSHOP_OPERATION, new CashOperationHandler());
        registerHandler(RecvPacketOpcode.COUPON_CODE, new CouponCodeHandler());
        registerHandler(RecvPacketOpcode.SPAWN_PET, new SpawnPetHandler());
        registerHandler(RecvPacketOpcode.MOVE_PET, new MovePetHandler());
        registerHandler(RecvPacketOpcode.PET_CHAT, new PetChatHandler());
        registerHandler(RecvPacketOpcode.PET_COMMAND, new PetCommandHandler());
        registerHandler(RecvPacketOpcode.PET_FOOD, new PetFoodHandler());
        registerHandler(RecvPacketOpcode.PET_LOOT, new PetLootHandler());
        registerHandler(RecvPacketOpcode.AUTO_AGGRO, new AutoAggroHandler());
        registerHandler(RecvPacketOpcode.MONSTER_BOMB, new MonsterBombHandler());
        registerHandler(RecvPacketOpcode.CANCEL_DEBUFF, new CancelDebuffHandler());
        registerHandler(RecvPacketOpcode.USE_SKILL_BOOK, new SkillBookHandler());
        registerHandler(RecvPacketOpcode.SKILL_MACRO, new SkillMacroHandler());
        registerHandler(RecvPacketOpcode.NOTE_ACTION, new NoteActionHandler());
        registerHandler(RecvPacketOpcode.CLOSE_CHALKBOARD, new CloseChalkboardHandler());
        registerHandler(RecvPacketOpcode.USE_MOUNT_FOOD, new UseMountFoodHandler());
        registerHandler(RecvPacketOpcode.MTS_OPERATION, new MTSHandler());
        registerHandler(RecvPacketOpcode.RING_ACTION, new RingActionHandler());
        registerHandler(RecvPacketOpcode.SPOUSE_CHAT, new SpouseChatHandler());
        registerHandler(RecvPacketOpcode.PET_AUTO_POT, new PetAutoPotHandler());
        registerHandler(RecvPacketOpcode.PET_EXCLUDE_ITEMS, new PetExcludeItemsHandler());
        registerHandler(RecvPacketOpcode.TOUCH_MONSTER_ATTACK, new TouchMonsterDamageHandler());
        registerHandler(RecvPacketOpcode.TROCK_ADD_MAP, new TrockAddMapHandler());
        registerHandler(RecvPacketOpcode.HIRED_MERCHANT_REQUEST, new HiredMerchantRequest());
        registerHandler(RecvPacketOpcode.MOB_DAMAGE_MOB, new MobDamageMobHandler());
        registerHandler(RecvPacketOpcode.REPORT, new ReportHandler());
        registerHandler(RecvPacketOpcode.MONSTER_BOOK_COVER, new MonsterBookCoverHandler());
      
        registerHandler(RecvPacketOpcode.MAKER_SKILL, new MakerSkillHandler());
        registerHandler(RecvPacketOpcode.ADD_FAMILY, new FamilyAddHandler());
        registerHandler(RecvPacketOpcode.USE_FAMILY, new FamilyUseHandler());
        registerHandler(RecvPacketOpcode.USE_HAMMER, new UseHammerHandler());
        registerHandler(RecvPacketOpcode.SCRIPTED_ITEM, new ScriptedItemHandler());
        registerHandler(RecvPacketOpcode.TOUCHING_REACTOR, new TouchReactorHandler());
        registerHandler(RecvPacketOpcode.BEHOLDER, new BeholderHandler());
        registerHandler(RecvPacketOpcode.ADMIN_COMMAND, new AdminCommandHandler());
        registerHandler(RecvPacketOpcode.ADMIN_LOG, new AdminLogHandler());
        registerHandler(RecvPacketOpcode.ALLIANCE_OPERATION, new AllianceOperationHandler());
        registerHandler(RecvPacketOpcode.USE_SOLOMON_ITEM, new UseSolomonHandler());
        registerHandler(RecvPacketOpcode.USE_GACHA_EXP, new UseGachaExpHandler());
        registerHandler(RecvPacketOpcode.USE_ITEM_REWARD, new ItemRewardHandler());
        registerHandler(RecvPacketOpcode.USE_REMOTE, new RemoteGachaponHandler());
        registerHandler(RecvPacketOpcode.ACCEPT_FAMILY, new AcceptFamilyHandler());
        registerHandler(RecvPacketOpcode.DUEY_ACTION, new DueyHandler());
        registerHandler(RecvPacketOpcode.USE_DEATHITEM, new UseDeathItemHandler());
        //registerHandler(RecvPacketOpcode.PLAYER_UPDATE, new PlayerUpdateHandler());don't use unused stuff
        registerHandler(RecvPacketOpcode.USE_MAPLELIFE, new UseMapleLifeHandler());
        registerHandler(RecvPacketOpcode.USE_CATCH_ITEM, new UseCatchItemHandler());
        registerHandler(RecvPacketOpcode.MOB_DAMAGE_MOB_FRIENDLY, new MobDamageMobFriendlyHandler());
        registerHandler(RecvPacketOpcode.PARTY_SEARCH_REGISTER, new PartySearchRegisterHandler());
        registerHandler(RecvPacketOpcode.PARTY_SEARCH_START, new PartySearchStartHandler());
        registerHandler(RecvPacketOpcode.ITEM_SORT2, new ItemIdSortHandler());
        registerHandler(RecvPacketOpcode.LEFT_KNOCKBACK, new LeftKnockbackHandler());
        registerHandler(RecvPacketOpcode.SNOWBALL, new SnowballHandler());
        registerHandler(RecvPacketOpcode.COCONUT, new CoconutHandler());
        registerHandler(RecvPacketOpcode.ARAN_COMBO_COUNTER, new AranComboHandler());
        registerHandler(RecvPacketOpcode.CLICK_GUIDE, new ClickGuideHandler());
        registerHandler(RecvPacketOpcode.FREDRICK_ACTION, new FredrickHandler());
        registerHandler(RecvPacketOpcode.MONSTER_CARNIVAL, new MonsterCarnivalHandler());
        registerHandler(RecvPacketOpcode.REMOTE_STORE, new RemoteStoreHandler());
        registerHandler(RecvPacketOpcode.WEDDING_ACTION, new WeddingHandler());
        registerHandler(RecvPacketOpcode.ADMIN_CHAT, new AdminChatHandler());
        */
    }
}

