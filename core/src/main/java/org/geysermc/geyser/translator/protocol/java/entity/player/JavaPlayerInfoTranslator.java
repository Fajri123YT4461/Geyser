/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.translator.protocol.java.entity.player;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntryAction;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerInfoPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.packet.PlayerListPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.skin.SkinManager;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

@Translator(packet = ClientboundPlayerInfoPacket.class)
public class JavaPlayerInfoTranslator extends PacketTranslator<ClientboundPlayerInfoPacket> {
    @Override
    public void translate(GeyserSession session, ClientboundPlayerInfoPacket packet) {
        if (packet.getAction() != PlayerListEntryAction.ADD_PLAYER && packet.getAction() != PlayerListEntryAction.REMOVE_PLAYER)
            return;

        PlayerListPacket translate = new PlayerListPacket();
        translate.setAction(packet.getAction() == PlayerListEntryAction.ADD_PLAYER ? PlayerListPacket.Action.ADD : PlayerListPacket.Action.REMOVE);

        for (PlayerListEntry entry : packet.getEntries()) {
            switch (packet.getAction()) {
                case ADD_PLAYER -> {
                    GameProfile profile = entry.getProfile();
                    PlayerEntity playerEntity;
                    boolean self = profile.getId().equals(session.getPlayerEntity().getUuid());

                    if (self) {
                        // Entity is ourself
                        playerEntity = session.getPlayerEntity();
                    } else {
                        playerEntity = session.getEntityCache().getPlayerEntity(profile.getId());
                    }

                    GameProfile.Property textures = profile.getProperty("textures");
                    String texturesProperty = textures == null ? null : textures.getValue();

                    if (playerEntity == null) {
                        // It's a new player
                        playerEntity = new PlayerEntity(
                                session,
                                -1,
                                session.getEntityCache().getNextEntityId().incrementAndGet(),
                                profile.getId(),
                                Vector3f.ZERO,
                                Vector3f.ZERO,
                                0, 0, 0,
                                profile.getName(),
                                texturesProperty
                        );

                        session.getEntityCache().addPlayerEntity(playerEntity);
                    } else {
                        playerEntity.setUsername(profile.getName());
                        playerEntity.setTexturesProperty(texturesProperty);
                    }

                    playerEntity.setPlayerList(true);

                    // We'll send our own PlayerListEntry in requestAndHandleSkinAndCape
                    // But we need to send other player's entries so they show up in the player list
                    // without processing their skin information - that'll be processed when they spawn in
                    if (self) {
                        SkinManager.requestAndHandleSkinAndCape(playerEntity, session, skinAndCape ->
                                GeyserImpl.getInstance().getLogger().debug("Loaded Local Bedrock Java Skin Data for " + session.getClientData().getUsername()));
                    } else {
                        playerEntity.setValid(true);
                        PlayerListPacket.Entry playerListEntry = SkinManager.buildCachedEntry(session, playerEntity);

                        translate.getEntries().add(playerListEntry);
                    }
                }
                case REMOVE_PLAYER -> {
                    // As the player entity is no longer present, we can remove the entry
                    PlayerEntity entity = session.getEntityCache().removePlayerEntity(entry.getProfile().getId());
                    if (entity != null) {
                        // Just remove the entity's player list status
                        // Don't despawn the entity - the Java server will also take care of that.
                        entity.setPlayerList(false);
                    }
                    if (entity == session.getPlayerEntity()) {
                        // If removing ourself we use our AuthData UUID
                        translate.getEntries().add(new PlayerListPacket.Entry(session.getAuthData().uuid()));
                    } else {
                        translate.getEntries().add(new PlayerListPacket.Entry(entry.getProfile().getId()));
                    }
                }
            }
        }

        if (!translate.getEntries().isEmpty()) {
            session.sendUpstreamPacket(translate);
        }
    }
}
